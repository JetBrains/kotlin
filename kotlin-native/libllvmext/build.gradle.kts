/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.tools.lib
import org.jetbrains.kotlin.tools.solib
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.Family.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.ToolExecutionTask

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("kotlin.native.build-tools-conventions")
    id("native")
    id("native-dependencies")
    id("git-clang-format")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

val library = lib("llvmext")
val kotlinLlvmPlugin = solib("LLVMKotlin") // standard LLVM naming

native {
    val obj = if (HostManager.hostIsMingw) "obj" else "o"
    val cxxflags = mutableListOf(
            // Using the same flags as the release llvm build.
            // But only keep -Wall diagnostics.
            "-Wall",
            "-O3",
            "-DNDEBUG",
            "-std=c++17",
            "-fno-exceptions",
            "-funwind-tables",
            "-fno-rtti",
            "-Werror", // fail on any warning, or we won't ever catch them
            "-I${llvmDir}/include",
            "-Isrc/main/include",
            *reproducibilityCompilerFlags,
    )
    when (org.jetbrains.kotlin.konan.target.HostManager.host.family) {
        LINUX -> {
            cxxflags.addAll(listOf("-DKONAN_LINUX=1"))
        }
        MINGW -> {
            cxxflags += listOf(
                "-DKONAN_WINDOWS=1",
                "-Wno-unused-command-line-argument",
            )
        }
        OSX -> {
            cxxflags += "-DKONAN_MACOS=1"
        }
        else -> Unit
    }
    suffixes {
        (".cpp" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
            flags(*cxxflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }

    }
    sourceSet {
        "main" {
            dir("src/main/cpp")
        }
    }
    val objSet = sourceSets["main"]!!.transform(".cpp" to ".$obj")

    target(library, objSet) {
        tool(*hostPlatform.clangForJni.llvmAr("").toTypedArray())
        flags("-qcv", ruleOut(), *ruleInAll())
    }

    if (!HostManager.hostIsMingw) {
        // This plugin contains undefined symbols: to LLVM itself.
        // On macOS and Linux we can just advertise these symbols as undefined, on MinGW we can't
        // do that (not easily at any rage).
        // This plugin is currently only used for testing with `FileCheck`, and the passes work the
        // same way across all targets. So, let's just not build it for MinGW for the time being.
        target(kotlinLlvmPlugin, objSet) {
            tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
            val ldflags = buildList {
                if (HostManager.hostIsMac) {
                    // The built dylib references llvm symbols.
                    add("-Wl,-undefined")
                    add("-Wl,dynamic_lookup")
                    // Set install_name to a non-absolute path.
                    add("-Wl,-install_name,@rpath/$kotlinLlvmPlugin")
                    // Unlike -ffile-prefix-map for clang, it's only possible to add a single directory for -oso_prefix:
                    // in the `ld_classic`, for example, see
                    // https://github.com/apple-oss-distributions/ld64/blob/1a4389663d65d6630e4b3e31ace2a86b6183b452/src/ld/Options.cpp#L4249
                    // Currently, we only need it for dependencies from inside the repo, so strip the root project's absolute path.
                    add("-Wl,-oso_prefix,${isolated.rootProject.projectDirectory.asFile}")
                }
            }
            flags("-shared", "-o", ruleOut(), *ruleInAll(), *ldflags.toTypedArray())
        }
    }
}

val cppApiElements = configurations.create("cppApiElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.API))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

val cppLinkElements = configurations.create("cppLinkElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_LINK))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.LINK_ARCHIVE))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

artifacts {
    add(cppApiElements.name, layout.projectDirectory.dir("src/main/include"))
    add(cppLinkElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
}

val printLlvmDir = tasks.register("printLlvmDir") {
    dependsOn(nativeDependencies.llvmDependency)
    doLast {
        println(nativeDependencies.llvmPath)
    }
}

val hostLlvmDistribution = configurations.create("hostLlvmDistribution") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    testFixturesApi(kotlinTest("junit5"))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesImplementation(project(":native:executors"))
    testFixturesImplementation(project(":native:kotlin-native-utils"))

    testRuntimeOnly(libs.junit.jupiter.engine)

    hostLlvmDistribution(project(":kotlin-native:dependencies", "llvmDevBinaryData"))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

open class TestArgumentProvider @Inject constructor(
        objectFactory: ObjectFactory,
) : CommandLineArgumentProvider {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE) // Only file contents matter for test execution.
    val llvmPlugin: RegularFileProperty = objectFactory.fileProperty()

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE) // Only paths within the directory matter.
    val llvmDistribution: ConfigurableFileCollection = objectFactory.fileCollection()

    override fun asArguments(): Iterable<String> = listOf(
            "-Dkotlin.llvmPlugin=${llvmPlugin.get().asFile}",
            "-Dkotlin.llvmDistribution=${llvmDistribution.singleFile}",
    )
}

projectTests {
    testData(project.isolated, "testData")
    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateFileCheckTestsKt", generateTestsInBuildDirectory = true, configureTestDataCollection = {
        filePatterns.set(listOf("**/*.ll"))
    })
    testTask {
        if (HostManager.hostIsMingw) {
            enabled = false
        } else {
            jvmArgumentProviders.add(objects.newInstance<TestArgumentProvider>().apply {
                llvmPlugin.fileProvider(tasks.named<ToolExecutionTask>(kotlinLlvmPlugin).map { it.output })
                llvmDistribution.from(hostLlvmDistribution)
            })
        }
    }
}
