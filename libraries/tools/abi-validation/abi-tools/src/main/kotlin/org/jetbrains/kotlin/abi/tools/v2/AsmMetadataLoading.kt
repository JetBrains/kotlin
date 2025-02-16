/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.v2

import kotlin.metadata.jvm.*
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.*

internal val ACCESS_NAMES = mapOf(
    Opcodes.ACC_PUBLIC to "public",
    Opcodes.ACC_PROTECTED to "protected",
    Opcodes.ACC_PRIVATE to "private",
    Opcodes.ACC_STATIC to "static",
    Opcodes.ACC_FINAL to "final",
    Opcodes.ACC_ABSTRACT to "abstract",
    Opcodes.ACC_SYNTHETIC to "synthetic",
    Opcodes.ACC_INTERFACE to "interface",
    Opcodes.ACC_ANNOTATION to "annotation"
)

internal fun isPublic(access: Int) = access and Opcodes.ACC_PUBLIC != 0 || access and Opcodes.ACC_PROTECTED != 0
internal fun isProtected(access: Int) = access and Opcodes.ACC_PROTECTED != 0
internal fun isStatic(access: Int) = access and Opcodes.ACC_STATIC != 0
internal fun isFinal(access: Int) = access and Opcodes.ACC_FINAL != 0
internal fun isSynthetic(access: Int) = access and Opcodes.ACC_SYNTHETIC != 0
internal fun isAbstract(access: Int) = access and Opcodes.ACC_ABSTRACT != 0
internal fun isInterface(access: Int) = access and Opcodes.ACC_INTERFACE != 0

internal fun ClassNode.isEffectivelyPublic(classVisibility: ClassVisibility?) =
    isPublic(access)
            && !isLocal()
            && !isWhenMappings()
            && !isSyntheticAnnotationClass()
            && !isEnumEntriesMappings()
            && (classVisibility?.isPublic(isPublishedApi()) ?: true)


private val ClassNode.innerClassNode: InnerClassNode? get() = innerClasses.singleOrNull { it.name == name }
private fun ClassNode.isLocal() = outerMethod != null
private fun ClassNode.isInner() = innerClassNode != null
private fun ClassNode.isWhenMappings() = isSynthetic(access) && name.endsWith("\$WhenMappings")
private fun ClassNode.isSyntheticAnnotationClass() = isSynthetic(access) && name.contains("\$annotationImpl\$")
private fun ClassNode.isEnumEntriesMappings() = isSynthetic(access) && name.endsWith("\$EntriesMappings")

internal val ClassNode.effectiveAccess: Int get() = innerClassNode?.access ?: access
internal val ClassNode.outerClassName: String? get() = innerClassNode?.outerName


private const val publishedApiAnnotationName = "kotlin/PublishedApi"
private fun ClassNode.isPublishedApi() = findAnnotation(publishedApiAnnotationName, includeInvisible = true) != null
internal fun List<AnnotationNode>.isPublishedApi() = firstOrNull { it.refersToName(publishedApiAnnotationName) } != null

internal const val DefaultImplsNameSuffix = "\$DefaultImpls"
internal fun ClassNode.isDefaultImpls(metadata: KotlinClassMetadata?) =
    isInner() && name.endsWith(DefaultImplsNameSuffix) && metadata.isSyntheticClass()

internal fun ClassNode.findAnnotation(annotationName: String, includeInvisible: Boolean = false) =
    findAnnotation(annotationName, visibleAnnotations, invisibleAnnotations, includeInvisible)
internal operator fun AnnotationNode.get(key: String): Any? = values.annotationValue(key)

private fun List<Any>.annotationValue(key: String): Any? {
    for (index in (0 until size / 2)) {
        if (this[index * 2] == key)
            return this[index * 2 + 1]
    }
    return null
}

private fun findAnnotation(
    annotationName: String,
    visibleAnnotations: List<AnnotationNode>?,
    invisibleAnnotations: List<AnnotationNode>?,
    includeInvisible: Boolean
): AnnotationNode? =
    visibleAnnotations?.firstOrNull { it.refersToName(annotationName) }
        ?: if (includeInvisible) invisibleAnnotations?.firstOrNull { it.refersToName(annotationName) } else null

internal fun AnnotationNode.refersToName(name: String) =
    desc.startsWith('L') && desc.endsWith(';') && desc.regionMatches(1, name, 0, name.length)
