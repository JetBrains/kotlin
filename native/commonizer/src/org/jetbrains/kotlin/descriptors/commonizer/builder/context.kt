/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import gnu.trove.THashMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.Parameters
import org.jetbrains.kotlin.descriptors.commonizer.Target
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.createKotlinNativeForwardDeclarationsModule
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager

/**
 * Temporary caches for constructed descriptors.
 */
class DeclarationsBuilderCache(private val dimension: Int) {
    init {
        check(dimension > 0)
    }

    private val modules = CommonizedGroup<List<ModuleDescriptorImpl>>(dimension)
    private val packageFragments = CommonizedGroupMap<Pair<Name, FqName>, CommonizedPackageFragmentDescriptor>(dimension)
    private val classes = CommonizedGroupMap<ClassId, CommonizedClassDescriptor>(dimension)
    private val typeAliases = CommonizedGroupMap<ClassId, CommonizedTypeAliasDescriptor>(dimension)

    private val forwardDeclarationsModules = CommonizedGroup<ModuleDescriptorImpl>(dimension)
    private val allModulesWithDependencies = CommonizedGroup<List<ModuleDescriptor>>(dimension)

    fun getCachedPackageFragments(moduleName: Name, packageFqName: FqName): List<CommonizedPackageFragmentDescriptor?> =
        packageFragments.getOrFail(moduleName to packageFqName)

    fun getCachedClasses(classId: ClassId): List<CommonizedClassDescriptor?> = classes.getOrFail(classId)

    fun getCachedClassifier(classId: ClassId, index: Int): ClassifierDescriptorWithTypeParameters? {
        // first, look up for class
        val classes: CommonizedGroup<CommonizedClassDescriptor>? = classes.getOrNull(classId)
        classes?.get(index)?.let { return it }

        // then, for type alias
        val typeAliases: CommonizedGroup<CommonizedTypeAliasDescriptor>? = typeAliases.getOrNull(classId)
        typeAliases?.get(index)?.let { return it }

        val indexOfCommon = dimension - 1
        if (indexOfCommon != index) {
            // then, for class from the common fragment
            classes?.get(indexOfCommon)?.let { return it }

            // then, for type alias from the common fragment
            typeAliases?.get(indexOfCommon)?.let { return it }
        }

        return null
    }

    fun cache(index: Int, modules: List<ModuleDescriptorImpl>) {
        this.modules[index] = modules
    }

    fun cache(moduleName: Name, packageFqName: FqName, index: Int, descriptor: CommonizedPackageFragmentDescriptor) {
        packageFragments[moduleName to packageFqName][index] = descriptor
    }

    fun cache(classId: ClassId, index: Int, descriptor: CommonizedClassDescriptor) {
        classes[classId][index] = descriptor
    }

    fun cache(classId: ClassId, index: Int, descriptor: CommonizedTypeAliasDescriptor) {
        typeAliases[classId][index] = descriptor
    }

