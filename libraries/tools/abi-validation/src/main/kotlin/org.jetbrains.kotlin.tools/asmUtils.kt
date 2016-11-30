package org.jetbrains.kotlin.tools

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import kotlin.comparisons.*

val ACCESS_NAMES = mapOf(
        Opcodes.ACC_PUBLIC to "public",
        Opcodes.ACC_PROTECTED to "protected",
        Opcodes.ACC_PRIVATE to "private",
        Opcodes.ACC_STATIC to "static",
        Opcodes.ACC_FINAL to "final",
        Opcodes.ACC_ABSTRACT  to "abstract",
        Opcodes.ACC_SYNTHETIC to "synthetic",
        Opcodes.ACC_INTERFACE to "interface",
        Opcodes.ACC_ANNOTATION to "annotation")

data class ClassBinarySignature(
        val name: String,
        val superName: String,
        val outerName: String?,
        val supertypes: List<String>,
        val memberSignatures: List<MemberBinarySignature>,
        val access: AccessFlags,
        val isEffectivelyPublic: Boolean,
        val isNotUsedWhenEmpty: Boolean) {

    val signature: String
        get() = "${access.getModifierString()} class $name" + if (supertypes.isEmpty()) "" else " : ${supertypes.joinToString()}"

}


interface MemberBinarySignature {
    val name: String
    val desc: String
    val access: AccessFlags
    val isInlineExposed: Boolean

    fun isEffectivelyPublic(classAccess: AccessFlags, classVisibility: ClassVisibility?)
            = access.isPublic && !(access.isProtected && classAccess.isFinal)
            && (findMemberVisibility(classVisibility)?.isPublic(isInlineExposed) ?: true)

    fun findMemberVisibility(classVisibility: ClassVisibility?)
            = classVisibility?.members?.get(MemberSignature(name, desc))

    val signature: String
}

data class MethodBinarySignature(
        override val name: String,
        override val desc: String,
        override val isInlineExposed: Boolean,
        override val access: AccessFlags) : MemberBinarySignature {
    override val signature: String
        get() = "${access.getModifierString()} fun $name $desc"

    override fun isEffectivelyPublic(classAccess: AccessFlags, classVisibility: ClassVisibility?)
            = super.isEffectivelyPublic(classAccess, classVisibility)
            && !isAccessMethod()

    private fun isAccessMethod() = access.isSynthetic && name.startsWith("access\$")
}

data class FieldBinarySignature(
        override val name: String,
        override val desc: String,
        override val isInlineExposed: Boolean,
        override val access: AccessFlags) : MemberBinarySignature {
    override val signature: String
        get() = "${access.getModifierString()} field $name $desc"

    override fun findMemberVisibility(classVisibility: ClassVisibility?): MemberVisibility? {
        val fieldVisibility = super.findMemberVisibility(classVisibility) ?: return null

        // good case for 'satisfying': fieldVisibility.satisfying { it.isLateInit() }?.let { classVisibility?.findSetterForProperty(it) }
        if (fieldVisibility.isLateInit()) {
            classVisibility?.findSetterForProperty(fieldVisibility)?.let { return it }
        }
        return fieldVisibility
    }
}

val MemberBinarySignature.kind: Int get() = when (this) {
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

fun isPublic(access: Int) = access and Opcodes.ACC_PUBLIC != 0 || access and Opcodes.ACC_PROTECTED != 0
fun isProtected(access: Int) = access and Opcodes.ACC_PROTECTED != 0
fun isStatic(access: Int) = access and Opcodes.ACC_STATIC != 0
fun isFinal(access: Int) = access and Opcodes.ACC_FINAL != 0
fun isSynthetic(access: Int) = access and Opcodes.ACC_SYNTHETIC != 0


fun ClassNode.isEffectivelyPublic(classVisibility: ClassVisibility?) =
        isPublic(access)
                && !isLocal()
                && !isWhenMappings()
                && (classVisibility?.isPublic(isInlineExposed()) ?: true)


val ClassNode.innerClassNode: InnerClassNode? get() = innerClasses.singleOrNull { it.name == name }
fun ClassNode.isLocal() = innerClassNode?.run { innerName == null && outerName == null} ?: false
fun ClassNode.isInner() = innerClassNode != null
fun ClassNode.isWhenMappings() = isSynthetic(access) && name.endsWith("\$WhenMappings")

val ClassNode.effectiveAccess: Int get() = innerClassNode?.access ?: access
val ClassNode.outerClassName: String? get() = innerClassNode?.outerName


const val inlineExposedAnnotationName = "kotlin/PublishedApi"
fun ClassNode.isInlineExposed() = findAnnotation(inlineExposedAnnotationName, includeInvisible = true) != null
fun MethodNode.isInlineExposed() = findAnnotation(inlineExposedAnnotationName, includeInvisible = true) != null
fun FieldNode.isInlineExposed() = findAnnotation(inlineExposedAnnotationName, includeInvisible = true) != null


private object KotlinClassKind {
    const val FILE = 2
    const val SYNTHETIC_CLASS = 3
    const val MULTIPART_FACADE = 4

    val FILE_OR_MULTIPART_FACADE_KINDS = listOf(FILE, MULTIPART_FACADE)
}

fun ClassNode.isFileOrMultipartFacade() = kotlinClassKind.let { it != null && it in KotlinClassKind.FILE_OR_MULTIPART_FACADE_KINDS }
fun ClassNode.isDefaultImpls() = isInner() && name.endsWith("\$DefaultImpls") && kotlinClassKind == KotlinClassKind.SYNTHETIC_CLASS


val ClassNode.kotlinClassKind: Int?
    get() = findAnnotation("kotlin/Metadata", false)?.get("k") as Int?

fun ClassNode.findAnnotation(annotationName: String, includeInvisible: Boolean = false) = findAnnotation(annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible)
fun MethodNode.findAnnotation(annotationName: String, includeInvisible: Boolean = false) = findAnnotation(annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible)
fun FieldNode.findAnnotation(annotationName: String, includeInvisible: Boolean = false) = findAnnotation(annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible)

operator fun AnnotationNode.get(key: String): Any? = values.annotationValue(key)

private fun List<Any>.annotationValue(key: String): Any? {
    for (index in (0 .. size / 2 - 1)) {
        if (this[index*2] == key)
            return this[index*2 + 1]
    }
    return null
}

private fun findAnnotation(annotationName: String, visibleAnnotations: List<AnnotationNode>?, invisibleAnnotations: List<AnnotationNode>?, includeInvisible: Boolean): AnnotationNode? =
        visibleAnnotations?.firstOrNull { it.refersToName(annotationName) } ?:
        if (includeInvisible) invisibleAnnotations?.firstOrNull { it.refersToName(annotationName) } else null

fun AnnotationNode.refersToName(name: String) = desc.startsWith('L') && desc.endsWith(';') && desc.regionMatches(1, name, 0, name.length)