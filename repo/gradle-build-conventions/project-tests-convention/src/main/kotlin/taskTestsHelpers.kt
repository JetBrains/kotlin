/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages

/*
 * Task-level counterparts to the project-level helpers in [ProjectTestsExtension].
 *
 * Use these inside a `testTask {}` or `nativeTestTask {}` body to declare inputs for a
 * specific test task only, rather than for all test tasks in the project.
 *
 * Example:
 * ```
 * projectTests {
 *     testTask(jUnitMode = JUnitMode.JUnit5) {
 *         @OptIn(KotlinCompilerDistUsage::class)
 *         withDist()  // only this task declares dist as an input
 *     }
 *     nativeTestTask(...) {
 *         // dist is NOT an input here
 *     }
 * }
 * ```
 */

private fun Test.configurationElements(
    name: String,
    dependencies: DependencySet.() -> Unit,
    attributes: AttributeContainer.() -> Unit = {},
): NamedDomainObjectProvider<out Configuration> {
    val conf = project.configurations.dependencyScopeNamedOrRegister(name) {
        dependencies(this.dependencies)
    }
    return project.configurations.resolvableNamedOrRegister(name + "Classpath") {
        extendsFrom(conf)
        isTransitive = false
        this.attributes(attributes)
    }
}

@KotlinCompilerDistUsage
fun Test.withDist() {
    project.normalization {
        runtimeClasspath {
            ignore("**/build.txt")
            ignore("*.spdx.json")
        }
    }

    val dist = configurationElements("distForTests", dependencies = {
        add(project.dependencies.project(path = ":kotlin-compiler", configuration = "distElements"))
    })
    addClasspathProperty(dist, TestCompilePaths.KOTLIN_DIST_PATH)
}

fun Test.withThirdPartyAnnotations() {
    addDirectoryProperty(
        project.rootDir.resolve("third-party/annotations"),
        TestCompilePaths.KOTLIN_THIRDPARTY_ANNOTATIONS_PATH
    )
}

fun Test.withThirdPartyJava8Annotations() {
    addDirectoryProperty(
        project.rootDir.resolve("third-party/java8-annotations"),
        TestCompilePaths.KOTLIN_THIRDPARTY_JAVA8_ANNOTATIONS_PATH
    )
}

fun Test.withThirdPartyJava9Annotations() {
    addDirectoryProperty(
        project.rootDir.resolve("third-party/java9-annotations"),
        TestCompilePaths.KOTLIN_THIRDPARTY_JAVA9_ANNOTATIONS_PATH
    )
}

fun Test.withThirdPartyJsr305() {
    addDirectoryProperty(
        project.rootDir.resolve("third-party/jsr305"),
        TestCompilePaths.KOTLIN_THIRDPARTY_JSR305_PATH
    )
}

fun Test.withMockJdkRuntime() {
    addFileProperty(
        project.rootDir.resolve("third-party/mockJDKs/mockJDK/jre/lib/rt.jar"),
        TestCompilePaths.KOTLIN_MOCKJDK_RUNTIME_PATH
    )
}

fun Test.withMockJDKModifiedRuntime() {
    addFileProperty(
        project.rootDir.resolve("third-party/mockJDKs/mockJDKModified/rt.jar"),
        TestCompilePaths.KOTLIN_MOCKJDKMODIFIED_RUNTIME_PATH
    )
}

fun Test.withMockJdkAnnotationsJar() {
    addFileProperty(
        project.rootDir.resolve("third-party/mockJDKs/mockJDK/jre/lib/annotations.jar"),
        TestCompilePaths.KOTLIN_MOCKJDK_ANNOTATIONS_PATH
    )
}

fun Test.withJvmStdlibAndReflect() {
    addClasspathProperty(
        configurationElements(
            "stdlibRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-stdlib")) }
        ), TestCompilePaths.KOTLIN_FULL_STDLIB_PATH
    )
    addClasspathProperty(
        configurationElements(
            "stdlibMinimalRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-stdlib-jvm-minimal-for-test")) }
        ), TestCompilePaths.KOTLIN_MINIMAL_STDLIB_PATH
    )
    addClasspathProperty(
        configurationElements(
            "kotlinReflectJarForTests",
            dependencies = { add(project.dependencies.project(":kotlin-reflect")) }
        ), TestCompilePaths.KOTLIN_REFLECT_JAR_PATH
    )
}

fun Test.withJvmStdlibSources() {
    addClasspathProperty(
        configurationElements(
            "stdlibRuntimeSourcesForTests",
            dependencies = { add(project.dependencies.project(":kotlin-stdlib", "distSources")) }
        ), TestCompilePaths.KOTLIN_FULL_STDLIB_SOURCES_PATH
    )
}

fun Test.withStdlibCommon() {
    addClasspathProperty(
        configurationElements(
            "stdlibCommonRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-stdlib", "commonMainMetadataElements")) }
        ), TestCompilePaths.KOTLIN_COMMON_STDLIB_PATH
    )
}

fun Test.withScriptRuntime() {
    addClasspathProperty(
        configurationElements(
            "scriptRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-script-runtime")) }
        ), TestCompilePaths.KOTLIN_SCRIPT_RUNTIME_PATH
    )
}

fun Test.withTestJar() {
    addClasspathProperty(
        configurationElements(
            "kotlinTestJarForTests",
            dependencies = { add(project.dependencies.project(":kotlin-test")) }
        ), TestCompilePaths.KOTLIN_TEST_JAR_PATH
    )
}

