/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.validation.api

import kotlinx.metadata.jvm.*

data class ClassBinarySignature(
    val name: String,
    val superName: String,
    val outerName: String?,
    val supertypes: List<String>,
    val memberSignatures: List<MemberBinarySignature>,
    val access: AccessFlags,
    val isEffectivelyPublic: Boolean,
    val isNotUsedWhenEmpty: Boolean
) {
    val signature: String
        get() = "${access.getModifierString()} class $name" + if (supertypes.isEmpty()) "" else " : ${supertypes.joinToString()}"
}

interface MemberBinarySignature {
    val jvmMember: JvmMemberSignature
    val name: String get() = jvmMember.name
    val desc: String get() = jvmMember.desc
    val access: AccessFlags
    val isPublishedApi: Boolean

    fun isEffectivelyPublic(classAccess: AccessFlags, classVisibility: ClassVisibility?) =
        access.isPublic && !(access.isProtected && classAccess.isFinal)
                && (findMemberVisibility(classVisibility)?.isPublic(isPublishedApi) ?: true)

    fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
        return classVisibility?.findMember(jvmMember)
    }

    val signature: String
}

data class MethodBinarySignature(
    override val jvmMember: JvmMethodSignature,
    override val isPublishedApi: Boolean,
    override val access: AccessFlags
) : MemberBinarySignature {
    override val signature: String
        get() = "${access.getModifierString()} fun $name $desc"

    override fun isEffectivelyPublic(classAccess: AccessFlags, classVisibility: ClassVisibility?) =
        super.isEffectivelyPublic(classAccess, classVisibility)
                && !isAccessOrAnnotationsMethod()
                && !isDummyDefaultConstructor()

    override fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
        return super.findMemberVisibility(classVisibility) ?: classVisibility?.let { alternateDefaultSignature(it.name)?.let(it::findMember) }
    }

    private fun isAccessOrAnnotationsMethod() = access.isSynthetic && (name.startsWith("access\$") || name.endsWith("\$annotations"))

    private fun isDummyDefaultConstructor() = access.isSynthetic && name == "<init>" && desc == "(Lkotlin/jvm/internal/DefaultConstructorMarker;)V"

    /**
     * Calculates the signature of this method without default parameters
     *
     * Returns `null` if this method isn't an entry point of a function
     * or a constructor with default parameters.
     * Returns an incorrect result, if there are more than 31 default parameters.
     */
    private fun alternateDefaultSignature(className: String): JvmMethodSignature? {
        return when {
            !access.isSynthetic -> null
            name == "<init>" && "ILkotlin/jvm/internal/DefaultConstructorMarker;" in desc ->
                JvmMethodSignature(name, desc.replace("ILkotlin/jvm/internal/DefaultConstructorMarker;", ""))
            name.endsWith("\$default") && "ILjava/lang/Object;)" in desc ->
                JvmMethodSignature(
                    name.removeSuffix("\$default"),
                    desc.replace("ILjava/lang/Object;)", ")").replace("(L$className;", "(")
                )
            else -> null
        }
    }
}

data class FieldBinarySignature(
    override val jvmMember: JvmFieldSignature,
    override val isPublishedApi: Boolean,
    override val access: AccessFlags
) : MemberBinarySignature {
    override val signature: String
        get() = "${access.getModifierString()} field $name $desc"

    override fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
        return super.findMemberVisibility(classVisibility)
            ?: takeIf { access.isStatic }?.let { super.findMemberVisibility(classVisibility?.companionVisibilities) }
    }
}

private val MemberBinarySignature.kind: Int
    get() = when (this) {
        is FieldBinarySignature -> 1
        is MethodBinarySignature -> 2
        else -> error("Unsupported $this")
    }

val MEMBER_SORT_ORDER = compareBy<MemberBinarySignature>(
    { it.kind },
    { it.name },
    { it.desc }
)

data class AccessFlags(val access: Int) {
    val isPublic: Boolean get() = isPublic(access)
    val isProtected: Boolean get() = isProtected(access)
    val isStatic: Boolean get() = isStatic(access)
    val isFinal: Boolean get() = isFinal(access)
    val isSynthetic: Boolean get() = isSynthetic(access)

    fun getModifiers(): List<String> = ACCESS_NAMES.entries.mapNotNull { if (access and it.key != 0) it.value else null }
    fun getModifierString(): String = getModifiers().joinToString(" ")
}

