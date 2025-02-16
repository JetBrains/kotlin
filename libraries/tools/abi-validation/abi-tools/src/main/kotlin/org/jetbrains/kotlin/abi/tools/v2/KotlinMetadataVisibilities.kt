/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.v2

import kotlin.metadata.*
import kotlin.metadata.jvm.*
import org.jetbrains.org.objectweb.asm.tree.*

internal class ClassVisibility(
    val name: String,
    val visibility: Visibility?,
    val classKind: ClassKind?,
    val members: Map<JvmMemberSignature, MemberVisibility>,
    val facadeClassName: String? = null
) {
    val isCompanion: Boolean get() = classKind == ClassKind.COMPANION_OBJECT
    var companionVisibilities: ClassVisibility? = null
    val partVisibilities = mutableListOf<ClassVisibility>()
}

internal fun ClassVisibility.findMember(signature: JvmMemberSignature): MemberVisibility? =
    members[signature] ?: partVisibilities.mapNotNull { it.members[signature] }.firstOrNull()


internal data class MemberVisibility(
    val member: JvmMemberSignature,
    val visibility: Visibility?,
    val isReified: Boolean,
    /*
     * This property includes both annotations on the member itself,
     * **and**, if the member is a property, annotations on a field itself
     */
    val propertyAnnotation: PropertyAnnotationHolders? = null
)

private fun isPublic(visibility: Visibility?, isPublishedApi: Boolean) =
    visibility == null
            || visibility == Visibility.PUBLIC
            || visibility == Visibility.PROTECTED
            || (isPublishedApi && visibility == Visibility.INTERNAL)

internal fun ClassVisibility.isPublic(isPublishedApi: Boolean) =
    isPublic(visibility, isPublishedApi)

internal fun MemberVisibility.isPublic(isPublishedApi: Boolean) =
    // Assuming isReified implies inline
    !isReified && isPublic(visibility, isPublishedApi)

internal fun MemberVisibility.isInternal(): Boolean = visibility == Visibility.INTERNAL

internal val ClassNode.kotlinMetadata: KotlinClassMetadata?
    get() {
        val metadata = findAnnotation("kotlin/Metadata", false) ?: return null

        @Suppress("UNCHECKED_CAST")
        val header = with(metadata) {
            Metadata(
                kind = get("k") as Int?,
                metadataVersion = (get("mv") as List<Int>?)?.toIntArray(),
                data1 = (get("d1") as List<String>?)?.toTypedArray(),
                data2 = (get("d2") as List<String>?)?.toTypedArray(),
                extraString = get("xs") as String?,
                packageName = get("pn") as String?,
                extraInt = get("xi") as Int?
            )
        }
        return KotlinClassMetadata.readLenient(header)
    }


internal fun KotlinClassMetadata?.isFileOrMultipartFacade() =
    this is KotlinClassMetadata.FileFacade || this is KotlinClassMetadata.MultiFileClassFacade

internal fun KotlinClassMetadata?.isSyntheticClass() = this is KotlinClassMetadata.SyntheticClass

// Auxiliary class that stores signatures of corresponding field and method for a property.
internal class PropertyAnnotationHolders(
    val field: JvmFieldSignature?,
    val method: JvmMethodSignature?,
)

internal fun KotlinClassMetadata.toClassVisibility(classNode: ClassNode): ClassVisibility {
    var visibility: Visibility? = null
    var kind: ClassKind? = null
    var _facadeClassName: String? = null
    val members = mutableListOf<MemberVisibility>()

    fun addMember(
        signature: JvmMemberSignature?,
        visibility: Visibility?,
        isReified: Boolean,
        propertyAnnotation: PropertyAnnotationHolders? = null
    ) {
        if (signature != null) {
            members.add(MemberVisibility(signature, visibility, isReified, propertyAnnotation))
        }
    }

    val container: KmDeclarationContainer? = when (this) {
        is KotlinClassMetadata.Class ->
            kmClass.also { klass ->
                visibility = klass.visibility
                kind = klass.kind

                for (constructor in klass.constructors) {
                    addMember(constructor.signature, constructor.visibility, isReified = false)
                }
            }

        is KotlinClassMetadata.FileFacade ->
            kmPackage

        is KotlinClassMetadata.MultiFileClassPart ->
            kmPackage.also { _facadeClassName = this.facadeClassName }

        else -> null
    }

    if (container != null) {
        fun List<KmTypeParameter>.containsReified() = any { it.isReified }

        for (function in container.functions) {
            addMember(function.signature, function.visibility, function.typeParameters.containsReified())
        }

        for (property in container.properties) {
            val isReified = property.typeParameters.containsReified()
            val propertyAnnotations =
                PropertyAnnotationHolders(property.fieldSignature, property.syntheticMethodForAnnotations)

            addMember(property.getterSignature, property.getter.visibility, isReified, propertyAnnotations)
            addMember(property.setterSignature, property.setter?.visibility, isReified, propertyAnnotations)

            val fieldVisibility = when {
                property.isLateinit -> property.setter!!.visibility
                property.getterSignature == null && property.setterSignature == null -> property.visibility // JvmField or const case
                else -> Visibility.PRIVATE
            }
            addMember(
                property.fieldSignature,
                fieldVisibility,
                isReified = false,
                propertyAnnotation = propertyAnnotations
            )
        }
    }

    return ClassVisibility(classNode.name, visibility, kind, members.associateBy { it.member }, _facadeClassName)
}

internal fun ClassNode.toClassVisibility() = kotlinMetadata?.toClassVisibility(this)

internal fun Map<String, ClassNode>.readKotlinVisibilities(visibilityFilter: (String) -> Boolean = { true }): Map<String, ClassVisibility> =
    /*
     * Optimized sequence of:
     * 1) Map values to visibility
     * 2) Filter keys by visibilityFilter
     * 3) Post-process each value and set facade/companion
     */
    entries
        .mapNotNull { (name, classNode) ->
            if (!visibilityFilter(name)) return@mapNotNull null
            val visibility = classNode.toClassVisibility() ?: return@mapNotNull null
            name to visibility
        }.toMap().apply {
            values.forEach {
                updateCompanionInfo(it)
                updateFacadeInfo(it)
            }
        }

private fun Map<String, ClassVisibility>.updateFacadeInfo(it: ClassVisibility) {
    if (it.facadeClassName == null) return
    getValue(it.facadeClassName).partVisibilities.add(it)
}

private fun Map<String, ClassVisibility>.updateCompanionInfo(it: ClassVisibility) {
    if (!it.isCompanion) return
    val containingClassName = it.name.substringBeforeLast('$')
    getValue(containingClassName).companionVisibilities = it
}