fun Test.withAnnotations() {
    addClasspathProperty(
        configurationElements(
            "kotlinAnnotationsForTests",
            dependencies = { add(project.dependencies.project(":kotlin-annotations-jvm")) }
        ), TestCompilePaths.KOTLIN_ANNOTATIONS_PATH
    )
}

fun Test.withStdlibWeb() {
    addClasspathProperty(
        configurationElements(
            "stdlibWebRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-stdlib", "webMainMetadataElements")) }
        ), TestCompilePaths.KOTLIN_WEB_STDLIB_KLIB_PATH
    )
}

fun Test.withJsRuntime() {
    addClasspathProperty(
        configurationElements(
            "stdlibJsRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-stdlib", "distJsKlib")) }
        ), TestCompilePaths.KOTLIN_JS_STDLIB_KLIB_PATH
    )
    addClasspathProperty(
        configurationElements(
            "stdlibJsMinimalRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-stdlib-js-ir-minimal-for-test", "jsRuntimeElements")) },
            attributes = {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging::class.java, KlibPackaging.NON_PACKED))
            }
        ), TestCompilePaths.KOTLIN_JS_REDUCED_STDLIB_PATH
    )
    addClasspathProperty(
        configurationElements(
            "testJsRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-test", "jsRuntimeElements")) }
        ), TestCompilePaths.KOTLIN_JS_KOTLIN_TEST_KLIB_PATH
    )
}

fun Test.withWasmRuntime() {
    addClasspathProperty(
        configurationElements(
            "stdlibWasmJsRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-stdlib", "wasmJsRuntimeElements")) },
            attributes = {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging::class.java, KlibPackaging.NON_PACKED))
            }
        ), TestCompilePaths.KOTLIN_WASM_JS_STDLIB_KLIB_PATH
    )
    addClasspathProperty(
        configurationElements(
            "stdlibWasmWasiRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-stdlib", "wasmWasiRuntimeElements")) },
            attributes = {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging::class.java, KlibPackaging.NON_PACKED))
            }
        ), TestCompilePaths.KOTLIN_WASM_WASI_STDLIB_KLIB_PATH
    )
    addClasspathProperty(
        configurationElements(
            "testWasmJsRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-test", "wasmJsRuntimeElements")) },
            attributes = {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging::class.java, KlibPackaging.NON_PACKED))
            }
        ), TestCompilePaths.KOTLIN_WASM_JS_KOTLIN_TEST_KLIB_PATH
    )
    addClasspathProperty(
        configurationElements(
            "testWasmWasiRuntimeForTests",
            dependencies = { add(project.dependencies.project(":kotlin-test", "wasmWasiRuntimeElements")) },
            attributes = {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                attributes.attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging::class.java, KlibPackaging.NON_PACKED))
            }
        ), TestCompilePaths.KOTLIN_WASM_WASI_KOTLIN_TEST_KLIB_PATH
    )
}

fun Test.withScriptingPlugin() {
    addClasspathProperty(
        configurationElements(
            "scriptingPluginForTests",
            dependencies = {
                add(project.dependencies.project(":kotlin-scripting-compiler"))
                add(project.dependencies.project(":kotlin-scripting-compiler-impl"))
                add(project.dependencies.project(":kotlin-scripting-common"))
                add(project.dependencies.project(":kotlin-scripting-jvm"))
            }
        ), TestCompilePaths.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH
    )
}

fun Test.withTestScriptDefinition() {
    addClasspathProperty(
        configurationElements(
            "testScriptDefinitionForTests",
            dependencies = { add(project.dependencies.project(":plugins:scripting:test-script-definition", "testFixturesApiElements")) }
        ), TestCompilePaths.KOTLIN_TEST_SCRIPT_DEFINITION_CLASSPATH
    )
}

fun Test.withPluginSandboxAnnotations() {
    addClasspathProperty(
        configurationElements(
            "pluginSandboxAnnotationsJar",
            dependencies = { add(project.dependencies.project(":plugins:plugin-sandbox:plugin-annotations")) }
        ), TestCompilePaths.PLUGIN_SANDBOX_ANNOTATIONS_JAR_PATH
    )
    addClasspathProperty(
        configurationElements(
            "pluginSandboxAnnotationsJsKlib",
            dependencies = { add(
                project.dependencies.project(":plugins:plugin-sandbox:plugin-annotations", "jsRuntimeElements")
            )},
            attributes = {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_RUNTIME))
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
            }
        ), TestCompilePaths.PLUGIN_SANDBOX_ANNOTATIONS_JS_KLIB_PATH
    )
    addClasspathProperty(
        configurationElements(
            "pluginSandboxAnnotationsWasmKlib",
            dependencies = {
                add(project.dependencies.project(":plugins:plugin-sandbox:plugin-annotations", "wasmJsRuntimeElements"))
            },
            attributes = {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_RUNTIME))
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
            }
        ), TestCompilePaths.PLUGIN_SANDBOX_ANNOTATIONS_WASM_KLIB_PATH
    )
}

fun Test.withPluginSandboxJar() {
    addClasspathProperty(
        configurationElements(
            "pluginSandboxJar",
            dependencies = { add(project.dependencies.project(":plugins:plugin-sandbox")) }
        ), TestCompilePaths.PLUGIN_SANDBOX_JAR_PATH
    )
}
