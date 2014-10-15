/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.j2k

import com.intellij.psi.PsiClass
import java.util.HashSet
import com.intellij.psi.PsiMember
import java.util.LinkedHashMap
import com.intellij.psi.PsiAnnotationMethod
import java.util.ArrayList
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiMethod
import org.jetbrains.jet.j2k.ast.*
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiReferenceExpression
import com.intellij.openapi.util.text.StringUtil
import java.util.HashMap
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiExpression
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElementFactory

class FieldCorrectionInfo(val name: Identifier, val access: Modifier?, val setterAccess: Modifier?)

class ClassBodyConverter(private val psiClass: PsiClass,
                         private val converter: Converter) {
    private val membersToRemove = HashSet<PsiMember>()
    private val fieldCorrections = HashMap<PsiField, FieldCorrectionInfo>()

    public fun convertBody(): ClassBody {
        processAccessorsToDrop()

        val correctedConverter = buildConverterWithCorrectedFieldNames()

        val constructorConverter = if (psiClass.getName() != null)
            ConstructorConverter(psiClass, correctedConverter, fieldCorrections)
        else
            null

        val convertedMembers = LinkedHashMap<PsiMember, Member>()
        for (element in psiClass.getChildren()) {
            if (element is PsiMember) {
                if (element is PsiAnnotationMethod) continue // converted in convertAnnotationType()

                val converted = correctedConverter.convertMember(element, membersToRemove, constructorConverter)
                if (converted != null && !converted.isEmpty) {
                    convertedMembers.put(element, converted)
                }
            }
        }

        for (member in membersToRemove) {
            convertedMembers.remove(member)
        }

        val useClassObject = shouldGenerateClassObject(convertedMembers)

        val members = ArrayList<Member>()
        val classObjectMembers = ArrayList<Member>()
        val factoryFunctions = ArrayList<FactoryFunction>()
        var primaryConstructorSignature: PrimaryConstructorSignature? = null
        for ((psiMember, member) in convertedMembers) {
            if (member is PrimaryConstructor) {
                assert(primaryConstructorSignature == null)
                primaryConstructorSignature = member.signature()
                val initializer = member.initializer()
                if (initializer != null) {
                    members.add(initializer)
                }
            }
            else if (member is FactoryFunction) {
                factoryFunctions.add(member)
            }
            else if (useClassObject
                     && (if (member is Class) shouldGenerateIntoClassObject(member) else psiMember.hasModifierProperty(PsiModifier.STATIC))) {
                classObjectMembers.add(member)
            }
            else {
                members.add(member)
            }
        }

        val lBrace = LBrace().assignPrototype(psiClass.getLBrace())
        val rBrace = RBrace().assignPrototype(psiClass.getRBrace())
        val classBody = ClassBody(primaryConstructorSignature, constructorConverter?.baseClassParams ?: listOf(), members, classObjectMembers, factoryFunctions, lBrace, rBrace)

        return if (constructorConverter != null) constructorConverter.postProcessConstructors(classBody) else classBody
    }

    private fun Converter.convertMember(member: PsiMember,
                                        membersToRemove: MutableSet<PsiMember>,
                                        constructorConverter: ConstructorConverter?): Member? {
        return when (member) {
            is PsiMethod -> convertMethod(member, membersToRemove, constructorConverter)
            is PsiField -> convertField(member, fieldCorrections[member])
            is PsiClass -> convertClass(member)
            is PsiClassInitializer -> convertInitializer(member)
            else -> throw IllegalArgumentException("Unknown member: $member")
        }
    }

    // do not convert private static methods into class object if possible
    private fun shouldGenerateClassObject(convertedMembers: Map<PsiMember, Member>): Boolean {
        if (psiClass.isEnum()) return false

        if (convertedMembers.values().any { it is Class && shouldGenerateIntoClassObject(it) }) return true

        val members = convertedMembers.keySet().filter { !it.isConstructor() }
        val classObjectMembers = members.filter { it !is PsiClass && it.hasModifierProperty(PsiModifier.STATIC) }
        val nestedClasses = members.filterIsInstance(javaClass<PsiClass>()).filter { it.hasModifierProperty(PsiModifier.STATIC) }
        if (classObjectMembers.all { it is PsiMethod && it.hasModifierProperty(PsiModifier.PRIVATE) }) {
            return nestedClasses.any { nestedClass -> classObjectMembers.any { converter.referenceSearcher.findMethodCalls(it as PsiMethod, nestedClass).isNotEmpty() } }
        }
        else {
            return true
        }
    }

    // we generate nested classes with factory functions into class object as a workaround until secondary constructors supported by Kotlin
    private fun shouldGenerateIntoClassObject(nestedClass: Class)
            = !nestedClass.modifiers.contains(Modifier.INNER) && nestedClass.body.factoryFunctions.isNotEmpty()

    private fun processAccessorsToDrop() {
        val fieldToGetterInfo = HashMap<PsiField, AccessorInfo>()
        val fieldToSetterInfo = HashMap<PsiField, AccessorInfo>()
        val fieldsWithConflict = HashSet<PsiField>()
        for (method in psiClass.getMethods()) {
            val info = getAccessorInfo(method) ?: continue
            val map = if (info.kind == AccessorKind.GETTER) fieldToGetterInfo else fieldToSetterInfo

            val prevInfo = map[info.field]
            if (prevInfo != null) {
                fieldsWithConflict.add(info.field)
                continue
            }

            map[info.field] = info
        }

        for ((field, getterInfo) in fieldToGetterInfo) {
            val setterInfo = run {
                val info = fieldToSetterInfo[field]
                if (info?.propertyName == getterInfo.propertyName) info else null
            }

            membersToRemove.add(getterInfo.method)
            if (setterInfo != null) {
                membersToRemove.add(setterInfo.method)
            }

            val getterAccess = converter.convertModifiers(getterInfo.method).accessModifier()
            val setterAccess = if (setterInfo != null)
                converter.convertModifiers(setterInfo.method).accessModifier()
            else
                converter.convertModifiers(field).accessModifier()
            //TODO: check that setter access is not bigger
            fieldCorrections[field] = FieldCorrectionInfo(Identifier(getterInfo.propertyName).assignNoPrototype(),
                                                          getterAccess,
                                                          setterAccess)
        }
    }

    private enum class AccessorKind {
        GETTER
        SETTER
    }

    private class AccessorInfo(val method: PsiMethod, val field: PsiField, val kind: AccessorKind, val propertyName: String)

    private fun getAccessorInfo(method: PsiMethod): AccessorInfo? {
        val name = method.getName()
        if (name.startsWith("get") && method.getParameterList().getParametersCount() == 0) {
            val body = method.getBody() ?: return null
            val returnStatement = (body.getStatements().singleOrNull() as? PsiReturnStatement) ?: return null
            val field = fieldByExpression(returnStatement.getReturnValue()) ?: return null
            val propertyName = StringUtil.decapitalize(name.substring("get".length))
            return AccessorInfo(method, field, AccessorKind.GETTER, propertyName)
        }
        else if (name.startsWith("set") && method.getParameterList().getParametersCount() == 1) {
            val body = method.getBody() ?: return null
            val statement = (body.getStatements().singleOrNull() as? PsiExpressionStatement) ?: return null
            val assignment = statement.getExpression() as? PsiAssignmentExpression ?: return null
            if (assignment.getOperationTokenType() != JavaTokenType.EQ) return null
            val field = fieldByExpression(assignment.getLExpression()) ?: return null
            if ((assignment.getRExpression() as? PsiReferenceExpression)?.resolve() != method.getParameterList().getParameters().single()) return null
            val propertyName = StringUtil.decapitalize(name.substring("set".length))
            return AccessorInfo(method, field, AccessorKind.SETTER, propertyName)
        }
        else {
            return null
        }
    }

    private fun fieldByExpression(expression: PsiExpression?): PsiField? {
        val refExpr = expression as? PsiReferenceExpression ?: return null
        if (!refExpr.isQualifierEmptyOrThis()) return null
        val field = refExpr.resolve() as? PsiField ?: return null
        if (field.getContainingClass() != psiClass || field.hasModifierProperty(PsiModifier.STATIC)) return null
        return field
    }

    //TODO: correct usages to accessors (and field too) across all code being converted + all other Kotlin code in the project
    private fun buildConverterWithCorrectedFieldNames(): Converter {
        if (fieldCorrections.isEmpty()) return converter
        return converter.withExpressionConverter { prevExpressionConverter ->
            object : ExpressionConverter {
                override fun convertExpression(expression: PsiExpression, converter: Converter): Expression {
                    val result = prevExpressionConverter.convertExpression(expression, converter)
                    if (expression !is PsiReferenceExpression) return result

                    val field = expression.resolve() as? PsiField ?: return result
                    val correction = fieldCorrections[field] ?: return result
                    if (correction.name.name == field.getName()) return result

                    val qualifier = expression.getQualifierExpression()
                    return if (qualifier != null) {
                        QualifiedExpression(converter.convertExpression(qualifier), correction.name)
                    }
                    else {
                        // check if field name is shadowed
                        val elementFactory = PsiElementFactory.SERVICE.getInstance(expression.getProject())
                        val refExpr = elementFactory.createExpressionFromText(correction.name.name, expression) as PsiReferenceExpression
                        if (refExpr.resolve() == null)
                            correction.name
                        else
                            QualifiedExpression(ThisExpression(Identifier.Empty).assignNoPrototype(), correction.name) //TODO: this is not correct in case of nested/anonymous classes
                    }

                }
            }
        }
    }
}