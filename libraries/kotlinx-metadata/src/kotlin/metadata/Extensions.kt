/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION_ERROR")

package kotlin.metadata

import kotlin.reflect.KClass

/**
 * A type of the extension visitor expected by the code that uses the visitor API.
 *
 * Each declaration which can have platform-specific extensions in the metadata has a method `visitExtensions` in its visitor, e.g.:
 *
 *     open fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor?
 *
 * The client code is supposed to return the extension visitor corresponding to the given type, or to return `null` if the type is
 * of no interest to that code. Each platform-specific extension visitor has a [KmExtensionType] instance declared in the `TYPE` property
 * its companion object. For example, to load JVM extensions on a function, one could do:
 * ```
 *     override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? {
 *         if (type != JvmFunctionExtensionVisitor.TYPE) return null
 *
 *         return object : JvmFunctionExtensionVisitor() {
 *             ...
 *         }
 *     }
 * ```
 * In case an extension visitor of an unrelated type is returned, the code using the visitor API must ignore that visitor.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public class KmExtensionType(private val klass: KClass<out KmExtensionVisitor>) {
    override fun equals(other: Any?): Boolean =
        other is KmExtensionType && klass == other.klass

    override fun hashCode(): Int =
        klass.hashCode()

    override fun toString(): String =
        klass.java.name
}

/**
 * A base interface for all extension visitors.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmExtensionVisitor {
    /**
     * Type of this extension visitor.
     */
    public val type: KmExtensionType
}

/**
 * A visitor to visit platform-specific extensions for a declaration container, such as a class or a package fragment.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmDeclarationContainerExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a class.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmClassExtensionVisitor : KmDeclarationContainerExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a package fragment.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmPackageExtensionVisitor : KmDeclarationContainerExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a function.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmFunctionExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a property.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmPropertyExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a constructor.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmConstructorExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a type parameter.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmTypeParameterExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a type.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmTypeExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a type alias.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmTypeAliasExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a value parameter.
 */
@Deprecated(VISITOR_API_MESSAGE, level = DeprecationLevel.ERROR)
public interface KmValueParameterExtensionVisitor : KmExtensionVisitor
