/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.Target
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirRootNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager

/**
 * Temporary caches for constructed descriptors.
 */
class DeclarationsBuilderCache(dimension: Int) {
    init {
        check(dimension > 0)
    }

    private val packageFragments = CommonizedGroupMap<Pair<Name, FqName>, CommonizedPackageFragmentDescriptor>(dimension)
    private val classes = CommonizedGroupMap<FqName, CommonizedClassDescriptor>(dimension)
    private val typeAliases = CommonizedGroupMap<FqName, CommonizedTypeAliasDescriptor>(dimension)
    private val modules = CommonizedGroup<Collection<ModuleDescriptor>>(dimension)

    fun getCachedPackageFragments(moduleName: Name, packageFqName: FqName): List<CommonizedPackageFragmentDescriptor?> =
        packageFragments.getOrFail(moduleName to packageFqName)

    fun getCachedClasses(fqName: FqName): List<CommonizedClassDescriptor?> = classes.getOrFail(fqName)

    private inline fun <reified K, reified V : DeclarationDescriptor> CommonizedGroupMap<K, V>.getOrFail(key: K): List<V?> =
        getOrNull(key)?.toList() ?: error("No cached ${V::class.java} with key $key found")

    fun cache(moduleName: Name, packageFqName: FqName, index: Int, descriptor: CommonizedPackageFragmentDescriptor) {
        packageFragments[moduleName to packageFqName][index] = descriptor
    }

    fun cache(fqName: FqName, index: Int, descriptor: CommonizedClassDescriptor) {
        classes[fqName][index] = descriptor
    }

    fun cache(fqName: FqName, index: Int, descriptor: CommonizedTypeAliasDescriptor) {
        typeAliases[fqName][index] = descriptor
    }

    fun cache(index: Int, modules: Collection<ModuleDescriptor>) {
        this.modules[index] = modules
    }

    fun getCachedClassifier(fqName: FqName, index: Int): ClassifierDescriptorWithTypeParameters? =
        classes.getOrNull(fqName)?.get(index) ?: typeAliases.getOrNull(fqName)?.get(index)

    fun getCachedModules(index: Int): Collection<ModuleDescriptor> = modules[index] ?: error("No module descriptors found for index $index")
}

class GlobalDeclarationsBuilderComponents(
    val storageManager: StorageManager,
    val targetComponents: List<TargetDeclarationsBuilderComponents>,
    val cache: DeclarationsBuilderCache
) {
    init {
        check(targetComponents.size > 1)
        targetComponents.forEachIndexed { index, targetComponent -> check(index == targetComponent.index) }
    }
}

class TargetDeclarationsBuilderComponents(
    val storageManager: StorageManager,
    val target: Target,
    val builtIns: KotlinBuiltIns,
    val isCommon: Boolean,
    val index: Int,
    private val cache: DeclarationsBuilderCache
) {
    // only for classes and type aliases
    fun getCachedClassifier(fqName: FqName): ClassifierDescriptorWithTypeParameters? = cache.getCachedClassifier(fqName, index)
}

fun CirRootNode.createGlobalBuilderComponents(storageManager: StorageManager): GlobalDeclarationsBuilderComponents {
    val cache = DeclarationsBuilderCache(dimension)

    val targetContexts = (0 until dimension).map { index ->
        val isCommon = index == indexOfCommon
        val target = (if (isCommon) common()!! else target[index]).target

        val builtIns = modules.asSequence()
            .mapNotNull { if (isCommon) it.common() else it.target[index] }
            .first()
            .builtIns

        TargetDeclarationsBuilderComponents(
            storageManager = storageManager,
            target = target,
            builtIns = builtIns,
            isCommon = isCommon,
            index = index,
            cache = cache
        )
    }

    return GlobalDeclarationsBuilderComponents(storageManager, targetContexts, cache)
}

interface TypeParameterResolver {
    fun resolve(name: Name): TypeParameterDescriptor?

    companion object {
        val EMPTY = object : TypeParameterResolver {
            override fun resolve(name: Name): TypeParameterDescriptor? = null
        }
    }
}

class TypeParameterResolverImpl(
    storageManager: StorageManager,
    ownTypeParameters: List<TypeParameterDescriptor>,
    private val parent: TypeParameterResolver = TypeParameterResolver.EMPTY
) : TypeParameterResolver {

    private val ownTypeParameters = storageManager.createLazyValue {
        // memoize the first occurrence of descriptor with the same Name
        ownTypeParameters.groupingBy { it.name }.reduce { _, accumulator, _ -> accumulator }
    }

    override fun resolve(name: Name) = ownTypeParameters()[name] ?: parent.resolve(name)
}

fun DeclarationDescriptor.getTypeParameterResolver(): TypeParameterResolver =
    when (this) {
        is CommonizedClassDescriptor -> typeParameterResolver
        is ClassDescriptor -> {
            // all class descriptors must be instances of CommonizedClassDescriptor, and therefore must implement ContainerWithTypeParameterResolver
            error("Class descriptor that is not instance of ${CommonizedClassDescriptor::class.java}: ${this::class.java}, $this")
        }
        else -> TypeParameterResolver.EMPTY
    }
