/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.analyzer.common.CommonAnalysisParameters
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.*
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.idea.configuration.IdeBuiltInsLoadingState
import org.jetbrains.kotlin.idea.project.IdeaEnvironment
import org.jetbrains.kotlin.idea.compiler.IdeSealedClassInheritorsProvider
import org.jetbrains.kotlin.idea.project.findAnalyzerServices
import org.jetbrains.kotlin.idea.project.useCompositeAnalysis
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.platform.idePlatformKind
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.RESOLUTION_ANCHOR_PROVIDER_CAPABILITY
import org.jetbrains.kotlin.resolve.ResolutionAnchorProvider
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters

class IdeaResolverForProject(
    debugName: String,
    projectContext: ProjectContext,
    modules: Collection<IdeaModuleInfo>,
    private val syntheticFilesByModule: Map<IdeaModuleInfo, Collection<KtFile>>,
    delegateResolver: ResolverForProject<IdeaModuleInfo>,
    fallbackModificationTracker: ModificationTracker? = null,
    private val settings: PlatformAnalysisSettings
) : AbstractResolverForProject<IdeaModuleInfo>(
    debugName,
    projectContext,
    modules,
    fallbackModificationTracker,
    delegateResolver,
    ServiceManager.getService(projectContext.project, IdePackageOracleFactory::class.java)
) {

    companion object {
        val PLATFORM_ANALYSIS_SETTINGS = ModuleCapability<PlatformAnalysisSettings>("PlatformAnalysisSettings")
    }

    private val resolutionAnchorProvider = ServiceManager.getService(projectContext.project, ResolutionAnchorProvider::class.java)

    private val constantSdkDependencyIfAny: SdkInfo? =
        if (settings is PlatformAnalysisSettingsImpl) settings.sdk?.let { SdkInfo(projectContext.project, it) } else null

    private val builtInsCache: BuiltInsCache =
        (delegateResolver as? IdeaResolverForProject)?.builtInsCache ?: BuiltInsCache(projectContext, this)

    override fun getAdditionalCapabilities(): Map<ModuleCapability<*>, Any?> {
        return super.getAdditionalCapabilities() +
                (PLATFORM_ANALYSIS_SETTINGS to settings) +
                (RESOLUTION_ANCHOR_PROVIDER_CAPABILITY to resolutionAnchorProvider)
    }

    override fun sdkDependency(module: IdeaModuleInfo): SdkInfo? {
        if (projectContext.project.useCompositeAnalysis) {
            require(constantSdkDependencyIfAny == null) { "Shouldn't pass SDK dependency manually for composite analysis mode" }
        }
        return constantSdkDependencyIfAny ?: module.findSdkAcrossDependencies()
    }

    override fun modulesContent(module: IdeaModuleInfo): ModuleContent<IdeaModuleInfo> =
        ModuleContent(module, syntheticFilesByModule[module] ?: emptyList(), module.contentScope())

    override fun builtInsForModule(module: IdeaModuleInfo): KotlinBuiltIns = builtInsCache.getOrCreateIfNeeded(module)

    override fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: IdeaModuleInfo): ResolverForModule {
        val moduleContent = ModuleContent(moduleInfo, syntheticFilesByModule[moduleInfo] ?: listOf(), moduleInfo.contentScope())

        val languageVersionSettings =
            IDELanguageSettingsProvider.getLanguageVersionSettings(moduleInfo, projectContext.project)

        val resolverForModuleFactory = getResolverForModuleFactory(moduleInfo)

        return resolverForModuleFactory.createResolverForModule(
            descriptor as ModuleDescriptorImpl,
            projectContext.withModule(descriptor),
            moduleContent,
            this,
            languageVersionSettings,
            IdeSealedClassInheritorsProvider,
        )
    }

    private fun getResolverForModuleFactory(moduleInfo: IdeaModuleInfo): ResolverForModuleFactory {
        val platform = moduleInfo.platform

        val jvmPlatformParameters = JvmPlatformParameters(
            packagePartProviderFactory = { IDEPackagePartProvider(it.moduleContentScope) },
            moduleByJavaClass = { javaClass: JavaClass ->
                val psiClass = (javaClass as JavaClassImpl).psi
                psiClass.getPlatformModuleInfo(JvmPlatforms.unspecifiedJvmPlatform)?.platformModule ?: psiClass.getNullableModuleInfo()
            },
            resolverForReferencedModule = { targetModuleInfo, referencingModuleInfo ->
                require(targetModuleInfo is IdeaModuleInfo && referencingModuleInfo is IdeaModuleInfo) {
                    "Unexpected modules passed through JvmPlatformParameters to IDE resolver ($targetModuleInfo, $referencingModuleInfo)"
                }
                tryGetResolverForModuleWithResolutionAnchorFallback(targetModuleInfo, referencingModuleInfo)
            },
            useBuiltinsProviderForModule = {
                IdeBuiltInsLoadingState.isFromDependenciesForJvm && it is LibraryInfo && it.isKotlinStdlib(projectContext.project)
            }
        )

        val commonPlatformParameters = CommonAnalysisParameters(
            metadataPartProviderFactory = { IDEPackagePartProvider(it.moduleContentScope) }
        )

        return if (!projectContext.project.useCompositeAnalysis) {
            val parameters = when {
                platform.isJvm() -> jvmPlatformParameters
                platform.isCommon() -> commonPlatformParameters
                else -> PlatformAnalysisParameters.Empty
            }

            platform.idePlatformKind.resolution.createResolverForModuleFactory(parameters, IdeaEnvironment, platform)
        } else {
            CompositeResolverForModuleFactory(
                commonPlatformParameters,
                jvmPlatformParameters,
                platform,
                CompositeAnalyzerServices(platform.componentPlatforms.map { it.findAnalyzerServices() })
            )
        }
    }

    // Important: ProjectContext must be from SDK to be sure that we won't run into deadlocks
    class BuiltInsCache(private val projectContextFromSdkResolver: ProjectContext, private val resolverForSdk: IdeaResolverForProject) {
        private val cache = mutableMapOf<BuiltInsCacheKey, KotlinBuiltIns>()

        fun getOrCreateIfNeeded(module: IdeaModuleInfo): KotlinBuiltIns = projectContextFromSdkResolver.storageManager.compute {
            val sdk = resolverForSdk.sdkDependency(module)
            val stdlib = findStdlibForModulesBuiltins(module)

            val key = module.platform.idePlatformKind.resolution.getKeyForBuiltIns(module, sdk, stdlib)
            val cachedBuiltIns = cache[key]
            if (cachedBuiltIns != null) return@compute cachedBuiltIns

            module.platform.idePlatformKind.resolution
                .createBuiltIns(module, projectContextFromSdkResolver, resolverForSdk, sdk, stdlib)
                .also {
                    // TODO: MemoizedFunction should be used here instead, but for proper we also need a module (for LV settings) that is not contained in the key
                    cache[key] = it
                }
        }

        private fun findStdlibForModulesBuiltins(module: IdeaModuleInfo): LibraryInfo? {
            if (IdeBuiltInsLoadingState.isFromClassLoader)
                return null

            return module.dependencies().lazyClosure { it.dependencies() }.firstOrNull {
                it is LibraryInfo && it.isKotlinStdlib(projectContextFromSdkResolver.project)
            } as? LibraryInfo
        }
    }

    private fun tryGetResolverForModuleWithResolutionAnchorFallback(
        targetModuleInfo: IdeaModuleInfo,
        referencingModuleInfo: IdeaModuleInfo,
    ): ResolverForModule? {
        tryGetResolverForModule(targetModuleInfo)?.let { return it }

        return getResolverForProjectUsingResolutionAnchor(targetModuleInfo, referencingModuleInfo)
    }

    private fun getResolverForProjectUsingResolutionAnchor(
        targetModuleInfo: IdeaModuleInfo,
        referencingModuleInfo: IdeaModuleInfo
    ): ResolverForModule? {
        val moduleDescriptorOfReferencingModule = descriptorByModule[referencingModuleInfo]?.moduleDescriptor
            ?: error("$referencingModuleInfo is not contained in this resolver, which means incorrect use of anchor-aware search")

        val anchorModuleInfo = resolutionAnchorProvider.getResolutionAnchor(moduleDescriptorOfReferencingModule)?.moduleInfo ?: return null

        val resolverForProjectFromAnchorModule = KotlinCacheService.getInstance(projectContext.project)
            .getResolutionFacadeByModuleInfo(anchorModuleInfo, anchorModuleInfo.platform)
            ?.getResolverForProject()
            ?: return null

        require(resolverForProjectFromAnchorModule is IdeaResolverForProject) {
            "Resolution via anchor modules is expected to be used only from IDE resolvers"
        }

        return resolverForProjectFromAnchorModule.tryGetResolverForModule(targetModuleInfo)
    }
}

interface BuiltInsCacheKey {
    object DefaultBuiltInsKey : BuiltInsCacheKey
}
