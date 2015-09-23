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

import com.intellij.psi.*
import com.intellij.psi.util.MethodSignatureUtil
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*

class PropertyInfo(
        val identifier: Identifier,
        val isVar: Boolean,
        val psiType: PsiType,
        val field: PsiField?,
        val getMethod: PsiMethod?,
        val setMethod: PsiMethod?,
        val isGetMethodBodyFieldAccess: Boolean,
        val isSetMethodBodyFieldAccess: Boolean,
        val modifiers: Modifiers,
        val specialSetterAccess: Modifier?
) {
    init {
        assert(field != null || getMethod != null || setMethod != null)
        if (isGetMethodBodyFieldAccess) {
            assert(field != null && getMethod != null)
        }
        if (isSetMethodBodyFieldAccess) {
            assert(field != null && setMethod != null)
        }
    }

    val name: String
        get() = identifier.name

    //TODO: what if annotations are not empty?
    val needExplicitGetter: Boolean
        get() {
            if (getMethod != null && getMethod.body != null && !isGetMethodBodyFieldAccess) return true
            return modifiers.contains(Modifier.OVERRIDE) && this.field == null && !modifiers.contains(Modifier.ABSTRACT)
        }

    //TODO: what if annotations are not empty?
    val needExplicitSetter: Boolean
        get() {
            if (!isVar) return false
            if (specialSetterAccess != null) return true
            if (setMethod != null && setMethod.body != null && !isSetMethodBodyFieldAccess) return true
            return modifiers.contains(Modifier.OVERRIDE) && this.field == null && !modifiers.contains(Modifier.ABSTRACT)
        }

    companion object {
        fun fromFieldWithNoAccessors(field: PsiField, converter: Converter): PropertyInfo {
            val isVar = field.isVar(converter.referenceSearcher)
            val modifiers = converter.convertModifiers(field, false)
            return PropertyInfo(field.declarationIdentifier(), isVar, field.type, field, null, null, false, false, modifiers, null)
        }
    }
}

class PropertyDetectionCache(private val converter: Converter) {
    private val cache = HashMap<PsiClass, Map<PsiMember, PropertyInfo>>()

    fun get(psiClass: PsiClass): Map<PsiMember, PropertyInfo> {
        cache[psiClass]?.let { return it }

        assert(converter.inConversionScope(psiClass))

        val detected = PropertyDetector(psiClass, converter).detectProperties()
        cache[psiClass] = detected
        return detected
    }
}

