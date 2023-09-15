/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.DefaultTask

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.runCommand
import java.io.File
import javax.inject.Inject


import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.supportedTargets
import org.jetbrains.kotlin.gradle.utils.mapToFile

@Serializable
sealed class SpmDependencyLocation : java.io.Serializable

@Serializable
data class PathLocation(val path: String) : SpmDependencyLocation()
@Serializable
data class UrlLocation(val url: String, val version: String) : SpmDependencyLocation()

@Serializable
data class SwiftDependency(
    val location: SpmDependencyLocation, val packageName: String, val products: List<String>
) : java.io.Serializable

abstract class PackageSwiftGenerateTask @Inject constructor(projectLayout: ProjectLayout) : DefaultTask() {

    @get:Input
    abstract val dependencies: ListProperty<SwiftDependency>

    @get:OutputFile
    val packageSwiftFile: Provider<RegularFile> = projectLayout.buildDirectory.file("spm/Package.swift")

    @TaskAction
    fun generate() {
        val dependencies = this.dependencies.get()
        val dependenciesString = dependencies.map {
            when (it.location) {
                is UrlLocation -> ".package(url: \"${it.location.url}\", exact: \"${it.location.version}\")"
                is PathLocation -> ".package(path: \"${it.location.path}\")"
            }
        }.joinToString(", ")

        val productsString = dependencies.flatMap { dep ->
            dep.products.map { product ->
                ".product(name: \"${product}\", package: \"${dep.packageName}\")"
            }
        }.joinToString(", ")

        // -all_load to reexport everything in a single library
        val outputString = """
            // swift-tools-version: 5.8

            import PackageDescription

            let package = Package(
                name: "FakePackage",
                products: [
                    .library(
                        name: "FakeLibrary",
                        type: .static,
                        targets: ["FakeTarget"]
                    ),
                ],
                dependencies: [$dependenciesString],
                targets: [
                    .target(
                        name: "FakeTarget",
                        dependencies: [${productsString}],
                        linkerSettings: [.unsafeFlags(["-all_load"])]
                    ),
                ]
            )
        """.trimIndent()

        val output = packageSwiftFile.getFile()
        val container = output.parentFile
        container.mkdirs()
        val fakeTargetDir = container.resolve("Sources/FakeTarget")
        fakeTargetDir.mkdirs()
        fakeTargetDir.resolve("Lib.swift").createNewFile()
        output.writeText(outputString)
    }

}


abstract class BuildSwiftPackage @Inject constructor() : DefaultTask() {

    @get:InputFile
    abstract val packageSwiftFile: RegularFileProperty

    @get:OutputDirectory
    val buildDirectory: Provider<File> = packageSwiftFile.map {
        it.asFile.parentFile.resolve(".build")
    }

    @TaskAction
    fun build() {
        runCommand(
            listOf("swift", "build")
        ) {
            directory(packageSwiftFile.getFile().parentFile)
        }
    }

}

