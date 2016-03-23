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


public fun isPublic(access: Int) = access and Opcodes.ACC_PUBLIC != 0 || access and Opcodes.ACC_PROTECTED != 0
public fun isStatic(access: Int) = access and Opcodes.ACC_STATIC != 0
fun getModifiers(access: Int): List<String> = ACCESS_NAMES.entries.mapNotNull { if (access and it.key != 0) it.value else null }
fun getModifierString(access: Int): String = getModifiers(access).joinToString(" ")

fun ClassNode.isSynthetic() = access and Opcodes.ACC_SYNTHETIC != 0
fun MethodNode.isSynthetic() = access and Opcodes.ACC_SYNTHETIC != 0
fun ClassNode.isPublic() = isPublic(access)
fun MethodNode.isPublic() = isPublic(access)
fun FieldNode.isPublic() = isPublic(access)


fun ClassNode.isEffectivelyPublic(classVisibility: ClassVisibility?) =
        isPublic()
                && !isLocal()
                && !isWhenMappings()
                && (classVisibility?.isPublic(isInlineExposed()) ?: true)
                && !isNonPublicFileOrFacade(classVisibility)

fun ClassNode.isNonPublicFileOrFacade(classVisibility: ClassVisibility?) =
        isFileOrMultipartFacade()
                && methods.none { it.isEffectivelyPublic(classVisibility) }
                && fields.none { it.isEffectivelyPublic(classVisibility) }


fun MethodNode.isEffectivelyPublic(classVisibility: ClassVisibility?) =
        isPublic()
                && (classVisibility?.members?.get(MemberSignature(name, desc))?.isPublic(isInlineExposed()) ?: true)
                && !isAccessMethod()

fun FieldNode.isEffectivelyPublic(classVisibility: ClassVisibility?) =
        isPublic()
                && (classVisibility?.members?.get(MemberSignature(name, desc))?.isPublic(isInlineExposed()) ?: true)
// TODO: lateinit exposed field


fun ClassNode.isLocal() = innerClasses.filter { it.name == name && it.innerName == null && it.outerName == null }.count() == 1
fun ClassNode.isWhenMappings() = isSynthetic() && name.endsWith("\$WhenMappings")
fun MethodNode.isAccessMethod() = isSynthetic() && name.startsWith("access\$")

val ClassNode.effectiveAccess: Int get() = innerClasses.singleOrNull { it.name == name }?.access ?: access

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

val ClassNode.outerClassName: String?
    get() = innerClasses.singleOrNull { it.name == name }?.outerName