    fun getOrPutForwardDeclarationsModule(index: Int, computable: () -> ModuleDescriptorImpl): ModuleDescriptorImpl {
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
            getOrNull(key) ?: error("No cached ${V::class.java} with key $key found")
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
    val lazyModulesLookupTable: NotNullLazyValue<MutableMap<String, ModuleDescriptor?>>,
    val isCommon: Boolean,
    val index: Int,
    private val cache: DeclarationsBuilderCache
) {
    // only for test purposes
    internal var extendedLookupForBuiltInsClassifiers: Boolean = false

    // N.B. this function may create new classifiers for types from Kotlin/Native forward declarations packages
    fun findClassOrTypeAlias(classId: ClassId): ClassifierDescriptorWithTypeParameters {
        return when {
            classId.packageFqName.isUnderStandardKotlinPackages -> {
                // look up for classifier in built-ins module:
                val builtInsModule = builtIns.builtInsModule

                // TODO: this works fine for Native as far as built-ins module contains full Native stdlib, but this is not enough for JVM and JS
                val classifier = builtInsModule.resolveClassOrTypeAlias(classId)
                if (classifier != null)
                    return classifier

                if (extendedLookupForBuiltInsClassifiers) {
                    return findOriginalClassOrTypeAlias(classId)
                        ?: error("Classifier ${classId.asString()} not found neither in built-ins module $builtInsModule nor in original modules for $target")
                }

                error("Classifier ${classId.asString()} not found in built-ins module $builtInsModule for $target")
            }
            classId.packageFqName.isUnderKotlinNativeSyntheticPackages -> {
                // that's a synthetic Kotlin/Native classifier that was exported as forward declaration in one or more modules,
                // but did not match any existing class or typealias
                cache.getOrPutForwardDeclarationsModule(index) {
                    // N.B. forward declarations module is created only on demand
                    createKotlinNativeForwardDeclarationsModule(
                        storageManager = storageManager,
                        builtIns = builtIns
                    )
                }.resolveClassOrTypeAlias(classId)
                    ?: error("Classifier ${classId.asString()} not found for $target")
            }
            else -> {
                cache.getCachedClassifier(classId, index) // first, look up in created descriptors cache
                    ?: findOriginalClassOrTypeAlias(classId) // then, attempt to load the original classifier
                    ?: error("Classifier ${classId.asString()} not found for $target")
            }
        }
    }

    private fun findOriginalClassOrTypeAlias(classId: ClassId): ClassifierDescriptorWithTypeParameters? {
        if (classId.packageFqName.isRoot)
            return null

        // first, guess containing module and look up in it
        val classifier = lazyModulesLookupTable()
            .guessModuleByPackageFqName(classId.packageFqName)
            ?.resolveClassOrTypeAlias(classId)

        // if failed, then look up though all modules
        return classifier
            ?: lazyModulesLookupTable().values
                .asSequence()
                .mapNotNull { it?.resolveClassOrTypeAlias(classId) }
                .firstOrNull()
    }
}

fun CirRootNode.createGlobalBuilderComponents(
    storageManager: StorageManager,
    parameters: Parameters
): GlobalDeclarationsBuilderComponents {
    val cache = DeclarationsBuilderCache(dimension)

    val targetContexts = (0 until dimension).map { index ->
        val isCommon = index == indexOfCommon

        // do not leak root inside of createLazyValue {} closures!!
        val root = if (isCommon) commonDeclaration()!! else targetDeclarations[index]!!

        val builtIns = root.builtInsProvider.loadBuiltIns()
        check(builtIns::class.java.name == root.builtInsClass) {
            "Unexpected built-ins class: ${builtIns::class.java}, $builtIns\nExpected: ${root.builtInsClass}"
        }

        val lazyModulesLookupTable = storageManager.createLazyValue {
            val source = if (isCommon) emptyMap() else parameters.targetProviders[index].modulesProvider.loadModules()
            THashMap(source)
        }

        TargetDeclarationsBuilderComponents(
            storageManager = storageManager,
            target = root.target,
            builtIns = builtIns,
            lazyModulesLookupTable = lazyModulesLookupTable,
            isCommon = isCommon,
            index = index,
            cache = cache
        ).also {
            it.extendedLookupForBuiltInsClassifiers = parameters.extendedLookupForBuiltInsClassifiers
        }
    }

    return GlobalDeclarationsBuilderComponents(storageManager, targetContexts, cache, parameters.statsCollector)
}

interface TypeParameterResolver {
    val parametersCount: Int
    fun resolve(index: Int): TypeParameterDescriptor?

    companion object {
        val EMPTY = object : TypeParameterResolver {
            override val parametersCount get() = 0
            override fun resolve(index: Int): TypeParameterDescriptor? = null
        }
    }
}

class TypeParameterResolverImpl(
    private val ownTypeParameters: List<TypeParameterDescriptor>,
    private val parent: TypeParameterResolver = TypeParameterResolver.EMPTY
) : TypeParameterResolver {
    override val parametersCount: Int
        get() = ownTypeParameters.size + parent.parametersCount

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    override fun resolve(index: Int): TypeParameterDescriptor? {
        val parentParametersCount = parent.parametersCount
        if (index >= 0 && index < parentParametersCount)
            return parent.resolve(index)

        val localIndex = index - parentParametersCount
        if (localIndex < ownTypeParameters.size)
            return ownTypeParameters[localIndex]

        error("Illegal type parameter index: $index. Should be between 0 and ${parametersCount - 1}")
    }
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
