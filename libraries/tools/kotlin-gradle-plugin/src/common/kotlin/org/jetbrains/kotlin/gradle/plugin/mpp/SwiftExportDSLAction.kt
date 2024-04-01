/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.KOTLIN_STDLIB_MODULE_NAME
import org.jetbrains.kotlin.gradle.internal.dumpKlibs
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmRun
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileConfig
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import java.io.File

internal val SwiftExportDSLAction = KotlinProjectSetupAction {

    this.project.launch {
        val targets = multiplatformExtension.awaitTargets().filterIsInstance<KotlinNativeTarget>()
        generateDSLForTarget(targets.single())
    }
}

abstract class SwiftExportDSL : DefaultTask() {

    @get:InputFiles
    abstract val inputKlibs: ListProperty<File>

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun action() {
        val modules = dumpKlibs(
            inputKlibs.get().filter { it.extension == "klib" }.map { it.path }
        )
        val packages = modules.map { it.packages }.flatten().toSet().sorted()
        val packagesFromModuleName = modules.associateBy({ it.libraryName }, { it.packages })

        val swiftExportModulesFile = output.file("SwiftExportModules.generated.kt").get().asFile
        val moduleStrings = modules.map {
            "data object Module${prepareName(it.libraryName)} : KotlinModule(\"${it.libraryName}\")".prependIndent()
        }
        val packageStrings = packages.map {
            "data object Package${prepareName(it)} : KotlinPackage(\"${it}\")".prependIndent()
        }

        swiftExportModulesFile.writeText(
            arrayOf(
                "sealed class KotlinModule(val moduleName: String) {",
                *moduleStrings.toTypedArray(),
                "}\n",
                "sealed class KotlinPackage(val packageName: String) {",
                *packageStrings.toTypedArray(),
                "}\n",
                """
                class SwiftModule(
                    val name: String,
                    val kotlinModules: List<KotlinModule>,
                ) {
                    override fun toString(): String {
                        return (listOf(
                            "SwiftModule(",
                            ("name=" + name + ",").prependIndent(),
                        ) + listOf(
                            "kotlinModules=[".prependIndent(),
                        ) + kotlinModules.map {
                            it.toString().prependIndent().prependIndent() + ","
                        } + listOf(
                            "]".prependIndent(),
                            ")",
                        )).joinToString("\n")
                    }
                }
                
                class SwiftExportConfiguration(
                    val swiftModules: List<SwiftModule>,
                    val collapsedPackages: List<KotlinPackage>,
                ) {
                    override fun toString(): String {
                        return (listOf(
                            "SwiftExportConfiguration(",
                            "swiftModules=[".prependIndent(),
                        ) + swiftModules.map {
                            it.toString().prependIndent().prependIndent() + ","
                        } + listOf(
                            "],".prependIndent(),
                            "collapsedPackages=[".prependIndent(),
                        ) + collapsedPackages.map { 
                            it.toString().prependIndent().prependIndent() + ","
                        } + listOf(
                            "]".prependIndent(),
                            ")",
                        )).joinToString("\n")
                    }
                }
                
                class SwiftExportMain {
                    companion object {
                        @JvmStatic
                        fun main(args: Array<String>) {
                            val config: SwiftExportConfiguration = exportSwiftApi()
                            println(config)
                        }
                    }
                }
                
                """.trimIndent()
            ).joinToString("\n")
        )
    }

    private fun prepareName(name: String): String {
        var nameString = ""
        var index = 0
        val capitalizedPrefix = setOf('_', ':', '.', '-')
        while (index <= name.indices.last) {
            if (index == 0) {
                nameString += name[index].toUpperCase()
                // FIXME: ???
            } else if (name[index] in capitalizedPrefix && index < name.indices.last) {
                index += 1
                nameString += name[index].toUpperCase()
            } else {
                nameString += name[index]
            }
            index += 1
        }
        return nameString
    }

}

private fun Project.generateDSLForTarget(target: KotlinNativeTarget) {
    val comp = target.compilations.getByName("main")
    val generatedSwiftApi = project.layout.buildDirectory.dir("swiftApi")
    val prepareDSLTask = tasks.register("prepareSwiftExportDSL", SwiftExportDSL::class.java) { task ->
        task.inputKlibs.set(comp.internal.configurations.compileDependencyConfiguration)
        // FIXME: target name is part of output dir? or inputs?
        task.output.set(generatedSwiftApi)
    }

    val stdlibConf = configurations.create("swiftApiStdlib")
    val stdlibDependency = project.objects.providerWithLazyConvention { kotlinExtension.coreLibrariesVersion }.map {
        dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_STDLIB_MODULE_NAME:${it}")
    }
    stdlibConf.dependencies.addLater(stdlibDependency)

    val userDeclaredSwiftApi = project.layout.projectDirectory.dir("swiftApi")
    multiplatformExtension.sourceSets.create("swiftApi") {
        it.kotlin.setSrcDirs(
            listOf(
                generatedSwiftApi,
                userDeclaredSwiftApi,
            )
        )
    }

    val compileSwiftExportDSL = tasks.register(
        "compileSwiftExportDSL",
        KotlinCompile::class.java,
        project.objects
            .newInstance(KotlinJvmCompilerOptionsDefault::class.java)
    )
    KotlinCompileConfig(project, kotlinExtension).execute(compileSwiftExportDSL)
    val swiftApiClasses = project.layout.buildDirectory.dir("swiftApiClasses")
    compileSwiftExportDSL.configure {
        it.dependsOn(prepareDSLTask)
        it.source(generatedSwiftApi, userDeclaredSwiftApi)
        it.destinationDirectory.set(swiftApiClasses)
        it.multiPlatformEnabled.set(true)
        it.compilerOptions.moduleName.set("swiftApiModule")
        it.libraries.from(stdlibConf)
    }

    tasks.register(
        "runSwiftExportDSL",
        KotlinJvmRun::class.java
    ) { task ->
        task.dependsOn(compileSwiftExportDSL)
        task.mainClass.set("SwiftExportMain")

        task.classpath(
            swiftApiClasses,
            stdlibConf,
        )

        project.extensions.findByType(JavaToolchainService::class.java)?.let { toolchainService ->
            val toolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain
            task.javaLauncher.convention(toolchainService.launcherFor(toolchain))
        }
    }
}