/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    `java-test-fixtures`
    id("test-inputs-check")
}

kotlin {
    explicitApi()
    compilerOptions {
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "org.jetbrains.dokka.InternalDokkaApi",
        )
    }
}

// This module was written against a stable Kotlin release and relies on plain component1()/component2()
// destructuring semantics. Override the repo-wide "-Xname-based-destructuring=complete" (set in
// common-configuration.gradle.kts) back down to "only-syntax" so short-form destructuring by variable
// name doesn't change behavior here. Registered after the `plugins {}` block applies common-configuration,
// so this action runs after (and therefore overrides) the one added there.
//
// It also disables "ErrorAboutDataClassCopyVisibilityChange", another language-version-2.5 deprecation-phase-2
// diagnostic that turns pre-existing data classes with non-public constructors (copied as-is from this
// module's original codebase) from a warning into a compile error.
//
// Finally, this module's vendored source carries pre-existing warnings (deprecated stdlib calls, etc.)
// that predate this migration and are out of scope to fix here, so all-warnings-as-errors is turned off
// for this module only (common-configuration otherwise enables it repo-wide).
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xname-based-destructuring=only-syntax")
        freeCompilerArgs.add("-XXLanguage:-ErrorAboutDataClassCopyVisibilityChange")
        allWarningsAsErrors.set(false)
    }
}

val intellijVersion = kotlinBuildProperties.versionsProperty("intellijSdk").get()

dependencies {
    api(project(":tools:kotlin-documentation-model:model"))

    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jsoup)
    implementation(libs.jetbrains.markdown)

    runtimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    runtimeOnly(libs.intellij.fastutil)
    runtimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    runtimeOnly(commonDependency("one.util:streamex"))
    runtimeOnly("com.jetbrains.intellij.platform:util-jdom:$intellijVersion") { isTransitive = false }
    runtimeOnly(libs.kotlinx.serialization.core) { isTransitive = false }

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-standalone"))
    implementation(project(":analysis:light-classes-base"))
    runtimeOnly(project(":analysis:analysis-api-platform-interface"))
    runtimeOnly(project(":analysis:analysis-api-fir"))
    runtimeOnly(project(":analysis:low-level-api-fir"))
    runtimeOnly(project(":analysis:symbol-light-classes"))
    runtimeOnly(project(":analysis:analysis-api-standalone:analysis-api-standalone-fir"))

    implementation(project(":compiler:cli-base"))
    implementation(project(":core:language.targets.jvm"))
    implementation(project(":js:js.config"))
    implementation(project(":native:native.config"))
    implementation(project(":wasm:wasm.config"))

    testFixturesImplementation(kotlinStdlib())
    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesApi(kotlinTest("junit5"))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(intellijCore())
    testImplementation(libs.junit.jupiter.params)
}

//region Kotlin/Native stdlib klib, needed by tests with `NATIVE`-platform source sets.
// `:kotlin-native:*` projects only exist with `kotlin.native.enabled=true`; otherwise the property is absent
// and tests that need it are skipped via `Assume` (see `nativeStdlibKlib` in TestSupport.kt).
if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
    val nativeStdlibKlib = configurations.dependencyScope("nativeStdlibKlib")
    val nativeStdlibKlibClasspath = configurations.resolvable("nativeStdlibKlibClasspath") {
        extendsFrom(nativeStdlibKlib.get())
    }
    dependencies {
        nativeStdlibKlib(project(":kotlin-native:runtime", configuration = "nativeStdlib"))
    }
    tasks.withType<Test>().configureEach {
        jvmArgumentProviders.add(
            objects.newInstance<SystemPropertyClasspathProvider>().apply {
                property.set("kotlin.native.stdlib.klib.path")
                classpath.from(nativeStdlibKlibClasspath)
            }
        )
    }
}
//endregion

// TODO KTI-3693  use withJsRuntime()
//region Kotlin/JS stdlib klib, needed by tests with `JS`-platform source sets.
// `withJsRuntime()` is disabled under isolated projects because two of its klibs carry NPM dependencies
// resolved on the root project (OSIP-739), so CI fails with "Property kotlin.js.stdlib.klib.path is
// not defined". Wire the property directly instead: `distJsKlib` is a plain klib without NPM and the
// dependencyScope/resolvable pattern resolves lazily at execution time, so it is safe in both modes
// (cf. ungated `withWasmRuntime()`, same pattern).
val stdlibJsKlib = configurations.dependencyScope("kdmStdlibJsKlib")
val stdlibJsKlibClasspath = configurations.resolvable("kdmStdlibJsKlibClasspath") {
    extendsFrom(stdlibJsKlib.get())
}
dependencies {
    stdlibJsKlib(project(":kotlin-stdlib", configuration = "distJsKlib"))
}
tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(
        objects.newInstance<SystemPropertyClasspathProvider>().apply {
            property.set("kotlin.js.stdlib.klib.path")
            classpath.from(stdlibJsKlibClasspath)
        }
    )
}
//endregion

//region Kotlin stdlib sources, needed by tests that verify documentation generated for the standard library.
val kotlinStdlibSourcesDir: File = file("$rootDir/libraries/stdlib")
tasks.withType<Test>().configureEach {
    addDirectoryProperty(kotlinStdlibSourcesDir, "kotlinStdlibSourcesDir")
}
//endregion


projectTests {
    // Test code reads these resources directly by file path (not via the classpath), so they must be
    // declared explicitly or `test-inputs-check` flags them as undeclared inputs.
    testData(project.isolated, "src/test/resources")
    testData(project.isolated, "src/testData")

    testGenerator("org.jetbrains.kotlin.documentation.analyzer.GenerateDocumentationAnalyzerTestsKt", generateTestsInBuildDirectory = true)

    // Built stdlib artifacts per platform, exposed as `kotlin.*.stdlib.path` system properties (see `stdlibFor`).
    withJvmStdlibAndReflect()
    withStdlibCommon()
    withWasmRuntime()

    // Single test task running tests with symbols java analysis (K2).
    testTask()
}
//endregion
