/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.j2k.ast.Class
import org.jetbrains.kotlin.j2k.usageProcessing.AccessorToPropertyProcessing
import org.jetbrains.kotlin.j2k.usageProcessing.MethodIntoObjectProcessing
import org.jetbrains.kotlin.j2k.usageProcessing.ToObjectWithOnlyMethodsProcessing
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.SpecialNames
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashMap

class FieldCorrectionInfo(val name: String, val access: Modifier?, val setterAccess: Modifier?) {
    val identifier = Identifier(name).assignNoPrototype()
}

enum class AccessorKind {
    GETTER,
    SETTER
}

class ClassBodyConverter(private val psiClass: PsiClass,
                         private val converter: Converter,
                         private val isOpenClass: Boolean,
                         private val isObject: Boolean) {
    private val membersToRemove = HashSet<PsiMember>()
    private val fieldCorrections = HashMap<PsiField, FieldCorrectionInfo>()

    public fun convertBody(): ClassBody {
        processAccessorsToDrop()

        val overloadReducer = OverloadReducer(psiClass.getMethods().filter { it !in membersToRemove } /* do not allow OverloadReducer to use accessors converted to properties */,
                                              isOpenClass,
                                              converter.referenceSearcher)

        val constructorConverter = if (psiClass.getName() != null && !isObject)
            ConstructorConverter(psiClass, converter, fieldCorrections, overloadReducer)
        else
            null

        val convertedMembers = LinkedHashMap<PsiMember, Member>()
        for (element in psiClass.getChildren()) {
            if (element is PsiMember) {
                if (element is PsiAnnotationMethod) continue // converted in convertAnnotationType()
                if (isObject && element.isConstructor()) continue // no constructor in object
                if (element is PsiMethod && overloadReducer.shouldDropMethod(element)) continue

                val converted = converter.convertMember(element, membersToRemove, constructorConverter, overloadReducer)
                if (converted != null) {
                    convertedMembers.put(element, converted)
                }
            }
        }

        for (member in membersToRemove) {
            convertedMembers.remove(member)
        }

        val lBrace = LBrace().assignPrototype(psiClass.getLBrace())
        val rBrace = RBrace().assignPrototype(psiClass.getRBrace())

        if (isObject) {
            val psiMembers = convertedMembers.keySet()
            if (psiMembers.all { it is PsiMethod }) { // for object with no fields we can use faster external usage processing
                converter.addUsageProcessing(ToObjectWithOnlyMethodsProcessing(psiClass))
            }
            else {
                for (psiMember in psiMembers) {
                    if (psiMember is PsiMethod /* fields in object can be accessed as fields from java */
                        && !psiMember.hasModifierProperty(PsiModifier.PRIVATE)) {
                        converter.addUsageProcessing(MethodIntoObjectProcessing(psiMember, JvmAbi.INSTANCE_FIELD))
                    }
                }
            }

            return ClassBody(null, null, convertedMembers.values().toList(), emptyList(), lBrace, rBrace)
        }

        val useCompanionObject = shouldGenerateCompanionObject(convertedMembers)

        val members = ArrayList<Member>()
        val companionObjectMembers = ArrayList<Member>()
        var primaryConstructorSignature: PrimaryConstructorSignature? = null
        for ((psiMember, member) in convertedMembers) {
            if (member is PrimaryConstructor) {
                assert(primaryConstructorSignature == null)
                primaryConstructorSignature = member.createSignature(converter)
                members.add(member.initializer())
            }
            else if (useCompanionObject && member !is Class && psiMember !is PsiEnumConstant && psiMember.hasModifierProperty(PsiModifier.STATIC)) {
                companionObjectMembers.add(member)
                if (psiMember is PsiMethod /* fields in companion object can be accessed as fields from java */
                        && !psiMember.hasModifierProperty(PsiModifier.PRIVATE)) {
                    converter.addUsageProcessing(MethodIntoObjectProcessing(psiMember, SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.getIdentifier()))
                }
            }
            else {
                members.add(member)
            }
        }

        if (primaryConstructorSignature != null
            && primaryConstructorSignature!!.annotations.isEmpty
            && primaryConstructorSignature!!.accessModifier == null
            && primaryConstructorSignature!!.parameterList.parameters.isEmpty()
            && members.none { it is SecondaryConstructor }
        ) {
            primaryConstructorSignature = null // no "()" after class name is needed in this case
        }

        return ClassBody(primaryConstructorSignature, constructorConverter?.baseClassParams, members, companionObjectMembers, lBrace, rBrace)
    }

    private fun Converter.convertMember(member: PsiMember,
                                        membersToRemove: MutableSet<PsiMember>,
                                        constructorConverter: ConstructorConverter?,
                                        overloadReducer: OverloadReducer): Member? {
        return when (member) {
            is PsiMethod -> convertMethod(member, membersToRemove, constructorConverter, overloadReducer, isOpenClass)
            is PsiField -> convertField(member, fieldCorrections[member])
            is PsiClass -> convertClass(member)
            is PsiClassInitializer -> convertInitializer(member)
            else -> throw IllegalArgumentException("Unknown member: $member")
        }
    }

    // do not convert private static methods into companion object if possible
    private fun shouldGenerateCompanionObject(convertedMembers: Map<PsiMember, Member>): Boolean {
        val members = convertedMembers.keySet().filter { !it.isConstructor() }
        val companionObjectMembers = members.filter { it !is PsiClass && it !is PsiEnumConstant && it.hasModifierProperty(PsiModifier.STATIC) }
        val nestedClasses = members.filterIsInstance<PsiClass>().filter { it.hasModifierProperty(PsiModifier.STATIC) }
        if (companionObjectMembers.all { it is PsiMethod && it.hasModifierProperty(PsiModifier.PRIVATE) }) {
            return nestedClasses.any { nestedClass -> companionObjectMembers.any { converter.referenceSearcher.findMethodCalls(it as PsiMethod, nestedClass).isNotEmpty() } }
        }
        else {
            return true
        }
    }

    private fun processAccessorsToDrop() {
        val fieldToGetterInfo = HashMap<PsiField, AccessorInfo>()
        val fieldToSetterInfo = HashMap<PsiField, AccessorInfo>()
        val fieldsWithConflict = HashSet<PsiField>()
        for (method in psiClass.getMethods()) {
            val info = getAccessorInfo(method) ?: continue
            if (method.getHierarchicalMethodSignature().getSuperSignatures().isNotEmpty()) continue // overrides or implements something
            val map = if (info.kind == AccessorKind.GETTER) fieldToGetterInfo else fieldToSetterInfo

            val prevInfo = map[info.field]
            if (prevInfo != null) {
                fieldsWithConflict.add(info.field)
                continue
            }

            map[info.field] = info
        }

        for ((field, getterInfo) in fieldToGetterInfo) {
            val propertyName = getterInfo.propertyName
            val setterInfo = run {
                val info = fieldToSetterInfo[field]
                if (info?.propertyName == propertyName) info else null
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
            fieldCorrections[field] = FieldCorrectionInfo(propertyName, getterAccess, setterAccess)

            converter.addUsageProcessing(AccessorToPropertyProcessing(getterInfo.method, AccessorKind.GETTER, propertyName))
            if (setterInfo != null) {
                converter.addUsageProcessing(AccessorToPropertyProcessing(setterInfo.method, AccessorKind.SETTER, propertyName))
            }
        }
    }

    private class AccessorInfo(val method: PsiMethod, val field: PsiField, val kind: AccessorKind, val propertyName: String)

    private fun getAccessorInfo(method: PsiMethod): AccessorInfo? {
        val name = method.getName()
        val static = method.hasModifierProperty(PsiModifier.STATIC)
        if (name.startsWith("get") && method.getParameterList().getParametersCount() == 0) {
            val body = method.getBody() ?: return null
            val returnStatement = (body.getStatements().singleOrNull() as? PsiReturnStatement) ?: return null
            val field = fieldByExpression(returnStatement.getReturnValue(), static) ?: return null
            if (field.getType() != method.getReturnType()) return null
            if (converter.typeConverter.variableMutability(field) != converter.typeConverter.methodMutability(method)) return null
            val propertyName = StringUtil.decapitalize(name.substring("get".length()))
            return AccessorInfo(method, field, AccessorKind.GETTER, propertyName)
        }
        else if (name.startsWith("set") && method.getParameterList().getParametersCount() == 1) {
            val body = method.getBody() ?: return null
            val statement = (body.getStatements().singleOrNull() as? PsiExpressionStatement) ?: return null
            val assignment = statement.getExpression() as? PsiAssignmentExpression ?: return null
            if (assignment.getOperationTokenType() != JavaTokenType.EQ) return null
            val field = fieldByExpression(assignment.getLExpression(), static) ?: return null
            val parameter = method.getParameterList().getParameters().single()
            if ((assignment.getRExpression() as? PsiReferenceExpression)?.resolve() != parameter) return null
            if (field.getType() != parameter.getType()) return null
            val propertyName = StringUtil.decapitalize(name.substring("set".length()))
            return AccessorInfo(method, field, AccessorKind.SETTER, propertyName)
        }
        else {
            return null
        }
    }

    private fun fieldByExpression(expression: PsiExpression?, static: Boolean): PsiField? {
        val refExpr = expression as? PsiReferenceExpression ?: return null
        if (static) {
            if (!refExpr.isQualifierEmptyOrClass(psiClass)) return null
        }
        else {
            if (!refExpr.isQualifierEmptyOrThis()) return null
        }
        val field = refExpr.resolve() as? PsiField ?: return null
        if (field.getContainingClass() != psiClass || field.hasModifierProperty(PsiModifier.STATIC) != static) return null
        return field
    }
}
