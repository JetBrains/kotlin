package org.jetbrains.kotlin.tools

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

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
        val isEffectivelyPublic: Boolean) {

    val signature: String
        get() = "${access.getModifierString()} class $name" + if (supertypes.isEmpty()) "" else ": ${supertypes.joinToString()}"

}


interface MemberBinarySignature {
    val name: String
    val desc: String
    val access: AccessFlags
    val isInlineExposed: Boolean

    fun isEffectivelyPublic(classAccess: AccessFlags, classVisibility: ClassVisibility?)
            = access.isPublic && !(access.isProtected && classAccess.isFinal)
            && (classVisibility?.members?.get(MemberSignature(name, desc))?.isPublic(isInlineExposed) ?: true)

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

    override fun isEffectivelyPublic(classAccess: AccessFlags, classVisibility: ClassVisibility?)
            = super.isEffectivelyPublic(classAccess, classVisibility)
    // TODO: lateinit exposed field
}



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
fun ClassNode.isWhenMappings() = isSynthetic(access) && name.endsWith("\$WhenMappings")

val ClassNode.effectiveAccess: Int get() = innerClassNode?.access ?: access
val ClassNode.outerClassName: String? get() = innerClassNode?.outerName


const val inlineExposedAnnotationName = "kotlin/internal/InlineExposed"
fun ClassNode.isInlineExposed() = hasAnnotation(inlineExposedAnnotationName, includeInvisible = true)
fun MethodNode.isInlineExposed() = hasAnnotation(inlineExposedAnnotationName, includeInvisible = true)
fun FieldNode.isInlineExposed() = hasAnnotation(inlineExposedAnnotationName, includeInvisible = true)

private object KotlinClassKind {
    const val FILE = 2
    const val MULTIPART_FACADE = 4

    val FILE_OR_MULTIPART_FACADE_KINDS = listOf(FILE, MULTIPART_FACADE)
}

fun ClassNode.isFileOrMultipartFacade() = kotlinClassKind.let { it != null && it in KotlinClassKind.FILE_OR_MULTIPART_FACADE_KINDS }


val ClassNode.kotlinClassKind: Int?
    get() = visibleAnnotations
            ?.filter { it.desc == "Lkotlin/Metadata;" }
            ?.map { (it.values.annotationValue("k") as? Int) }
            ?.firstOrNull()

fun ClassNode.hasAnnotation(annotationName: String, includeInvisible: Boolean = false) = hasAnnotation(annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible)
fun MethodNode.hasAnnotation(annotationName: String, includeInvisible: Boolean = false) = hasAnnotation(annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible)
fun FieldNode.hasAnnotation(annotationName: String, includeInvisible: Boolean = false) = hasAnnotation(annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible)

private fun hasAnnotation(annotationName: String, visibleAnnotations: List<AnnotationNode>?, invisibleAnnotations: List<AnnotationNode>?, includeInvisible: Boolean)
        = "L$annotationName;".let { desc ->
            (visibleAnnotations?.any { it.desc == desc } ?: false)
            || (includeInvisible && (invisibleAnnotations?.any { it.desc == desc } ?: false))
        }

private fun List<Any>.annotationValue(key: String): Any? {
    for (index in (0 .. size / 2 - 1)) {
        if (this[index*2] == key)
            return this[index*2 + 1]
    }
    return null
}
