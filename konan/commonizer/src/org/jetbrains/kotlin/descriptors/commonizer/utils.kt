/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.konan.library.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.konan.library.KonanFactories.createDefaultKonanResolvedModuleDescriptorsFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.konan.impl.ForwardDeclarationsFqNames
import org.jetbrains.kotlin.serialization.konan.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils.getTypeParameterDescriptorOrNull
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

internal fun <T> Sequence<T>.toList(expectedCapacity: Int): List<T> {
    val result = ArrayList<T>(expectedCapacity)
    toCollection(result)
    return result
}

internal inline fun <reified T> Iterable<T?>.firstNonNull() = firstIsInstance<T>()

internal inline val KotlinType.declarationDescriptor: ClassifierDescriptor
    get() = (constructor.declarationDescriptor ?: error("No declaration descriptor found for $constructor"))

internal inline val KotlinType.fqName: FqName
    get() = declarationDescriptor.fqNameSafe

internal val KotlinType.fqNameWithTypeParameters: String
    get() = buildString { buildFqNameWithTypeParameters(this@fqNameWithTypeParameters, HashSet()) }

private fun StringBuilder.buildFqNameWithTypeParameters(type: KotlinType, exploredTypeParameters: MutableSet<KotlinType>) {
    append(type.fqName)

    val typeParameterDescriptor = getTypeParameterDescriptorOrNull(type)
    if (typeParameterDescriptor != null) {
        // N.B this is type parameter type

        if (exploredTypeParameters.add(type.makeNotNullable())) { // print upper bounds once the first time when type parameter type is met
            append(":[")
            typeParameterDescriptor.upperBounds.forEachIndexed { index, upperBound ->
                if (index > 0)
                    append(",")
                buildFqNameWithTypeParameters(upperBound, exploredTypeParameters)
            }
            append("]")
        }
    } else {
        // N.B. this is classifier type

        val arguments = type.arguments
        if (arguments.isNotEmpty()) {
            append("<")
            arguments.forEachIndexed { index, argument ->
                if (index > 0)
                    append(",")

                if (argument.isStarProjection)
                    append("*")
                else {
                    val variance = argument.projectionKind
                    if (variance != Variance.INVARIANT)
                        append(variance).append(" ")
                    buildFqNameWithTypeParameters(argument.type, exploredTypeParameters)
                }
            }
            append(">")
        }
    }

    if (type.isMarkedNullable)
        append("?")
}

internal fun Any?.isNull(): Boolean = this == null

private val STANDARD_KOTLIN_PACKAGE_PREFIXES = listOf(
    KotlinBuiltIns.BUILT_INS_PACKAGE_NAME.asString(),
    "kotlinx"
)

private val KOTLIN_NATIVE_SYNTHETIC_PACKAGES_PREFIXES = ForwardDeclarationsFqNames.syntheticPackages
    .map { fqName ->
        check(!fqName.isRoot)
        fqName.asString()
    }

internal val FqName.isUnderStandardKotlinPackages: Boolean
    get() = hasAnyPrefix(STANDARD_KOTLIN_PACKAGE_PREFIXES)

internal val FqName.isUnderKotlinNativeSyntheticPackages: Boolean
    get() = hasAnyPrefix(KOTLIN_NATIVE_SYNTHETIC_PACKAGES_PREFIXES)

private fun FqName.hasAnyPrefix(prefixes: List<String>): Boolean =
    asString().let { fqName ->
        prefixes.any { prefix ->
            val lengthDifference = fqName.length - prefix.length
            when {
                lengthDifference == 0 -> fqName == prefix
                lengthDifference > 0 -> fqName[prefix.length] == '.' && fqName.startsWith(prefix)
                else -> false
            }
        }
    }

internal val ModuleDescriptor.packageFragmentProvider
    get() = (this as ModuleDescriptorImpl).packageFragmentProviderForModuleContentWithoutDependencies

internal fun createKotlinNativeForwardDeclarationsModule(
    storageManager: StorageManager,
    builtIns: KotlinBuiltIns
) =
    (createDefaultKonanResolvedModuleDescriptorsFactory(DefaultDeserializedDescriptorFactory) as KlibResolvedModuleDescriptorsFactoryImpl)
        .createForwardDeclarationsModule(
            builtIns = builtIns,
            storageManager = storageManager
        )

// similar to org.jetbrains.kotlin.descriptors.DescriptorUtilKt#resolveClassByFqName, but resolves also type aliases
internal fun ModuleDescriptor.resolveClassOrTypeAliasByFqName(
    fqName: FqName,
    lookupLocation: LookupLocation
): ClassifierDescriptorWithTypeParameters? {
    if (fqName.isRoot) return null

    (getPackage(fqName.parent()).memberScope.getContributedClassifier(
        fqName.shortName(),
        lookupLocation
    ) as? ClassifierDescriptorWithTypeParameters)?.let { return it }

    return (resolveClassOrTypeAliasByFqName(fqName.parent(), lookupLocation) as? ClassDescriptor)
        ?.unsubstitutedInnerClassesScope
        ?.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassifierDescriptorWithTypeParameters
}
