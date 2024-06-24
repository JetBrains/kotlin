/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.metadata.internal.extensions

import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.reflect.KClass

/**
 * A type of the extension expected by the code that uses the extensions API.
 *
 * Each declaration that can have platform-specific extensions in the metadata has a method `getExtension`, e.g.:
 * `fun KmFunction.getExtension(type: KmExtensionType): KmFunctionExtension`.
 *
 * These functions are used by -jvm and -klib counterparts to retrieve platform-specific metadata
 * and should not be used in any other way.
 */
public class KmExtensionType(private val klass: KClass<out KmExtension>) {
    override fun equals(other: Any?): Boolean =
        other is KmExtensionType && klass == other.klass

    override fun hashCode(): Int =
        klass.hashCode()

    override fun toString(): String =
        klass.java.name
}

/**
 * Base interface for all extensions to hold the extension type.
 */
public interface KmExtension {

    /**
     * Type of this extension.
     */
    public val type: KmExtensionType
}

public interface KmClassExtension : KmExtension

public fun KmClass.getExtension(type: KmExtensionType): KmClassExtension = extensions.singleOfType(type)

public interface KmPackageExtension : KmExtension

public fun KmPackage.getExtension(type: KmExtensionType): KmPackageExtension = extensions.singleOfType(type)

public interface KmModuleFragmentExtension : KmExtension

public fun KmModuleFragment.getExtension(type: KmExtensionType): KmModuleFragmentExtension = extensions.singleOfType(type)

public interface KmFunctionExtension : KmExtension

public fun KmFunction.getExtension(type: KmExtensionType): KmFunctionExtension = extensions.singleOfType(type)

public interface KmPropertyExtension : KmExtension

public fun KmProperty.getExtension(type: KmExtensionType): KmPropertyExtension = extensions.singleOfType(type)

public interface KmConstructorExtension : KmExtension

public fun KmConstructor.getExtension(type: KmExtensionType): KmConstructorExtension = extensions.singleOfType(type)

public interface KmTypeParameterExtension : KmExtension

public fun KmTypeParameter.getExtension(type: KmExtensionType): KmTypeParameterExtension = extensions.singleOfType(type)

public interface KmTypeExtension : KmExtension {
    /**
     * Has to be implemented for [KmType.equals] to work.
     * Remember to implement `hashCode`, too (although it is not used in [KmType.hashCode]).
     */
    override fun equals(other: Any?): Boolean
}

public fun KmType.getExtension(type: KmExtensionType): KmTypeExtension = extensions.singleOfType(type)

public interface KmTypeAliasExtension : KmExtension

public fun KmTypeAlias.getExtension(type: KmExtensionType): KmTypeAliasExtension = extensions.singleOfType(type)

public interface KmValueParameterExtension : KmExtension

public fun KmValueParameter.getExtension(type: KmExtensionType): KmValueParameterExtension = extensions.singleOfType(type)

private fun <N : KmExtension> Collection<N>.singleOfType(type: KmExtensionType): N {
    var result: N? = null
    for (node in this) {
        if (node.type != type) continue
        if (result != null) {
            throw IllegalStateException("Multiple extensions handle the same extension type: $type")
        }
        result = node
    }
    if (result == null) {
        throw IllegalStateException("No extensions handle the extension type: $type")
    }
    return result
}
