/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import kotlinx.metadata.*
import kotlinx.metadata.jvm.*
import org.objectweb.asm.tree.ClassNode

class ClassVisibility(
    val name: String,
    val flags: Flags?,
    val members: Map<JvmMemberSignature, MemberVisibility>,
    val facadeClassName: String? = null
) {
    val visibility get() = flags
    val isCompanion: Boolean get() = flags != null && Flag.Class.IS_COMPANION_OBJECT(flags)

    var companionVisibilities: ClassVisibility? = null
    val partVisibilities = mutableListOf<ClassVisibility>()
}

fun ClassVisibility.findMember(signature: JvmMemberSignature): MemberVisibility? =
    members[signature] ?: partVisibilities.mapNotNull { it.members[signature] }.firstOrNull()


data class MemberVisibility(val member: JvmMemberSignature, val visibility: Flags?, val isReified: Boolean)

private fun isPublic(visibility: Flags?, isPublishedApi: Boolean) =
    visibility == null
            || Flag.IS_PUBLIC(visibility)
            || Flag.IS_PROTECTED(visibility)
            || (isPublishedApi && Flag.IS_INTERNAL(visibility))

fun ClassVisibility.isPublic(isPublishedApi: Boolean) =
    isPublic(visibility, isPublishedApi)

fun MemberVisibility.isPublic(isPublishedApi: Boolean) =
        // Assuming isReified implies inline
        !isReified && isPublic(visibility, isPublishedApi)

fun MemberVisibility.isInternal(): Boolean = visibility != null && Flag.IS_INTERNAL(visibility)

val ClassNode.kotlinMetadata: KotlinClassMetadata?
    get() {
        val metadata = findAnnotation("kotlin/Metadata", false) ?: return null
        @Suppress("UNCHECKED_CAST")
        val header = with(metadata) {
            KotlinClassHeader(
                kind = get("k") as Int?,
                metadataVersion = (get("mv") as List<Int>?)?.toIntArray(),
                bytecodeVersion = (get("bv") as List<Int>?)?.toIntArray(),
                data1 = (get("d1") as List<String>?)?.toTypedArray(),
                data2 = (get("d2") as List<String>?)?.toTypedArray(),
                extraString = get("xs") as String?,
                packageName = get("pn") as String?,
                extraInt = get("xi") as Int?
            )
        }
        return KotlinClassMetadata.read(header)
    }


fun KotlinClassMetadata?.isFileOrMultipartFacade() =
    this is KotlinClassMetadata.FileFacade || this is KotlinClassMetadata.MultiFileClassFacade

fun KotlinClassMetadata?.isSyntheticClass() = this is KotlinClassMetadata.SyntheticClass

fun KotlinClassMetadata.toClassVisibility(classNode: ClassNode): ClassVisibility {
    var flags: Flags? = null
    var _facadeClassName: String? = null
    val members = mutableListOf<MemberVisibility>()

    fun addMember(signature: JvmMemberSignature?, flags: Flags, isReified: Boolean) {
        if (signature != null) {
            members.add(MemberVisibility(signature, flags, isReified))
        }
    }

    val container: KmDeclarationContainer? = when (this) {
        is KotlinClassMetadata.Class ->
            toKmClass().also { klass ->
                flags = klass.flags

                for (constructor in klass.constructors) {
                    addMember(constructor.signature, constructor.flags, isReified = false)
                }
            }
        is KotlinClassMetadata.FileFacade ->
            toKmPackage()
        is KotlinClassMetadata.MultiFileClassPart ->
            toKmPackage().also { _facadeClassName = this.facadeClassName }
        else -> null
    }

    if (container != null) {
        fun List<KmTypeParameter>.containsReified() = any { Flag.TypeParameter.IS_REIFIED(it.flags) }

        for (function in container.functions) {
            addMember(function.signature, function.flags, function.typeParameters.containsReified())
        }

        for (property in container.properties) {
            val isReified = property.typeParameters.containsReified()
            addMember(property.getterSignature, property.getterFlags, isReified)
            addMember(property.setterSignature, property.setterFlags, isReified)

            val fieldVisibility = when {
                Flag.Property.IS_LATEINIT(property.flags) -> property.setterFlags
                property.getterSignature == null && property.setterSignature == null -> property.flags // JvmField or const case
                else -> flagsOf(Flag.IS_PRIVATE)
            }
            addMember(property.fieldSignature, fieldVisibility, isReified = false)
        }
    }

    return ClassVisibility(classNode.name, flags, members.associateBy { it.member }, _facadeClassName)
}

fun ClassNode.toClassVisibility() = kotlinMetadata?.toClassVisibility(this)

fun Sequence<ClassNode>.readKotlinVisibilities(): Map<String, ClassVisibility> =
    mapNotNull { it.toClassVisibility() }
        .associateBy { it.name }
        .apply {
            values.asSequence().filter { it.isCompanion }.forEach {
                val containingClassName = it.name.substringBeforeLast('$')
                getValue(containingClassName).companionVisibilities = it
            }

            values.asSequence().filter { it.facadeClassName != null }.forEach {
                getValue(it.facadeClassName!!).partVisibilities.add(it)
            }
        }
