/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.StatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.Target
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.createKotlinNativeForwardDeclarationsModule
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
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

    private val modules = CommonizedGroup<List<ModuleDescriptorImpl>>(dimension)
    private val packageFragments = CommonizedGroupMap<Pair<Name, FqName>, CommonizedPackageFragmentDescriptor>(dimension)
    private val classes = CommonizedGroupMap<FqName, CommonizedClassDescriptor>(dimension)
    private val typeAliases = CommonizedGroupMap<FqName, CommonizedTypeAliasDescriptor>(dimension)

    private val forwardDeclarationsModules = CommonizedGroup<ModuleDescriptorImpl>(dimension)
    private val allModulesWithDependencies = CommonizedGroup<List<ModuleDescriptor>>(dimension)

    fun getCachedPackageFragments(moduleName: Name, packageFqName: FqName): List<CommonizedPackageFragmentDescriptor?> =
        packageFragments.getOrFail(moduleName to packageFqName)

    fun getCachedClasses(fqName: FqName): List<CommonizedClassDescriptor?> = classes.getOrFail(fqName)

    fun getCachedClassifier(fqName: FqName, index: Int): ClassifierDescriptorWithTypeParameters? =
        classes.getOrNull(fqName)?.get(index) ?: typeAliases.getOrNull(fqName)?.get(index)

    fun cache(index: Int, modules: List<ModuleDescriptorImpl>) {
        this.modules[index] = modules
    }

    fun cache(moduleName: Name, packageFqName: FqName, index: Int, descriptor: CommonizedPackageFragmentDescriptor) {
        packageFragments[moduleName to packageFqName][index] = descriptor
    }

    fun cache(fqName: FqName, index: Int, descriptor: CommonizedClassDescriptor) {
        classes[fqName][index] = descriptor
    }

    fun cache(fqName: FqName, index: Int, descriptor: CommonizedTypeAliasDescriptor) {
        typeAliases[fqName][index] = descriptor
    }

    fun computeIfAbsentForwardDeclarationsModule(index: Int, computable: () -> ModuleDescriptorImpl): ModuleDescriptorImpl {
        forwardDeclarationsModules[index]?.let { return it }

        val module = computable()
        forwardDeclarationsModules[index] = module
        return module
    }

    fun getAllModules(index: Int): List<ModuleDescriptor> {
        allModulesWithDependencies[index]?.let { return it }

        val modulesForTarget = modules[index] ?: error("No module descriptors found for index $index")
        val forwardDeclarationsModule = forwardDeclarationsModules[index]

        // forward declarations module is created on demand (and only when commonizing Kotlin/Native target)
        // so, don't return it if it's not necessary
        val allModules = if (forwardDeclarationsModule != null)
            modulesForTarget + forwardDeclarationsModule
        else
            modulesForTarget

        // set dependencies for target modules and cache them
        modulesForTarget.forEach { it.setDependencies(allModules) }
        allModulesWithDependencies[index] = allModules

        return allModules
    }

    companion object {
        private inline fun <reified K, reified V : DeclarationDescriptor> CommonizedGroupMap<K, V>.getOrFail(key: K): List<V?> =
            getOrNull(key)?.toList() ?: error("No cached ${V::class.java} with key $key found")
    }
}

class GlobalDeclarationsBuilderComponents(
    val storageManager: StorageManager,
    val targetComponents: List<TargetDeclarationsBuilderComponents>,
    val cache: DeclarationsBuilderCache,
    val statsCollector: StatsCollector?
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
    // N.B. this function may create new classifiers for types from Kotlin/Native forward declarations packages
    fun findAppropriateClassOrTypeAlias(fqName: FqName): ClassifierDescriptorWithTypeParameters? {

        return if (fqName.isUnderKotlinNativeSyntheticPackages) {
            // that's a synthetic Kotlin/Native classifier that was exported as forward declaration in one or more modules,
            // but did not match any existing class or typealias
            val module = cache.computeIfAbsentForwardDeclarationsModule(index) {
                // N.B. forward declarations module is created only on demand
                createKotlinNativeForwardDeclarationsModule(
                    storageManager = storageManager,
                    builtIns = builtIns
                )
            }

            // create and return new classifier
            module.packageFragmentProvider
                .getPackageFragments(fqName.parent())
                .single()
                .getMemberScope()
                .getContributedClassifier(
                    name = fqName.shortName(),
                    location = NoLookupLocation.FOR_ALREADY_TRACKED
                ) as ClassifierDescriptorWithTypeParameters
        } else {
            // look up in created descriptors cache
            cache.getCachedClassifier(fqName, index)
        }
    }
}

fun CirRootNode.createGlobalBuilderComponents(
    storageManager: StorageManager,
    statsCollector: StatsCollector?
): GlobalDeclarationsBuilderComponents {
    val cache = DeclarationsBuilderCache(dimension)

    val targetContexts = (0 until dimension).map { index ->
        val isCommon = index == indexOfCommon
        val root = if (isCommon) common()!! else target[index]!!

        val builtIns = root.builtInsProvider.loadBuiltIns()
        check(builtIns::class.java.name == root.builtInsClass) {
            "Unexpected built-ins class: ${builtIns::class.java}, $builtIns\nExpected: ${root.builtInsClass}"
        }

        TargetDeclarationsBuilderComponents(
            storageManager = storageManager,
            target = root.target,
            builtIns = builtIns,
            isCommon = isCommon,
            index = index,
            cache = cache
        )
    }

    return GlobalDeclarationsBuilderComponents(storageManager, targetContexts, cache, statsCollector)
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
