/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import gnu.trove.THashMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerParameters
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
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
    val target: CommonizerTarget,
    val builtIns: KotlinBuiltIns,
    val lazyClassifierLookupTable: NotNullLazyValue<LazyClassifierLookupTable>,
    val index: Int,
    private val cache: DeclarationsBuilderCache
) {
    // N.B. this function may create new classifiers for types from Kotlin/Native forward declarations packages
    fun findClassOrTypeAlias(classifierId: ClassId): ClassifierDescriptorWithTypeParameters {
        return if (classifierId.packageFqName.isUnderKotlinNativeSyntheticPackages) {
            // that's a synthetic Kotlin/Native classifier that was exported as forward declaration in one or more modules,
            // but did not match any existing class or typealias
            cache.getOrPutForwardDeclarationsModule(index) {
                // N.B. forward declarations module is created only on demand
                createKotlinNativeForwardDeclarationsModule(
                    storageManager = storageManager,
                    builtIns = builtIns
                )
            }.resolveClassOrTypeAlias(classifierId)
                ?: error("Classifier ${classifierId.asString()} not found for $target")
        } else {
            cache.getCachedClassifier(classifierId, index) // first, look up in created descriptors cache
                ?: lazyClassifierLookupTable().resolveClassOrTypeAlias(classifierId) // then, attempt to load the original classifier
                ?: error("Classifier ${classifierId.asString()} not found for $target")
        }
    }
}

class LazyClassifierLookupTable(lazyModules: Map<String, List<ModuleDescriptor>>) {
    private val table = THashMap<String, List<ModuleDescriptor>>()
    private val allModules: Collection<ModuleDescriptor>

    init {
        // add "module:" prefix for each key representing a module name, not a package name
        lazyModules.forEach { (moduleName, modules) -> table[MODULE_NAME_PREFIX + moduleName.toLowerCase()] = modules }
        allModules = lazyModules.values.flatten()
    }

    fun resolveClassOrTypeAlias(classifierId: ClassId): ClassifierDescriptorWithTypeParameters? {
        if (table.isEmpty) return null

        val packageFqName = classifierId.packageFqName
        if (packageFqName.isRoot) return null

        val packageFqNameRaw = packageFqName.asString()
        table[packageFqNameRaw]?.let { modules ->
            for (module in modules)
                return module.resolveClassOrTypeAlias(classifierId) ?: continue
        }

        val packageFqNameFragments = packageFqNameRaw.split('.')
        val moduleNameForLookup = when (packageFqNameFragments[0]) {
            "kotlin" -> "kotlin"
            "platform" -> if (packageFqNameFragments.size == 2) packageFqNameFragments[1].toLowerCase() else null
            else -> null
        }

        // try to find the classifier by guessing its container module
        if (moduleNameForLookup != null) {
            table[MODULE_NAME_PREFIX + moduleNameForLookup]?.let { modules ->
                for (module in modules) {
                    val classifier = module.resolveClassOrTypeAlias(classifierId) ?: continue
                    table[packageFqNameRaw] = modules // cache to speed-up the further look-ups
                    return classifier
                }
            }
        }

        // last resort: brute force
        for (module in allModules) {
            val classifier = module.resolveClassOrTypeAlias(classifierId) ?: continue
            table[packageFqNameRaw] = listOf(module) // cache to speed-up the further look-ups
            return classifier
        }

        table[packageFqNameRaw] = null // cache to speed-up the further look-ups
        return null
    }

    companion object {
        private const val MODULE_NAME_PREFIX = "module:"
    }
}

fun CirRootNode.createGlobalBuilderComponents(
    storageManager: StorageManager,
    parameters: CommonizerParameters
): GlobalDeclarationsBuilderComponents? {
    if (!parameters.generateDescriptors)
        return null

    val cache = DeclarationsBuilderCache(dimension)

    val lazyCommonDependeeModules = storageManager.createLazyValue {
        parameters.dependeeModulesProvider?.loadModules(emptyList()).orEmpty()
    }

    val targetContexts = (0 until dimension).map { index ->
        val isCommon = index == indexOfCommon

        // do not leak root inside of createLazyValue {} closures!!
        val root = if (isCommon) commonDeclaration()!! else targetDeclarations[index]!!

        val builtIns = root.builtInsProvider.loadBuiltIns()
        check(builtIns::class.java.name == root.builtInsClass) {
            "Unexpected built-ins class: ${builtIns::class.java}, $builtIns\nExpected: ${root.builtInsClass}"
        }

        val lazyModulesLookupTable = storageManager.createLazyValue {
            val result = mutableMapOf<String, MutableList<ModuleDescriptor>>()

            val commonDependeeModules: Map<String, ModuleDescriptor> = lazyCommonDependeeModules()

            if (!isCommon) {
                with(parameters.targetProviders[index]) {
                    val targetDependeeModules: Map<String, ModuleDescriptor> =
                        dependeeModulesProvider?.loadModules(commonDependeeModules.values).orEmpty()

                    val targetModules: Map<String, ModuleDescriptor> =
                        modulesProvider.loadModules(targetDependeeModules.values + commonDependeeModules.values)

                    targetModules.forEach { (moduleName, module) -> result.getOrPut(moduleName) { mutableListOf() } += module }
                    targetDependeeModules.forEach { (moduleName, module) -> result.getOrPut(moduleName) { mutableListOf() } += module }
                }
            }

            commonDependeeModules.forEach { (moduleName, module) -> result.getOrPut(moduleName) { mutableListOf() } += module }

            result.getOrPut(StandardNames.BUILT_INS_PACKAGE_FQ_NAME.asString()) { mutableListOf() } += builtIns.builtInsModule

            LazyClassifierLookupTable(result)
        }

        TargetDeclarationsBuilderComponents(
            storageManager = storageManager,
            target = root.target,
            builtIns = builtIns,
            lazyClassifierLookupTable = lazyModulesLookupTable,
            index = index,
            cache = cache
        )
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
