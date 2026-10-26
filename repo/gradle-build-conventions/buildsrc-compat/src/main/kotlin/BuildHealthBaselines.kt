/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * The buildHealth ratchet. DAWP has no baseline mechanism of its own, so an issue type promoted to
 * severity("fail") in the convention fails `buildHealth` for every module EXCEPT those explicitly pinned to
 * "warn" via the lists below. This is what allows a rule to become severity fail progressively: clean modules
 * are protected immediately, while modules that already violate the rule are grandfathered until they are fixed.
 *
 * To promote another issue type to a failing rule:
 *   1. add `on<IssueType> { severity("fail") }` to the `all` block of the convention,
 *   2. generate the corresponding baseline list from the current report (see below),
 *   3. add the per-project `severity("warn")` overrides to the convention.
 *
 * The lists are generated from the warnings in
 * `build/reports/dependency-analysis/build-health-report.txt` (baseline: 2026-11-18, master; run with
 * `-Pkotlin.native.enabled=true`). The dominant families behind the entries: IntelliJ platform jars shipping the
 * same classes as both `intellij-core-implementation` and the standalone `syntax-api` artifacts, fat published
 * artifacts consumed in-repo, and fat compiler jars on test classpaths. (The abi-comparator fat jar used to
 * embed kotlin-stdlib and org.jetbrains:annotations classes on test classpaths; that family pruned itself from
 * the lists once the jar was slimmed.)
 *
 * The TeamCity BuildHealth build runs `:buildHealth` with `-Pkotlin.native.enabled=true`, which adds
 * Kotlin/Native-only projects to the analysis and changes some native modules' classpaths, so its report contains
 * extra dirty modules: regenerate the lists from a report produced under that flag (it is a superset of the
 * default-mode report).
 *
 * Pruning rule: when a fix removes the warnings that put a module on a list, remove its entry from the list in
 * the same change so the module becomes protected by the global "fail".
 */
