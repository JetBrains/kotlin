/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

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
 *
 *     override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? {
 *         if (type != JvmFunctionExtensionVisitor.TYPE) return null
 *
 *         return object : JvmFunctionExtensionVisitor() {
 *             ...
 *         }
 *     }
 *
 * In case an extension visitor of an unrelated type is returned, the code using the visitor API must ignore that visitor.
 */
data class KmExtensionType(val klass: KClass<out KmExtensionVisitor>)

/**
 * A base interface for all extension visitors.
 */
interface KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a declaration container, such as a class or a package fragment.
 */
interface KmDeclarationContainerExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a class.
 */
interface KmClassExtensionVisitor : KmDeclarationContainerExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a package fragment.
 */
interface KmPackageExtensionVisitor : KmDeclarationContainerExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a function.
 */
interface KmFunctionExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a property.
 */
interface KmPropertyExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a constructor.
 */
interface KmConstructorExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a type parameter.
 */
interface KmTypeParameterExtensionVisitor : KmExtensionVisitor

/**
 * A visitor to visit platform-specific extensions for a type.
 */
interface KmTypeExtensionVisitor : KmExtensionVisitor
