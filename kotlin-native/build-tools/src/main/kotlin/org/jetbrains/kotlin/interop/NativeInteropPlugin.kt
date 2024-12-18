/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.interop

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension
import org.jetbrains.kotlin.dependencies.NativeDependenciesPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanJvmInteropTask
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.NativePlugin
import org.jetbrains.kotlin.tools.NativeToolsExtension
import org.jetbrains.kotlin.tools.ToolExecutionTask
import org.jetbrains.kotlin.tools.libname
import org.jetbrains.kotlin.tools.obj
import org.jetbrains.kotlin.tools.solib
import java.io.File

open class NativeInteropPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.apply(plugin = "org.jetbrains.kotlin.jvm")
        target.apply<NativeDependenciesPlugin>()
        target.apply<NativePlugin>()

        val nativeInteropPlugin = target.extensions.create<NativeInteropExtension>("nativeInteropPlugin")

        val cppImplementation = target.configurations.create(CPP_IMPLEMENTATION_CONFIGURATION) {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(CppUsage.USAGE_ATTRIBUTE, target.objects.named(CppUsage.API))
                attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
            }
        }

        val cppLink = target.configurations.create(CPP_LINK_CONFIGURATION) {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(CppUsage.USAGE_ATTRIBUTE, target.objects.named(CppUsage.LIBRARY_LINK))
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
            }
        }

        val cppApiElements = target.configurations.create(CPP_API_ELEMENTS_CONFIGURATION) {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(CppUsage.USAGE_ATTRIBUTE, target.objects.named(CppUsage.API))
                attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
            }
        }

        val cppLinkElements = target.configurations.create(CPP_LINK_ELEMENTS_CONFIGURATION) {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(CppUsage.USAGE_ATTRIBUTE, target.objects.named(CppUsage.LIBRARY_LINK))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, target.objects.named(LibraryElements.DYNAMIC_LIB))
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
            }
        }

        val cppRuntimeElements = target.configurations.create(CPP_RUNTIME_ELEMENTS_CONFIGURATION) {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(CppUsage.USAGE_ATTRIBUTE, target.objects.named(CppUsage.LIBRARY_RUNTIME))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, target.objects.named(LibraryElements.DYNAMIC_LIB))
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
            }
        }

        val interopStubGenerator = target.configurations.create(INTEROP_STUB_GENERATOR_CONFIGURATION)
        val interopStubGeneratorCppRuntime = target.configurations.create(INTEROP_STUB_GENERATOR_CPP_RUNTIME_CONFIGURATION) {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(CppUsage.USAGE_ATTRIBUTE, target.objects.named(CppUsage.LIBRARY_RUNTIME))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, target.objects.named(LibraryElements.DYNAMIC_LIB))
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
            }
        }

        target.dependencies {
            interopStubGenerator(project(":kotlin-native:Interop:StubGenerator"))
            interopStubGenerator(project(":kotlin-native:endorsedLibraries:kotlinx.cli", "jvmRuntimeElements"))
            interopStubGeneratorCppRuntime(project(":kotlin-native:libclangInterop"))
            interopStubGeneratorCppRuntime(project(":kotlin-native:Interop:Runtime"))
            "api"(project(":kotlin-native:Interop:Runtime"))
            "testImplementation"(project(":kotlin-native:Interop:StubGeneratorConsistencyCheck", "tests-jar"))
        }

        val genTask = target.tasks.register<KonanJvmInteropTask>("genInteropStubs") {
            dependsOn(target.extensions.getByType<NativeDependenciesExtension>().hostPlatformDependency)
            dependsOn(target.extensions.getByType<NativeDependenciesExtension>().llvmDependency)
            interopStubGeneratorClasspath.from(interopStubGenerator)
            interopStubGeneratorNativeLibraries.from(interopStubGeneratorCppRuntime)
            outputDirectory.set(target.layout.buildDirectory.dir("nativeInteropStubs"))
            defFile.set(target.layout.projectDirectory.file(nativeInteropPlugin.defFileName))
            compilerOpts.set(nativeInteropPlugin.cCompilerArgs)
            compilerOpts.addAll(nativeInteropPlugin.commonCompilerArgs)
            headersDirs.workingDir.set(target.layout.projectDirectory)
            headersDirs.from(nativeInteropPlugin.selfHeaders.map { it.map { target.layout.projectDirectory.dir(it) } })
            headersDirs.from(cppImplementation)
            headersDirs.systemFrom(nativeInteropPlugin.systemIncludeDirs)
        }

        val prebuiltRoot = target.provider { target.layout.projectDirectory.dir("gen/main") }
        val generatedRoot = genTask.map { it.outputDirectory.get() }

        val bindingsRoot = nativeInteropPlugin.usePrebuiltSources.flatMap {
            if (it) prebuiltRoot else generatedRoot
        }

        val generatedTestsRoot = target.layout.projectDirectory.dir("tests-gen")

        val kotlinJvm = target.extensions.getByType<KotlinJvmProjectExtension>()
        kotlinJvm.sourceSets.named("main") {
            kotlin.srcDir(bindingsRoot.map { it.dir("kotlin") })
        }
        kotlinJvm.sourceSets.named("test") {
            kotlin.srcDir(generatedTestsRoot)
        }

        val usePrebuiltSources = nativeInteropPlugin.usePrebuiltSources

        val updatePrebuilt = target.tasks.register<Sync>("updatePrebuilt") {
            onlyIf { usePrebuiltSources.get() }
            into(prebuiltRoot)
            from(generatedRoot)
        }

        target.tasks.register("generateTests") {
            val packageName = "org.jetbrains.kotlin.konan.interop"
            val testName = "ConsistencyCheckTest"

            outputs.dir(generatedTestsRoot)
            val testDirectory = generatedTestsRoot.dir(packageName.replace('.', File.separatorChar))
            val testFile = testDirectory.file("$testName.kt")
            val testFileContents = """
                package $packageName

                class `${target.path.replace(':', ' ').trim()} $testName` : Abstract$testName()
            """.trimIndent()
            inputs.property("testFileContents", testFileContents)
            doLast {
                generatedTestsRoot.asFile.deleteRecursively()
                testDirectory.asFile.mkdirs()
                testFile.asFile.writeText(testFileContents)
            }
        }

        target.tasks.named<Test>("test") {
            jvmArgumentProviders.add(target.objects.newInstance<ConsistencyCheckArgumentsProvider>().apply {
                this.usePrebuiltSources.set(usePrebuiltSources)
                this.generatedRoot.set(generatedRoot)
                this.bindingsRoot.set(bindingsRoot)
                this.hostName.set(PlatformInfo.hostName)
                this.projectName.set(target.path)
                this.regenerateTaskName.set(updatePrebuilt.name)
            })
            useJUnitPlatform {}
        }

        target.afterEvaluate {
            applyFinish(target, bindingsRoot)
        }
    }

    private fun applyFinish(target: Project, bindingsRoot: Provider<Directory>): Unit = with(target) {
        val nativeInteropPlugin = extensions.getByType<NativeInteropExtension>()

        val defFileName = nativeInteropPlugin.defFileName.run {
            finalizeValue()
            get()
        }
        val commonCompilerArgs = nativeInteropPlugin.commonCompilerArgs.run {
            finalizeValue()
            get()
        }
        val cCompilerArgs = nativeInteropPlugin.cCompilerArgs.run {
            finalizeValue()
            get()
        }
        val cppCompilerArgs = nativeInteropPlugin.cppCompilerArgs.run {
            finalizeValue()
            get()
        }
        val selfHeaders = nativeInteropPlugin.selfHeaders.run {
            finalizeValue()
            get()
        }
        val systemIncludeDirs = nativeInteropPlugin.systemIncludeDirs.run {
            finalizeValue()
            get()
        }
        val linkerArgs = nativeInteropPlugin.linkerArgs.run {
            finalizeValue()
            get()
        }
        val additionalLinkedStaticLibraries = nativeInteropPlugin.additionalLinkedStaticLibraries.run {
            finalizeValue()
            get()
        }

        val cppImplementation = configurations.getByName(CPP_IMPLEMENTATION_CONFIGURATION)
        val cppLink = configurations.getByName(CPP_LINK_CONFIGURATION)

        val includeDirs = project.files(*systemIncludeDirs.toTypedArray(), *selfHeaders.toTypedArray(), cppImplementation)

        val stubsName = "${defFileName.removeSuffix(".def").split(".").reversed().joinToString(separator = "")}stubs"
        val library = solib(stubsName)

        val linkedStaticLibraries = project.files(cppLink.incoming.artifactView {
            attributes {
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.LINK_ARCHIVE))
            }
        }.files, *additionalLinkedStaticLibraries.toTypedArray())

        extensions.getByType<NativeToolsExtension>().apply {
            val obj = if (HostManager.hostIsMingw) "obj" else "o"
            suffixes {
                (".c" to ".$obj") {
                    tool(*hostPlatform.clangForJni.clangC("").toTypedArray())
                    val cflags = cCompilerArgs + commonCompilerArgs + includeDirs.map { "-I${it.absolutePath}" } + hostPlatform.clangForJni.hostCompilerArgsForJni
                    flags(*cflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
                }
                (".cpp" to ".$obj") {
                    tool(*hostPlatform.clang.clangCXX("").toTypedArray())
                    val cxxflags = cppCompilerArgs + commonCompilerArgs + includeDirs.map { "-I${it.absolutePath}" }
                    flags(*cxxflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
                }
            }
            sourceSet {
                "main-c" {
                    file(bindingsRoot.get().file("c/$stubsName.c").asFile.toRelativeString(layout.projectDirectory.asFile))
                }
                "main-cpp" {
                    dir("src/main/cpp")
                }
            }
            val objSet = arrayOf(sourceSets["main-c"]!!.transform(".c" to ".$obj"),
                    sourceSets["main-cpp"]!!.transform(".cpp" to ".$obj"))

            target(library, *objSet) {
                tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
                val ldflags = buildList {
                    addAll(linkedStaticLibraries.map { it.absolutePath })
                    cppLink.incoming.artifactView {
                        attributes {
                            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
                        }
                    }.files.flatMapTo(this) { listOf("-L${it.parentFile.absolutePath}", "-l${libname(it)}") }
                    addAll(linkerArgs)
                }
                flags("-shared", "-o", ruleOut(), *ruleInAll(), *ldflags.toTypedArray())
            }
        }

        tasks.named(library).configure {
            inputs.files(linkedStaticLibraries).withPathSensitivity(PathSensitivity.NONE)
        }
        tasks.named(obj(stubsName)).configure {
            inputs.dir(bindingsRoot.map { it.dir("c") }).withPathSensitivity(PathSensitivity.RELATIVE) // if C file was generated, need to set up task dependency
            includeDirs.forEach { inputs.dir(it).withPathSensitivity(PathSensitivity.RELATIVE) }
        }

        artifacts {
            selfHeaders.forEach { add(CPP_API_ELEMENTS_CONFIGURATION, layout.projectDirectory.dir(it)) }
            add(CPP_LINK_ELEMENTS_CONFIGURATION, tasks.named<ToolExecutionTask>(library).map { it.output })
            add(CPP_RUNTIME_ELEMENTS_CONFIGURATION, tasks.named<ToolExecutionTask>(library).map { it.output })
        }
    }

    companion object {
        const val CPP_IMPLEMENTATION_CONFIGURATION = "cppImplementation"
        const val CPP_LINK_CONFIGURATION = "cppLink"
        const val CPP_API_ELEMENTS_CONFIGURATION = "cppApiElements"
        const val CPP_LINK_ELEMENTS_CONFIGURATION = "cppLinkElements"
        const val CPP_RUNTIME_ELEMENTS_CONFIGURATION = "cppRuntimeElements"
        const val INTEROP_STUB_GENERATOR_CONFIGURATION = "interopStubGenerator"
        const val INTEROP_STUB_GENERATOR_CPP_RUNTIME_CONFIGURATION = "interopStubGeneratorCppRuntime"
    }
}