object BuildHealthBaselines {
    // Modules with duplicate-classes warnings at the baseline date — warning-only, do not grow the list
    val duplicateClassesBaselineProjects: Set<String> = setOf(
        ":analysis:analysis-api-fir",
        ":analysis:analysis-api-standalone",
        ":analysis:analysis-api-standalone:analysis-api-standalone-fir",
        ":analysis:analysis-test-framework",
        ":analysis:decompiled:decompiler",
        ":analysis:decompiled:light-classes-for-decompiled",
        ":analysis:light-classes-base",
        ":analysis:low-level-api-fir",
        ":analysis:low-level-api-fir:low-level-api-fir-compiler-tests",
        ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests",
        ":analysis:stubs",
        ":analysis:symbol-light-classes",
        ":analysis:test-data-manager",
        ":compiler",
        ":compiler:android-tests",
        ":compiler:backend",
        ":compiler:backend.jvm",
        ":compiler:backend.jvm.codegen",
        ":compiler:backend.jvm.entrypoint",
        ":compiler:backend.jvm.lower",
        ":compiler:cli",
        ":compiler:cli-base",
        ":compiler:cli-jklib",
        ":compiler:cli-js",
        ":compiler:cli-jvm",
        ":compiler:cli-metadata",
        ":compiler:cli:cli-arguments-generator",
        ":compiler:cli:cli-native-klib",
        ":compiler:config",
        ":compiler:config:configuration-keys-generator",
        ":compiler:fir:entrypoint",
        ":compiler:fir:fir-serialization",
        ":compiler:fir:fir2ir:jvm-backend",
        ":compiler:fir:modularized-tests",
        ":compiler:fir:raw-fir:mp-parsing2fir",
        ":compiler:fir:resolve",
        ":compiler:fir:semantics.api",
        ":compiler:frontend.common-psi:feature-to-flag-map-generator",
        ":compiler:frontend.common.jvm",
        ":compiler:incremental-compilation-impl",
        ":compiler:ir.serialization.common",
        ":compiler:ir.serialization.js",
        ":compiler:ir.serialization.native",
        ":compiler:java-direct",
        ":compiler:multiplatform-parsing",
        ":compiler:psi:psi-frontend-utils",
        ":compiler:test-infrastructure-utils",
        ":compiler:tests-common",
        ":compiler:tests-integration",
        ":compiler:util",
        ":compose-compiler-gradle-plugin",
        ":generators",
        ":gradle:kotlin-gradle-ecosystem-plugin",
        ":jps:jps-common",
        ":jps:jps-plugin",
        ":js:js.tests",
        ":kotlin-allopen-compiler-plugin",
        ":kotlin-annotation-processing",
        ":kotlin-assignment-compiler-plugin",
        ":kotlin-atomicfu-compiler-plugin",
        ":kotlin-build-common",
        ":kotlin-compiler-client-embeddable",
        ":kotlin-compiler-native-image",
        ":kotlin-compiler-runner-unshaded",
        ":kotlin-daemon-tests",
        ":kotlin-dataframe-compiler-plugin",
        ":kotlin-dataframe-compiler-plugin.backend",
        ":kotlin-dataframe-compiler-plugin.k2",
        ":kotlin-gradle-plugin",
        ":kotlin-gradle-plugin-integration-tests",
        ":kotlin-lombok-compiler-plugin",
        ":kotlin-main-kts-test",
        ":kotlin-noarg-compiler-plugin",
        ":kotlin-power-assert-compiler-plugin",
        ":kotlin-sam-with-receiver-compiler-plugin",
        ":kotlin-scripting-compiler",
        ":kotlin-scripting-compiler-impl",
        ":kotlin-scripting-jsr223-test",
        ":kotlin-scripting-jvm-host-test",
        ":kotlin-util-klib-abi",
        ":kotlin-util-klib-metadata",
        ":kotlinx-metadata-klib",
        ":kotlinx-serialization-compiler-plugin",
        ":kotlinx-serialization-compiler-plugin.k2",
        ":libraries:tools:abi-comparator",
        ":libraries:tools:analysis-api-based-klib-reader",
        ":native:base",
        ":native:cinterop.deserialization",
        ":native:frontend.native",
        ":native:kotlin-klib-commonizer",
        ":native:native.tests",
        ":native:native.tests:codegen-box",
        ":native:native.tests:driver",
        ":native:native.tests:gc-fuzzing-tests",
        ":native:native.tests:gc-fuzzing-tests:engine",
        ":native:native.tests:klib-ir-inliner",
        ":native:native.tests:litmus-tests",
        ":native:native.tests:stress",
        ":native:objcexport-header-generator-k1",
        ":native:swift:swift-export-embeddable",
        ":native:swift:swift-export-ide",
        ":native:swift:swift-export-standalone-integration-tests",
        ":native:swift:swift-export-standalone-integration-tests:coroutines",
        ":native:swift:swift-export-standalone-integration-tests:external",
        ":native:swift:swift-export-standalone-integration-tests:simple",
        ":plugins:compose-compiler-plugin:compiler-hosted",
        ":plugins:compose-compiler-plugin:compiler-hosted:integration-tests",
        ":plugins:js-plain-objects:compiler-plugin",
        ":plugins:jvm-abi-gen",
        ":plugins:parcelize:parcelize-compiler",
        ":plugins:plugin-sandbox",
        ":plugins:plugins-interactions-testing",
        ":plugins:scripting:scripting-tests",
        ":repo:codebase-tests",
        ":test-instrumenter",
        ":tools:kotlin-documentation-model:analyzer",
        ":tools:kotlinp-jvm",
        ":tools:kotlinp-klib",
    )

    // Modules that additionally have duplicate-classes warnings when Kotlin/Native is enabled — the CI BuildHealth
    // build analyzes the tree in this mode; most of these projects only exist with the flag on
    val duplicateClassesBaselineProjectsNativeEnabled: Set<String> = setOf(
        ":analysis:analysis-api-standalone:analysis-api-standalone-native",
        ":kotlin-native:Interop:StubGenerator",
        ":kotlin-native:backend.native",
        ":kotlin-native:klib",
        ":native:cli-native",
        ":native:native.tests:cli-tests",
        ":native:native.tests:klib-ir-inliner:withPlatformLibs",
        ":native:objcexport-header-generator",
    )
}
