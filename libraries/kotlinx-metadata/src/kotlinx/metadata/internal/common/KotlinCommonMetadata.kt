/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package kotlinx.metadata.internal.common

import kotlinx.metadata.*
import kotlinx.metadata.VISITOR_API_MESSAGE
import kotlinx.metadata.internal.accept
import kotlinx.metadata.internal.extensions.KmModuleFragmentExtension
import kotlinx.metadata.internal.extensions.MetadataExtensions
import kotlinx.metadata.internal.extensions.singleOfType
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.readBuiltinsPackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import java.io.ByteArrayInputStream

/**
 * Reads metadata that is not from annotation nor from `.kotlin_module` file.
 * Usually such metadata comes from serialized klibs. However, special `kotlin_builtins` file from standard library
 * distribution can also be read with this reader.
 */
class KotlinCommonMetadata private constructor(private val proto: ProtoBuf.PackageFragment) {
    fun toKmModuleFragment(): KmModuleFragment =
        KmModuleFragment().apply(this::accept)

    class Writer : KmModuleFragmentVisitor() {
        // TODO
    }

    fun accept(v: KmModuleFragmentVisitor) {
        val strings = NameResolverImpl(proto.strings, proto.qualifiedNames)
        if (proto.hasPackage()) {
            v.visitPackage()?.let { proto.`package`.accept(it, strings) }
        }
        for (klass in proto.class_List) {
            v.visitClass()?.let { klass.accept(it, strings) }
        }
        v.visitEnd()
    }

    companion object {
        @JvmStatic
        fun read(bytes: ByteArray): KotlinCommonMetadata? {
            val (proto, _) = ByteArrayInputStream(bytes).readBuiltinsPackageFragment()
            if (proto == null) return null

            return KotlinCommonMetadata(proto)
        }
    }
}

/**
 * Represents a Kotlin module fragment.
 *
 * Do not confuse with `KmModule`: while KmModule represents JVM-specific `.kotlin_module` file, KmModuleFragment is not platform-specific.
 * It usually represents metadata serialized to klib or part of klib,
 * but also may represent a special `.kotlin_builtins` file that can be encountered only in standard library.
 *
 * Can be read with [KotlinCommonMetadata.read].
 */
@Suppress("DEPRECATION")
class KmModuleFragment : KmModuleFragmentVisitor() {

    /**
     * Top-level functions, type aliases and properties in the module fragment.
     */
    var pkg: KmPackage? = null

    /**
     * Classes in the module fragment.
     */
    val classes: MutableList<KmClass> = ArrayList()

    private val extensions: List<KmModuleFragmentExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createModuleFragmentExtensions)

    @Deprecated(VISITOR_API_MESSAGE)
    override fun visitPackage(): KmPackageVisitor? =
        KmPackage().also { pkg = it }

    @Deprecated(VISITOR_API_MESSAGE)
    override fun visitExtensions(type: KmExtensionType): KmModuleFragmentExtensionVisitor? =
        extensions.singleOfType(type)

    @Deprecated(VISITOR_API_MESSAGE)
    override fun visitClass(): KmClassVisitor? =
        KmClass().addTo(classes)

    /**
     * Populates the given visitor with data in this module fragment.
     *
     * @param visitor the visitor which will visit data in the module fragment.
     */
    @Deprecated(VISITOR_API_MESSAGE)
    fun accept(visitor: KmModuleFragmentVisitor) {
        pkg?.let { visitor.visitPackage()?.let(it::accept) }
        classes.forEach { visitor.visitClass()?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * A visitor to visit module fragments. The module fragment can have no more than one package, and any number of classes,
 * and must have at least one declaration.
 *
 * When using this class, [visitEnd] must be called exactly once and after calls to all other visit* methods.
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmModuleFragmentVisitor @JvmOverloads constructor(private val delegate: KmModuleFragmentVisitor? = null) {

    /**
     * Visits a package within the module fragment.
     */
    open fun visitPackage(): KmPackageVisitor? =
        delegate?.visitPackage()

    /**
     * Visits a class within the module fragment.
     */
    open fun visitClass(): KmClassVisitor? =
        delegate?.visitClass()

    /**
     * Visits the extensions of the given type on the module fragment.
     *
     * @param type the type of extension visitor to be returned.
     */
    open fun visitExtensions(type: KmExtensionType): KmModuleFragmentExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the module fragment.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit platform-specific extensions for a module fragment.
 */
@Deprecated(VISITOR_API_MESSAGE)
interface KmModuleFragmentExtensionVisitor : KmExtensionVisitor
