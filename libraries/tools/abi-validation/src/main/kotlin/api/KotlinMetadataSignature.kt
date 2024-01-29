/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import kotlinx.metadata.jvm.*
import kotlinx.validation.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.*

@ExternalApi // Only name is part of the API, nothing else is used by stdlib
public data class ClassBinarySignature internal constructor(
    internal val name: String,
    internal val superName: String,
    internal val outerName: String?,
    internal val supertypes: List<String>,
    internal val memberSignatures: List<MemberBinarySignature>,
    internal val access: AccessFlags,
    internal val isEffectivelyPublic: Boolean,
    internal val isNotUsedWhenEmpty: Boolean,
    internal val annotations: List<AnnotationNode>
) {
    internal val signature: String
        get() = "${access.getModifierString()} class $name" + if (supertypes.isEmpty()) "" else " : ${supertypes.joinToString()}"

}

internal interface MemberBinarySignature {
    val jvmMember: JvmMemberSignature
    val name: String get() = jvmMember.name
    val desc: String get() = jvmMember.desc
    val access: AccessFlags
    val isPublishedApi: Boolean
    val annotations: List<AnnotationNode>

    fun isEffectivelyPublic(classAccess: AccessFlags, classVisibility: ClassVisibility?) =
        access.isPublic && !(access.isProtected && classAccess.isFinal)
                && (findMemberVisibility(classVisibility)?.isPublic(isPublishedApi) ?: true)

    fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
        return classVisibility?.findMember(jvmMember)
    }

    val signature: String
}

internal data class MethodBinarySignature(
    override val jvmMember: JvmMethodSignature,
    override val isPublishedApi: Boolean,
    override val access: AccessFlags,
    override val annotations: List<AnnotationNode>,
    private val alternateDefaultSignature: JvmMethodSignature?
) : MemberBinarySignature {
    override val signature: String
        get() = "${access.getModifierString()} fun $name $desc"

    override fun isEffectivelyPublic(classAccess: AccessFlags, classVisibility: ClassVisibility?) =
        super.isEffectivelyPublic(classAccess, classVisibility)
                && !isAccessOrAnnotationsMethod()
                && !isDummyDefaultConstructor()

    override fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
        return super.findMemberVisibility(classVisibility)
            ?: classVisibility?.let { alternateDefaultSignature?.let(it::findMember) }
    }

    private fun isAccessOrAnnotationsMethod() =
        access.isSynthetic && (name.startsWith("access\$") || name.endsWith("\$annotations"))

    private fun isDummyDefaultConstructor() =
        access.isSynthetic && name == "<init>" && desc == "(Lkotlin/jvm/internal/DefaultConstructorMarker;)V"
}

/**
 * Calculates the signature of this method without default parameters
 *
 * Returns `null` if this method isn't an entry point of a function
 * or a constructor with default parameters.
 * Returns an incorrect result, if there are more than 31 default parameters.
 */
internal fun MethodNode.alternateDefaultSignature(className: String): JvmMethodSignature? {
    return when {
        access and Opcodes.ACC_SYNTHETIC == 0 -> null
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

internal fun MethodNode.toMethodBinarySignature(
    /*
     * Extra annotations are:
     * * Annotations from the original method for synthetic `$default` method
     * * Annotations from getter, setter or field for synthetic `$annotation` method
     * * Annotations from a field for getter and setter
     */
    extraAnnotations: List<AnnotationNode>,
    alternateDefaultSignature: JvmMethodSignature?
): MethodBinarySignature {
    val allAnnotations = visibleAnnotations.orEmpty() + invisibleAnnotations.orEmpty() + extraAnnotations
    return MethodBinarySignature(
        JvmMethodSignature(name, desc),
        allAnnotations.isPublishedApi(),
        AccessFlags(access),
        allAnnotations,
        alternateDefaultSignature
    )
}

internal data class FieldBinarySignature(
    override val jvmMember: JvmFieldSignature,
    override val isPublishedApi: Boolean,
    override val access: AccessFlags,
    override val annotations: List<AnnotationNode>
) : MemberBinarySignature {
    override val signature: String
        get() = "${access.getModifierString()} field $name $desc"

    override fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
        return super.findMemberVisibility(classVisibility)
            ?: takeIf { access.isStatic }?.let { super.findMemberVisibility(classVisibility?.companionVisibilities) }
    }
}

internal fun FieldNode.toFieldBinarySignature(extraAnnotations: List<AnnotationNode>): FieldBinarySignature {
    val allAnnotations = visibleAnnotations.orEmpty() + invisibleAnnotations.orEmpty() + extraAnnotations
    return FieldBinarySignature(
        JvmFieldSignature(name, desc),
        allAnnotations.isPublishedApi(),
        AccessFlags(access),
        allAnnotations
    )
}
private val MemberBinarySignature.kind: Int
    get() = when (this) {
        is FieldBinarySignature -> 1
        is MethodBinarySignature -> 2
        else -> error("Unsupported $this")
    }

internal val MEMBER_SORT_ORDER = compareBy<MemberBinarySignature>(
    { it.kind },
    { it.name },
    { it.desc }
)

internal data class AccessFlags(val access: Int) {
    val isPublic: Boolean get() = isPublic(access)
    val isProtected: Boolean get() = isProtected(access)
    val isStatic: Boolean get() = isStatic(access)
    val isFinal: Boolean get() = isFinal(access)
    val isSynthetic: Boolean get() = isSynthetic(access)
    val isAbstract: Boolean get() = isAbstract(access)
    val isInterface: Boolean get() = isInterface(access)

    private fun getModifiers(): List<String> =
        ACCESS_NAMES.entries.mapNotNull { if (access and it.key != 0) it.value else null }

    fun getModifierString(): String = getModifiers().joinToString(" ")
}

internal fun FieldNode.isCompanionField(outerClassMetadata: KotlinClassMetadata?): Boolean {
    val access = AccessFlags(access)
    if (!access.isFinal || !access.isStatic) return false
    val metadata = outerClassMetadata ?: return false
    // Non-classes are not affected by the problem
    if (metadata !is KotlinClassMetadata.Class) return false
    return metadata.toKmClass().companionObject == name
}

internal fun ClassNode.companionName(outerClassMetadata: KotlinClassMetadata?): String {
    val outerKClass = (outerClassMetadata as KotlinClassMetadata.Class).toKmClass()
    return name + "$" + outerKClass.companionObject
}
