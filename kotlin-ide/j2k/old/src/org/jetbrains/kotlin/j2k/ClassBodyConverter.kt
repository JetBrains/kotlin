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
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.j2k.usageProcessing.AccessorToPropertyProcessing
import org.jetbrains.kotlin.j2k.usageProcessing.MemberIntoObjectProcessing
import org.jetbrains.kotlin.j2k.usageProcessing.ToObjectWithOnlyMethodsProcessing
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.SpecialNames
import java.util.*

enum class AccessorKind {
    GETTER,
    SETTER
}

enum class ClassKind {
    FINAL_CLASS,
    OPEN_CLASS,
    INTERFACE,
    OBJECT,
    ANONYMOUS_OBJECT,
    FINAL_ENUM,
    OPEN_ENUM,
    ANNOTATION_CLASS;

    fun isObject() = this == OBJECT || this == ANONYMOUS_OBJECT
    fun isOpen() = this == OPEN_CLASS || this == INTERFACE || this == OPEN_ENUM
    fun isEnum() = this == FINAL_ENUM || this == OPEN_ENUM
}

class ClassBodyConverter(private val psiClass: PsiClass,
                         private val classKind: ClassKind,
                         private val converter: Converter
) {
    private val fieldsToDrop = HashSet<PsiField>()

    fun convertBody(): ClassBody {
        val memberToPropertyInfo = converter.propertyDetectionCache[psiClass]

        for ((member, propertyInfo) in memberToPropertyInfo) {
            if (member is PsiMethod) {
                if (member == propertyInfo.getMethod) {
                    converter.addUsageProcessing(AccessorToPropertyProcessing(member, AccessorKind.GETTER, propertyInfo.name))
                }
                else {
                    converter.addUsageProcessing(AccessorToPropertyProcessing(member, AccessorKind.SETTER, propertyInfo.name))
                }
            }
        }

        val overloadReducer = OverloadReducer(psiClass.methods.filter { !memberToPropertyInfo.containsKey(it) } /* do not allow OverloadReducer to use accessors converted to properties */,
                                              classKind.isOpen(),
                                              converter.referenceSearcher)

        val constructorConverter = if (psiClass.name != null && !classKind.isObject())
            ConstructorConverter(psiClass, converter, { field -> memberToPropertyInfo[field]!! }, overloadReducer)
        else
            null

        val convertedMembers = LinkedHashMap<PsiMember, Member>()
        for (element in psiClass.children) {
            if (element is PsiMember) {
                if (element is PsiAnnotationMethod) continue // converted in convertAnnotationType()
                if (classKind.isObject() && element.isConstructor()) continue // no constructor in object
                if (element is PsiMethod && overloadReducer.shouldDropMethod(element)) continue

                val converted = convertMember(element, constructorConverter, overloadReducer, memberToPropertyInfo)
                if (converted != null) {
                    convertedMembers.put(element, converted)
                }
            }
        }

        fieldsToDrop.forEach { convertedMembers.remove(it) }

        val lBrace = LBrace().assignPrototype(psiClass.lBrace)
        val rBrace = RBrace().assignPrototype(psiClass.rBrace)

        if (classKind.isObject()) {
            val psiMembers = convertedMembers.keys
            if (psiMembers.all { it is PsiMethod }) { // for object with no fields we can use faster external usage processing
                converter.addUsageProcessing(ToObjectWithOnlyMethodsProcessing(psiClass))
            }
            else {
                for (psiMember in psiMembers) {
                    if (!psiMember.hasModifierProperty(PsiModifier.PRIVATE)) {
                        converter.addUsageProcessing(MemberIntoObjectProcessing(psiMember, JvmAbi.INSTANCE_FIELD))
                    }
                }
            }

            return ClassBody(null, null, null, convertedMembers.values.toList(), emptyList(), lBrace, rBrace, classKind)
        }

        val useCompanionObject = shouldGenerateCompanionObject(convertedMembers)

        val members = ArrayList<Member>()
        val companionObjectMembers = ArrayList<Member>()
        var primaryConstructorSignature: PrimaryConstructorSignature? = null
        var primaryConstructor: PrimaryConstructor? = null
        for ((psiMember, member) in convertedMembers) {
            if (member is PrimaryConstructor) {
                primaryConstructor = member
                assert(primaryConstructorSignature == null)
                primaryConstructorSignature = primaryConstructor.createSignature(converter)
                members.add(primaryConstructor.initializer)
            }
            else if (useCompanionObject && member !is Class && psiMember !is PsiEnumConstant && psiMember.hasModifierProperty(PsiModifier.STATIC)) {
                companionObjectMembers.add(member)
                if (!psiMember.hasModifierProperty(PsiModifier.PRIVATE)) {
                    converter.addUsageProcessing(MemberIntoObjectProcessing(psiMember, SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.identifier))
                }
            }
            else {
                members.add(member)
            }
        }

        if (primaryConstructor != null
            && primaryConstructorSignature != null
            && classKind != ClassKind.ANONYMOUS_OBJECT
            && primaryConstructorSignature.annotations.isEmpty
            && primaryConstructorSignature.accessModifier == null
            && primaryConstructorSignature.parameterList.parameters.isEmpty()
            && members.none { it is SecondaryConstructor }
        ) {
            primaryConstructorSignature = null // no "()" after class name is needed in this case
        }

        return ClassBody(primaryConstructor, primaryConstructorSignature, constructorConverter?.baseClassParams, members, companionObjectMembers, lBrace, rBrace, classKind)
    }

    private fun convertMember(
            member: PsiMember,
            constructorConverter: ConstructorConverter?,
            overloadReducer: OverloadReducer,
            memberToPropertyInfo: Map<PsiMember, PropertyInfo>
    ): Member? {
        when (member) {
            is PsiMethod -> {
                memberToPropertyInfo[member]?.let { propertyInfo ->
                    if (propertyInfo.field != null) return null // just drop the method, property will be generated when converting the field
                    return if (member == propertyInfo.getMethod || propertyInfo.getMethod == null)
                        converter.convertProperty(propertyInfo, classKind)
                    else
                        null // drop the method, property will be generated when converting the field or the getter
                }

                return converter.convertMethod(member, fieldsToDrop, constructorConverter, overloadReducer, classKind)
            }

            is PsiField -> {
                val propertyInfo = memberToPropertyInfo[member]!!
                return converter.convertProperty(propertyInfo, classKind)
            }

            is PsiClass -> return converter.convertClass(member)

            is PsiClassInitializer -> return converter.convertInitializer(member)

            else -> throw IllegalArgumentException("Unknown member: $member")
        }
    }

    // do not convert private static methods into companion object if possible
    private fun shouldGenerateCompanionObject(convertedMembers: Map<PsiMember, Member>): Boolean {
        val members = convertedMembers.keys.filter { !it.isConstructor() }
        val companionObjectMembers = members.filter { it !is PsiClass && it !is PsiEnumConstant && it.hasModifierProperty(PsiModifier.STATIC) }
        val nestedClasses = members.filterIsInstance<PsiClass>().filter { it.hasModifierProperty(PsiModifier.STATIC) }
        return if (companionObjectMembers.all { it is PsiMethod && it.hasModifierProperty(PsiModifier.PRIVATE) }) {
            nestedClasses.any { nestedClass -> companionObjectMembers.any { converter.referenceSearcher.findMethodCalls(it as PsiMethod, nestedClass).isNotEmpty() } }
        }
        else {
            true
        }
    }
}