private class PropertyDetector(
        private val psiClass: PsiClass,
        private val converter: Converter
) {
    private val isOpenClass = converter.needOpenModifier(psiClass)

    public fun detectProperties(): Map<PsiMember, PropertyInfo> {
        val propertyNameToGetterInfo = LinkedHashMap<String, AccessorInfo>()
        val propertyNameToSetterInfo = LinkedHashMap<String, AccessorInfo>()
        val propertyNamesWithConflict = HashSet<String>()
        val prohibitedPropertyNames = psiClass.fields.map { it.name }.toMutableSet() //TODO: fields from base
        for (method in psiClass.getMethods()) {
            val info = getAccessorInfo(method) ?: continue

            val map = if (info.kind == AccessorKind.GETTER) propertyNameToGetterInfo else propertyNameToSetterInfo

            val prevInfo = map[info.propertyName]
            if (prevInfo != null) {
                propertyNamesWithConflict.add(info.propertyName)
                continue
            }

            map[info.propertyName] = info
            info.field?.let { prohibitedPropertyNames.remove(it.name) }
        }

        val memberToPropertyInfo = HashMap<PsiMember, PropertyInfo>()

        val propertyNames = propertyNameToGetterInfo.keySet() + propertyNameToSetterInfo.keySet()
        for (propertyName in propertyNames) {
            // TODO: use "field" expression if the conflicting field is used only inside get/set-methods
            if (propertyName in prohibitedPropertyNames) continue // cannot create such property - will conflict with existing field
            //TODO: what about overrides in this case?

            val getterInfo = propertyNameToGetterInfo[propertyName]
            var setterInfo = propertyNameToSetterInfo[propertyName]

            // no property without getter except for overrides
            if (getterInfo == null && setterInfo!!.method.hierarchicalMethodSignature.superSignatures.isEmpty()) continue

            if (setterInfo != null && getterInfo != null && setterInfo.method.parameterList.parameters.single().type != getterInfo.method.returnType) {
                setterInfo = null
            }

            var field = getterInfo?.field ?: setterInfo?.field

            if (field != null && memberToPropertyInfo.containsKey(field)) { // already used in another property
                field = null
            }

            //TODO: no body for getter OR setter

            val isVar = if (setterInfo != null)
                true
            else if (getterInfo!!.superProperty != null && getterInfo.superProperty!!.isVar)
                true
            else
                field != null && field.isVar(converter.referenceSearcher)

            val type = field?.type ?: getterInfo?.method?.returnType ?: setterInfo!!.method.parameterList.parameters.single()?.type!!

            val isOverride = getterInfo?.superProperty != null || setterInfo?.superProperty != null

            val modifiers = convertModifiers(field, getterInfo?.method, setterInfo?.method, isOverride)

            val propertyAccess = modifiers.accessModifier()
            val setterAccess = if (setterInfo != null)
                converter.convertModifiers(setterInfo.method, false).accessModifier()
            else if (field != null && field.isVar(converter.referenceSearcher))
                converter.convertModifiers(field, false).accessModifier()
            else
                propertyAccess
            val specialSetterAccess = setterAccess?.check { it != propertyAccess }

            val propertyInfo = PropertyInfo(Identifier(propertyName).assignNoPrototype(),
                                            isVar,
                                            type,
                                            field,
                                            getterInfo?.method,
                                            setterInfo?.method,
                                            field != null && getterInfo?.field == field,
                                            field != null && setterInfo?.field == field,
                                            modifiers,
                                            specialSetterAccess)

            if (field != null) {
                memberToPropertyInfo[field] = propertyInfo
            }
            if (getterInfo != null) {
                memberToPropertyInfo[getterInfo.method] = propertyInfo
            }
            if (setterInfo != null) {
                memberToPropertyInfo[setterInfo.method] = propertyInfo
            }
        }

        dropPropertiesWithConflictingAccessors(memberToPropertyInfo)

        val mappedFields = memberToPropertyInfo.values()
                .map { it.field }
                .filterNotNull()
                .toSet()

        // map all other fields
        for (field in psiClass.fields) {
            if (field !in mappedFields) {
                memberToPropertyInfo[field] = PropertyInfo.fromFieldWithNoAccessors(field, converter)
            }
        }

        return memberToPropertyInfo
    }

    private fun dropPropertiesWithConflictingAccessors(memberToPropertyInfo: MutableMap<PsiMember, PropertyInfo>) {
        val propertyInfos = memberToPropertyInfo.values().distinct()

        val mappedMethods = propertyInfos.map { it.getMethod }.filterNotNull().toSet() + propertyInfos.map { it.setMethod }.filterNotNull().toSet()

        //TODO: bases
        val prohibitedSignatures = psiClass.methods
                .filter { it !in mappedMethods }
                .map { it.getSignature(PsiSubstitutor.EMPTY) }
                .toSet()

        fun dropProperty(propertyInfo: PropertyInfo) {
            propertyInfo.field?.let { memberToPropertyInfo.remove(it) }
            propertyInfo.getMethod?.let { memberToPropertyInfo.remove(it) }
            propertyInfo.setMethod?.let { memberToPropertyInfo.remove(it) }
        }

        for (propertyInfo in propertyInfos) {
            if (propertyInfo.modifiers.contains(Modifier.OVERRIDE)) continue // cannot drop override

            //TODO: test this case
            val getterName = JvmAbi.getterName(propertyInfo.name)
            val getterSignature = MethodSignatureUtil.createMethodSignature(getterName, emptyArray(), emptyArray(), PsiSubstitutor.EMPTY)
            if (getterSignature in prohibitedSignatures) {
                dropProperty(propertyInfo)
                continue
            }

            if (propertyInfo.isVar) {
                val setterName = JvmAbi.setterName(propertyInfo.name)
                val setterSignature = MethodSignatureUtil.createMethodSignature(setterName, arrayOf(propertyInfo.psiType), emptyArray(), PsiSubstitutor.EMPTY)
                if (setterSignature in prohibitedSignatures) {
                    dropProperty(propertyInfo)
                    continue
                }
            }
        }
    }

    private fun convertModifiers(field: PsiField?, getMethod: PsiMethod?, setMethod: PsiMethod?, isOverride: Boolean): Modifiers {
        val fieldModifiers = field?.let { converter.convertModifiers(it, false) } ?: Modifiers.Empty
        val getterModifiers = getMethod?.let { converter.convertModifiers(it, isOpenClass) } ?: Modifiers.Empty
        val setterModifiers = setMethod?.let { converter.convertModifiers(it, isOpenClass) } ?: Modifiers.Empty

        val modifiers = ArrayList<Modifier>()

        //TODO: what if one is abstract and another is not?
        val isGetterAbstract = getMethod?.hasModifierProperty(PsiModifier.ABSTRACT) ?: true
        val isSetterAbstract = setMethod?.hasModifierProperty(PsiModifier.ABSTRACT) ?: true
        if (field == null && isGetterAbstract && isSetterAbstract) {
            modifiers.add(Modifier.ABSTRACT)
        }

        if (getterModifiers.contains(Modifier.OPEN) || setterModifiers.contains(Modifier.OPEN)) {
            modifiers.add(Modifier.OPEN)
        }

        if (isOverride) {
            modifiers.add(Modifier.OVERRIDE)
        }

        if (getMethod != null) {
            modifiers.addIfNotNull(getterModifiers.accessModifier())
        }
        else if (setMethod != null) {
            modifiers.addIfNotNull(getterModifiers.accessModifier())
        }
        else {
            modifiers.addIfNotNull(fieldModifiers.accessModifier())
        }

        val prototypes = listOf<PsiElement?>(field, getMethod, setMethod)
                .filterNotNull()
                .map { PrototypeInfo(it, CommentsAndSpacesInheritance.NO_SPACES) }
        return Modifiers(modifiers).assignPrototypes(*prototypes.toTypedArray())
    }

    private class AccessorInfo(
            val method: PsiMethod,
            val field: PsiField?,
            val kind: AccessorKind,
            val propertyName: String,
            val superProperty: SuperPropertyInfo?
    )

    private class SuperPropertyInfo(
            val isVar: Boolean
            //TODO: add visibility
    )

    private fun getAccessorInfo(method: PsiMethod): AccessorInfo? {
        propertyNameByGetMethod(method)?.let { propertyName ->
            val field = fieldFromGetterBody(method)
            return createAccessorInfo(method, field, AccessorKind.GETTER, propertyName)
        }

        propertyNameBySetMethod(method)?.let { propertyName ->
            val field = fieldFromSetterBody(method)
            return createAccessorInfo(method, field, AccessorKind.SETTER, propertyName)
        }

        return null
    }

    private fun createAccessorInfo(getOrSetMethod: PsiMethod, field: PsiField?, kind: AccessorKind, propertyName: String): AccessorInfo? {
        //TODO: multiple
        val superMethod = converter.services.superMethodsSearcher.findDeepestSuperMethods(getOrSetMethod).firstOrNull()
                          ?: return AccessorInfo(getOrSetMethod, field, kind, propertyName, null)

        val containingClass = superMethod.containingClass!!
        val superPropertyInfo: SuperPropertyInfo? = if (converter.inConversionScope(containingClass)) {
            val propertyInfo = converter.propertyDetectionCache[containingClass][superMethod]
            if (propertyInfo != null) SuperPropertyInfo(propertyInfo.isVar) else null
        }
        else if (superMethod is KotlinLightMethod) {
            val origin = superMethod.getOrigin()
            if (origin is JetProperty) SuperPropertyInfo(origin.isVar) else null
        }
        else {
            null
        }
        return superPropertyInfo?.let { AccessorInfo(getOrSetMethod, field, kind, propertyName, it) }
    }

    private fun propertyNameByGetMethod(method: PsiMethod): String? {
        if (method.isConstructor) return null
        if (method.parameterList.parametersCount != 0) return null

        val name = method.name
        if (!Name.isValidIdentifier(name)) return null
        val propertyName = SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(Name.identifier(name))?.identifier ?: return null

        val returnType = method.returnType ?: return null
        if (returnType.canonicalText == "void") return null
        if (method.typeParameters.isNotEmpty()) return null

        return propertyName
    }

    private fun propertyNameBySetMethod(method: PsiMethod): String? {
        if (method.isConstructor) return null
        if (method.parameterList.parametersCount != 1) return null

        val name = method.name
        if (!Name.isValidIdentifier(name)) return null
        val propertyName = SyntheticJavaPropertyDescriptor.propertyNameBySetMethodName(Name.identifier(name), false/*TODO!!*/)?.identifier ?: return null

        if (method.returnType?.canonicalText != "void") return null
        if (method.typeParameters.isNotEmpty()) return null

        return propertyName
    }

    private fun fieldFromGetterBody(getter: PsiMethod): PsiField? {
        val body = getter.getBody() ?: return null
        val returnStatement = (body.getStatements().singleOrNull() as? PsiReturnStatement) ?: return null
        val isStatic = getter.hasModifierProperty(PsiModifier.STATIC)
        val field = fieldByExpression(returnStatement.getReturnValue(), isStatic) ?: return null
        if (field.getType() != getter.getReturnType()) return null
        if (converter.typeConverter.variableMutability(field) != converter.typeConverter.methodMutability(getter)) return null
        return field
    }

    private fun fieldFromSetterBody(setter: PsiMethod): PsiField? {
        val body = setter.getBody() ?: return null
        val statement = (body.getStatements().singleOrNull() as? PsiExpressionStatement) ?: return null
        val assignment = statement.getExpression() as? PsiAssignmentExpression ?: return null
        if (assignment.getOperationTokenType() != JavaTokenType.EQ) return null
        val isStatic = setter.hasModifierProperty(PsiModifier.STATIC)
        val field = fieldByExpression(assignment.getLExpression(), isStatic) ?: return null
        val parameter = setter.getParameterList().getParameters().single()
        if ((assignment.getRExpression() as? PsiReferenceExpression)?.resolve() != parameter) return null
        if (field.getType() != parameter.getType()) return null
        return field
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