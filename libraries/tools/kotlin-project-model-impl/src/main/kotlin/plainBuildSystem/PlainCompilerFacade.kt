/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.plainBuildSystem

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.project.modelx.*
import org.jetbrains.kotlin.project.modelx.compiler.KpmCompiler
import org.jetbrains.kotlin.project.modelx.compiler.Compilers
import org.jetbrains.kotlin.project.modelx.compiler.KPMCompilerArgumentsMapper
import org.jetbrains.kotlin.project.modelx.serialization.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

class PlainCompilerFacade(
    private val serializer: KpmSerializer,
    private val kpmDtoTransformer: KotlinModuleDtoTransformer,
    private val compilers: Compilers,
    private val kpmFileStructure: KpmFileStructure,
    private val argumentsMapper: KPMCompilerArgumentsMapper.PreconfiguredFactory
) {
    fun exec(request: CompilationRequest): ExitCode {
        val moduleDto = buildKotlinModule(request)
        val module = kpmDtoTransformer.fromDto(moduleDto)

        val buildSystemAdapter = PlainBuildSystemAdapter(
            module = module,
            moduleDependencies = buildModuleDependencies(request),
            kpmFileStructure = kpmFileStructure,
        )

        val dependencyExpansion = KpmDependencyExpansion(module, buildSystemAdapter.variantMatcher)
        val kpmCompiler = KpmCompiler(
            compilers = compilers,
            compilationProcessor = KpmCompilationProcessor(module, dependencyExpansion),
            argumentsMapper = argumentsMapper.create(buildSystemAdapter)
        )

        val exitCode = kpmCompiler.compileAll(module, request.fragmentMap)
        writeKpmMetadata(module)
        // writePsm(module)
        packageArtifacts(module)

        return exitCode
    }

    private fun writeKpmMetadata(module: KotlinModule) {
        val moduleDto = kpmDtoTransformer.toDto(module)
        val metadataBytes = serializer.serialize(moduleDto)
        kpmFileStructure.kpmFilePath.parent.createDirectories()
        kpmFileStructure.kpmFilePath.writeBytes(metadataBytes)

        moduleDto
            .fragments
            .filterIsInstance<VariantDto>()
            .map {
                KpmVariantInfo(
                    moduleId = module.id,
                    variant = it
                )
            }
            .forEach { kpmVariantInfo ->
                val variantInfoBytes = serializer.serializeVariantInfo(kpmVariantInfo)
                kpmFileStructure.kpmVariantInfoFilePath(kpmVariantInfo.variant.id).run {
                    parent.createDirectories()
                    writeBytes(variantInfoBytes)
                }
            }
    }

    private fun writePsm(module: KotlinModule) {

    }

    private fun packageArtifacts(module: KotlinModule) {
        zip(kpmFileStructure.metadataOutputDir, kpmFileStructure.metadataArtifact(module.id))

        for ((variantId, variant) in module.variants) {
            when (variant.platform) {
                Platform.JVM -> zip(kpmFileStructure.jvmOutputDir(variantId), kpmFileStructure.jvmArtifact(module.id, variantId))
                Platform.JS -> zip(kpmFileStructure.jsOutputDir(variantId), kpmFileStructure.jsArtifact(module.id, variantId))
                Platform.Native -> TODO("Not implemented")
            }
        }
    }

    private fun buildModuleDependencies(request: CompilationRequest): Map<ModuleId, KotlinModuleDependency> {
        val provider = PlainModuleDependencyProvider(serializer, kpmDtoTransformer, kpmFileStructure.metadataExtractDir)
        request.libs.map(Paths::get).forEach {
            provider.scanForStdlib(it)
            provider.scan(it)
        }
        return provider.moduleDependencies()
    }

    private fun extractModule(moduleConfig: DependencyModuleConfig): KotlinModuleDependency {
        val metaArtifact = moduleConfig.metaArtifact?.let(Paths::get) ?: error("Meta artifact not found")
        // Currently we support folders only, later we can extract it from zip directly
        val kpmFileBytes = Files.readAllBytes(metaArtifact.resolve("META-INF/kpm.json"))
        val module = serializer.deserialize(kpmFileBytes).let(kpmDtoTransformer::fromDto)

        val fragmentArtifacts = module.fragments.mapValues { metaArtifact.resolve(it.value.id).let(::listOf) }
        val variantArtifacts = moduleConfig.variants.mapValues {
            (listOf(it.value.artifact) + it.value.dependencies).map(Paths::get)
        }

        return KotlinModuleDependency(
            fragmentArtifacts = fragmentArtifacts,
            variantArtifacts = variantArtifacts,
            module = module
        )
    }

//    private fun buildTrivialModule(moduleConfig: DependencyModuleConfig): KotlinModuleDependency {
//        val fragments = listOf(
//            CommonFragment(
//                id = "common",
//                attributes = emptyMap(),
//                moduleDependencies = emptySet()
//            )
//        ) + moduleConfig.variants.map {
//            val attributes = it.value.attributes.toKpmAttributes()
//            val platformAttribute = (attributes[Platforms] as? Platforms) ?: error("Attribute $Platforms not found")
//            Variant(
//                id = it.key,
//                attributes = attributes,
//                moduleDependencies = it.value.dependencies.toSet(),
//                platform = platformAttribute.platforms.single()
//            )
//        }
//
//        val module = KotlinModule(
//            id = moduleConfig.id,
//            fragments = fragments.associateBy(Fragment::id),
//            refinements = moduleConfig.variants.mapValues { setOf("common") }
//        )
//
//        val fragmentArtifacts = if (moduleConfig.metaArtifact != null) {
//            mapOf("common" to listOf(Paths.get(moduleConfig.metaArtifact)))
//        } else {
//            mapOf("common" to emptyList())
//        }
//
//        val variantArtifacts = moduleConfig
//            .variants
//            .mapValues { (listOf(it.value.artifact) + it.value.dependencies).map(Paths::get) }
//
//        return KotlinModuleDependency(
//            fragmentArtifacts = fragmentArtifacts,
//            variantArtifacts = variantArtifacts,
//            module = module
//        )
//    }

    private fun KpmCompiler.compileAll(module: KotlinModule, config: Map<FragmentId, FragmentConfig>): ExitCode {
        val metadataCompilationExitCodes = module
            .iterateRefinementTree()
            .flatMap { fragments ->
                fragments.map {
                    it.id to compileMetadata(it.id)
                }
            }
            .toMap()

        val variantCompilationExitCodes = module.variants.values.associate { variant ->
            variant.id to compileVariant(variant.id)
        }

        // In case of error just return first one
        return (metadataCompilationExitCodes.values + variantCompilationExitCodes.values)
            .firstOrNull { it != ExitCode.OK }
            ?: ExitCode.OK
    }

    private fun parsePlatform(string: String) = when (string) {
        "js" -> Platform.JS
        "jvm" -> Platform.JVM
        "native" -> Platform.Native
        else -> error("Unknown platform: $string")
    }

    private fun buildKotlinModule(request: CompilationRequest): KotlinModuleDto {
        val fragments = mutableMapOf<FragmentId, FragmentDto>()
        val refinements = mutableMapOf<FragmentId, Set<FragmentId>>()

        for ((fragmentId, fragmentConfig) in request.fragmentMap) {
            val attributes = fragmentConfig.attributes

            val platform = attributes
                ?.get("platforms")
                ?.jsonArray
                ?.singleOrNull()
                ?.jsonPrimitive
                ?.contentOrNull

            fragments[fragmentId] =
                if (attributes != null && platform != null) {
                    VariantDto(
                        id = fragmentId,
                        settings = fragmentConfig.settings,
                        attributes = fragmentConfig.attributes,
                        moduleDependencies = fragmentConfig.moduleDependencies,
                        platform = platform
                    )
                } else {
                    CommonFragmentDto(
                        id = fragmentId,
                        settings = fragmentConfig.settings,
                        moduleDependencies = fragmentConfig.moduleDependencies,
                        attributes = emptyMap()
                    )
                }

            refinements[fragmentId] = fragmentConfig.refines.toSet()
        }

        return KotlinModuleDto(
            id = request.moduleId,
            fragments = fragments.values.toList(),
            refinement = refinements
        )
    }
}