@Suppress("JSON_FORMAT_REDUNDANT")
abstract class CInteropProcessWithMultipleLibraries @Inject constructor(
    @Internal val projectLayout: ProjectLayout
) : DefaultTask() {

    @get:InputFile
    abstract val packageSwiftFile: RegularFileProperty

    @get:OutputDirectory
    val interopsDirectory: Provider<File> = projectLayout.buildDirectory.dir("spmInterop").mapToFile()

    @get:OutputFile
    val targetsDescription: Provider<File> = interopsDirectory.map { it.resolve("targetsDescription.json") }

    @Serializable
    private data class BuildDescription(val targetDependencyMap: Map<String, List<String>>)

//    @Serializable
//    sealed class PifBuildSettingValue {
//        @Serializable
//        data class PifBuildSettingValueString(val value: String) : PifBuildSettingValue()
//        @Serializable
//        data class PifBuildSettingValueList(val values: List<String>) : PifBuildSettingValue()
//    }

    @Serializable
    private data class PifBuildSettings(
        val HEADER_SEARCH_PATHS: List<String>? = null,
        val LIBRARY_SEARCH_PATHS: List<String>? = null,
        val FRAMEWORK_SEARCH_PATHS: List<String>? = null,
    )
    @Serializable
    private data class PifBuildConfiguration(val buildSettings: PifBuildSettings)

    @Serializable
    private data class PifItemContents(val buildConfigurations: List<PifBuildConfiguration>? = null, val name: String? = null)

    @Serializable
    private data class PifItem(val type: String, val contents: PifItemContents)

    @Serializable
    data class TargetsDescription(
        val targetToLibraries: Map<String, List<String>>,
        val targetToFrameworks: Map<String, List<String>>,
    ) : java.io.Serializable

//    object ModuleSerializer : JsonContentPolymorphicSerializer<PifBuildSettingValue>(PifBuildSettingValue::class) {
//        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out PifBuildSettingValue> {
//            element.
//            return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
//                "team" -> TeamModule.serializer()
//                "org" -> OrgModule.serializer()
//                else -> throw Exception("Unknown Module: key 'type' not found or does not matches any module type")
//            }
//        }
//    }

//    @Serializable
//    private data class PifDump(val )

    @TaskAction
    fun processInterops() {
        val buildDir = packageSwiftFile.get().asFile.parentFile.resolve(".build")
        val buildDescription = Json {
            ignoreUnknownKeys = true
        }.decodeFromStream<BuildDescription>(buildDir.resolve("debug/description.json").inputStream())

        val binaryFrameworks = mutableListOf<String>()
        buildDir.resolve("debug").listFiles()?.forEach {
            if (it.extension == "framework") {
                binaryFrameworks.add(it.nameWithoutExtension)
            }
        }

        val sortedTargetsForCInterop = mutableListOf<String>()
        sortedTargetsForCInterop.addAll(binaryFrameworks)
        val visited = LinkedHashSet<String>()

        fun dfsTopSort(target: String) {
            if (target in visited) return
            visited.add(target)
            for (dependency in buildDescription.targetDependencyMap[target] ?: emptyList()) {
                dfsTopSort(dependency)
            }
            sortedTargetsForCInterop.add(target)
        }
        dfsTopSort("FakeTarget")
        sortedTargetsForCInterop.removeLast() // remove FakeTarget

        val pifDump = Json {
            ignoreUnknownKeys = true
        }.decodeFromString<List<PifItem>>(
            runCommand(
                listOf("swift", "package", "dump-pif")
            ) {
                directory(packageSwiftFile.getFile().parentFile)
            }
        )

        fun filterInherited(flags: List<String>): List<String> {
            return flags.filter { it != "\$(inherited)" }
        }

        val targetToHeaders = mutableMapOf<String, List<String>>()
        val targetToLibraries = mutableMapOf<String, List<String>>()
        val targetToFrameworks = mutableMapOf<String, List<String>>()
        pifDump.forEach { item ->
            if (item.type != "target") return@forEach
            targetToHeaders[item.contents.name!!] = filterInherited(item.contents.buildConfigurations!![0].buildSettings.HEADER_SEARCH_PATHS ?: emptyList())
            targetToLibraries[item.contents.name!!] = filterInherited(item.contents.buildConfigurations!![0].buildSettings.LIBRARY_SEARCH_PATHS ?: emptyList())
            targetToFrameworks[item.contents.name!!] = filterInherited(item.contents.buildConfigurations!![0].buildSettings.FRAMEWORK_SEARCH_PATHS ?: emptyList())
        }

        targetsDescription.get().writeText(
            Json.encodeToString<TargetsDescription>(TargetsDescription(targetToLibraries, targetToFrameworks))
        )

        val spmInteropDir = interopsDirectory.get()
        spmInteropDir.mkdirs()

        val konanTargetNames = project.multiplatformExtension.supportedTargets().map { it.konanTarget.name }

        for (target in sortedTargetsForCInterop) {
            val defFilePath = spmInteropDir.resolve("$target.def")
            defFilePath.writeText(
                """
                language = Objective-C
                modules = $target
                """.trimIndent()
            )

            try {
                konanTargetNames.forEach { konanTargetName ->
                    runCommand(
                        listOf(
                            "/Users/Timofey.Solonin/.konan/kotlin-native-prebuilt-macos-aarch64-1.9.20-Beta-224/bin/cinterop",
                            "-def", defFilePath.path,
                            "-target", konanTargetName,
                            *((buildDescription.targetDependencyMap[target] ?: emptyList()) + binaryFrameworks).filter { it != target }.flatMap {
                                val argsFromDependency = mutableListOf(
                                    "-compiler-option", "-I${buildDir.resolve("debug/$it.build")}"
                                )
                                val expectedDependencyLibPath = spmInteropDir.resolve("$it.klib")
                                if (expectedDependencyLibPath.exists()) {
                                    argsFromDependency.addAll(listOf("-library", expectedDependencyLibPath.path))
                                }
                                return@flatMap argsFromDependency
                            }.toTypedArray(),
                            "-compiler-option", "-F${buildDir.resolve("debug")}",
                            "-compiler-option", "-I${buildDir.resolve("debug/$target.build")}",
                            *targetToHeaders.getOrElse(target, { emptyList() }).flatMap {
                                listOf("-compiler-option", "-I$it")
                            }.toTypedArray(),
                            "-compiler-option", "-fmodules",
                            "-Xmodule-name", target,
                            "-o", spmInteropDir.resolve("${target}_$konanTargetName.klib").path
                        )
                    )
                }
            } catch (e: Throwable) {
                println(e)
            }
        }
    }
}

