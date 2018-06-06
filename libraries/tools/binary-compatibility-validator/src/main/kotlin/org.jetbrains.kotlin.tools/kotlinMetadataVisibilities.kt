/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools

import kotlinx.metadata.*
import kotlinx.metadata.jvm.*
import org.objectweb.asm.tree.ClassNode

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


private val VISIBILITY_FLAGS_MAP = mapOf(
    Flag.IS_INTERNAL to "internal",
    Flag.IS_PRIVATE to "private",
    Flag.IS_PRIVATE_TO_THIS to "private",
    Flag.IS_PROTECTED to "protected",
    Flag.IS_PUBLIC to "public",
    Flag.IS_LOCAL to "local"
)

private fun Flags.toVisibility() = VISIBILITY_FLAGS_MAP.entries.firstOrNull { (modifier) -> modifier(this) }?.value

private fun visitFunction(flags: Flags, name: String, addMember: (MemberVisibility) -> Unit) =
    object : KmFunctionVisitor() {
        var jvmDesc: JvmMemberSignature? = null
        override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? {
            if (type != JvmFunctionExtensionVisitor.TYPE) return null
            return object : JvmFunctionExtensionVisitor() {
                override fun visit(desc: JvmMethodSignature?) {
                    jvmDesc = desc
                }
            }
        }

        override fun visitEnd() {
            jvmDesc?.let { jvmDesc ->
                addMember(MemberVisibility(jvmDesc, flags))
            }
        }
    }

private fun visitConstructor(flags: Flags, addMember: (MemberVisibility) -> Unit) =
    object : KmConstructorVisitor() {
        var jvmDesc: JvmMemberSignature? = null
        override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor? {
            if (type != JvmConstructorExtensionVisitor.TYPE) return null
            return object : JvmConstructorExtensionVisitor() {
                override fun visit(desc: JvmMethodSignature?) {
                    jvmDesc = desc
                }
            }
        }

        override fun visitEnd() {
            jvmDesc?.let { signature ->
                addMember(MemberVisibility(signature, flags))
            }
        }
    }

private fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags, addMember: (MemberVisibility) -> Unit) =
    object : KmPropertyVisitor() {
        var _fieldDesc: JvmMemberSignature? = null
        var _getterDesc: JvmMemberSignature? = null
        var _setterDesc: JvmMemberSignature? = null

        override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? {
            if (type != JvmPropertyExtensionVisitor.TYPE) return null
            return object : JvmPropertyExtensionVisitor() {
                override fun visit(fieldDesc: JvmFieldSignature?, getterDesc: JvmMethodSignature?, setterDesc: JvmMethodSignature?) {
                    _fieldDesc = fieldDesc
                    _getterDesc = getterDesc
                    _setterDesc = setterDesc
                }
            }
        }

        override fun visitEnd() {
            _getterDesc?.let { addMember(MemberVisibility(it, getterFlags)) }
            _setterDesc?.let { addMember(MemberVisibility(it, setterFlags)) }
            _fieldDesc?.let {
                val fieldVisibility = when {
                    Flag.Property.IS_LATEINIT(flags) -> setterFlags
                    _getterDesc == null && _setterDesc == null -> flags // JvmField or const case
                    else -> flagsOf(Flag.IS_PRIVATE)
                }
                addMember(MemberVisibility(it, fieldVisibility))
            }
        }
    }

private fun visitPackage(addMember: (MemberVisibility) -> Unit) =
    object : KmPackageVisitor() {
        override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
            return visitFunction(flags, name, addMember)
        }

        override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? {
            return visitProperty(flags, name, getterFlags, setterFlags, addMember)
        }

        override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? {
            return super.visitTypeAlias(flags, name)
        }
    }

private fun visitClass(flags: (Flags) -> Unit, addMember: (MemberVisibility) -> Unit) =
    object : KmClassVisitor() {
        override fun visit(flags: Flags, name: ClassName) {
            flags(flags)
        }

        override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
            return visitFunction(flags, name, addMember)
        }

        override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? {
            return visitProperty(flags, name, getterFlags, setterFlags, addMember)
        }

        override fun visitConstructor(flags: Flags): KmConstructorVisitor? {
            return visitConstructor(flags, addMember)
        }

        override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? {
            return super.visitTypeAlias(flags, name)
        }
    }

fun KotlinClassMetadata.toClassVisibility(classNode: ClassNode): ClassVisibility? {
    var flags: Flags? = null
    var _facadeClassName: String? = null
    val members = mutableListOf<MemberVisibility>()
    val addMember: (MemberVisibility) -> Unit = { members.add(it) }

    when (this) {
        is KotlinClassMetadata.Class ->
            this.accept(visitClass({ flags = it }, addMember))
        is KotlinClassMetadata.FileFacade ->
            this.accept(visitPackage(addMember))
        is KotlinClassMetadata.MultiFileClassPart -> {
            _facadeClassName = this.facadeClassName
            this.accept(visitPackage(addMember))
        }
        else -> {}
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