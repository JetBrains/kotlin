/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Shared constants for project module path lists.
 *
 * These lists are used by both the root `build.gradle.kts` and convention plugins
 * (e.g., `common-configuration.gradle.kts`). Centralizing them here avoids cross-project
 * `rootProject.extra` access, which violates Gradle project isolation.
 */
object ProjectModuleLists {
    val irCompilerModules = arrayOf(
        ":compiler:ir.tree",
        ":compiler:ir.serialization.common",
        ":compiler:ir.serialization.js",
        ":compiler:ir.serialization.jvm",
        ":compiler:ir.serialization.native",
        ":compiler:ir.objcinterop",
        ":compiler:ir.backend.common",
        ":compiler:ir.backend.native",
        ":compiler:ir.actualization",
        ":compiler:ir.interpreter",
        ":compiler:ir.inline",
        ":compiler:ir.validation",
        ":wasm:wasm.ir",
        ":js:typescript-export-model",
        ":js:typescript-printer",
    )

    val irCompilerModulesForIDE = arrayOf(
        ":compiler:ir.tree",
        ":compiler:ir.serialization.common",
        ":compiler:ir.serialization.jvm",
        ":compiler:ir.serialization.js", // used in IJ android plugin in `ComposeIrGenerationExtension`
        ":compiler:ir.objcinterop",
        ":compiler:ir.backend.common",
        ":compiler:ir.backend.native",
        ":compiler:ir.actualization",
        ":compiler:ir.interpreter",
        ":compiler:ir.inline",
        ":compiler:ir.validation",
    )

    val commonCompilerModules = arrayOf(
        ":compiler:cli-base",
        ":compiler:cli",
        ":compiler:cli-jvm",
        ":compiler:cli-js",
        ":compiler:cli-metadata",
        ":compiler:psi:psi-api",
        ":compiler:psi:psi-impl",
        ":compiler:psi:psi-utils",
        ":compiler:psi:psi-frontend-utils",
        ":compiler:psi:parser",
        ":compiler:frontend.common-psi",
        ":compiler:frontend.common",
        ":compiler:util",
        ":compiler:config",
        ":compiler:config.jvm",
        ":compiler:compiler.version",
        ":compiler:arguments.common",
        ":compiler:resolution.common",
        ":compiler:resolution.common.jvm",
        ":compiler:backend.common.jvm",
        ":compiler:plugin-api",
        ":core:metadata",
        ":core:metadata.jvm",
        ":core:deserialization.common",
        ":core:deserialization.common.jvm",
        ":core:compiler.common",
        ":core:compiler.common.jvm",
        ":core:compiler.common.js",
        ":core:compiler.common.native",
        ":core:compiler.common.wasm",
        ":core:compiler.common.web",
        ":core:util.runtime",
        ":compiler:frontend.common.jvm",
        ":compiler:frontend.java", // TODO this is fe10 module but some utils used in fir ide now
        ":analysis:analysis-internal-utils",
        ":analysis:light-classes-base",
        ":analysis:decompiled:decompiler-to-stubs",
        ":analysis:decompiled:decompiler-to-file-stubs",
        ":analysis:decompiled:decompiler-js",
        ":analysis:decompiled:decompiler-native",
        ":analysis:decompiled:decompiler-to-psi",
        ":analysis:decompiled:light-classes-for-decompiled",
        ":analysis:kt-references",
        ":kotlin-build-common",
        ":kotlin-util-io",
        ":kotlin-util-klib",
        ":kotlin-util-klib-abi",
        ":native:base",
        ":native:binary-options",
        ":native:kotlin-native-utils",
        ":compiler:build-tools:kotlin-build-statistics",
        ":compiler:build-tools:kotlin-build-tools-api",
        ":js:js.config",
        ":js:js.frontend.common",
        ":wasm:wasm.config",
        ":native:native.config",
    )

    val firCompilerCoreModules = arrayOf(
        ":compiler:fir:cones",
        ":compiler:fir:providers",
        ":compiler:fir:semantics",
        ":compiler:fir:resolve",
        ":compiler:fir:fir-serialization",
        ":compiler:fir:fir-deserialization",
        ":compiler:fir:plugin-utils",
        ":compiler:fir:tree",
        ":compiler:fir:fir-jvm",
        ":compiler:fir:fir-js",
        ":compiler:fir:fir-native",
        ":compiler:fir:raw-fir:raw-fir.common",
        ":compiler:fir:raw-fir:psi2fir",
        ":compiler:fir:checkers",
        ":compiler:fir:checkers:checkers.jvm",
        ":compiler:fir:checkers:checkers.js",
        ":compiler:fir:checkers:checkers.native",
        ":compiler:fir:checkers:checkers.wasm",
        ":compiler:fir:checkers:checkers.web.common",
        ":compiler:fir:entrypoint", // TODO should not be in core modules but FIR IDE uses DependencyListForCliModule from this module
        ":compiler:fir:fir2ir:jvm-backend", // TODO should not be in core modules but FIR IDE uses Fir2IrSignatureComposer from this module
        ":compiler:fir:fir2ir", // TODO should not be in core modules but FIR IDE uses Fir2IrSignatureComposer from this module
    )

    val firAllCompilerModules: Array<String> =
        firCompilerCoreModules + arrayOf(
            ":compiler:fir:raw-fir:light-tree2fir",
            ":compiler:fir:analysis-tests",
            ":compiler:fir:analysis-tests:legacy-fir-tests",
        )

    val fe10CompilerModules = arrayOf(
        ":compiler",
        ":core:descriptors.runtime",
        ":core:descriptors",
        ":core:descriptors.jvm",
        ":compiler:light-classes",
        ":compiler:resolution",
        ":compiler:serialization",
        ":compiler:frontend",
        ":compiler:container",
        ":core:deserialization",
        ":compiler:frontend:cfg",
        ":compiler:ir.psi2ir",
        ":compiler:backend.jvm",
        ":compiler:backend.jvm.lower",
        ":compiler:backend.jvm.codegen",
        ":compiler:backend.jvm.entrypoint",
        ":compiler:backend.js",
        ":compiler:backend.wasm",
        ":kotlin-util-klib-metadata",
        ":compiler:backend",
        ":compiler:javac-wrapper",
        ":compiler:incremental-compilation-impl",
        ":js:js.ast",
        ":js:js.sourcemap",
        ":js:js.serializer",
        ":js:js.parser",
        ":js:js.frontend",
        ":js:js.translator",
        ":native:frontend.native",
        ":wasm:wasm.frontend",
        ":compiler:backend.common.jvm",
    )

    val compilerModules: Array<String> =
        irCompilerModules +
                fe10CompilerModules +
                commonCompilerModules +
                firAllCompilerModules

    /**
     * An array of projects used in the IntelliJ Kotlin Plugin.
     *
     * Experimental declarations from Kotlin stdlib cannot be used in those projects to avoid stdlib binary compatibility problems.
     * See KT-62510 for details.
     */
    val projectsUsedInIntelliJKotlinPlugin: Array<String> =
        fe10CompilerModules +
                commonCompilerModules +
                firCompilerCoreModules +
                irCompilerModulesForIDE +
                arrayOf(
                    ":analysis:analysis-api",
                    ":analysis:analysis-api-fe10",
                    ":analysis:analysis-api-fir",
                    ":analysis:analysis-api-impl-base",
                    ":analysis:analysis-api-platform-interface",
                    ":analysis:analysis-api-standalone:analysis-api-standalone-base",
                    ":analysis:analysis-api-standalone:analysis-api-fir-standalone-base",
                    ":analysis:analysis-api-standalone",
                    ":analysis:analysis-test-framework",
                    ":analysis:decompiled",
                    ":analysis:kt-references",
                    ":analysis:light-classes-base",
                    ":analysis:low-level-api-fir",
                    ":analysis:stubs",
                    ":analysis:symbol-light-classes",
                ) +
                arrayOf(
                    ":kotlin-allopen-compiler-plugin.cli",
                    ":kotlin-allopen-compiler-plugin.common",
                    ":kotlin-allopen-compiler-plugin.k1",
                    ":kotlin-allopen-compiler-plugin.k2",

                    ":kotlin-assignment-compiler-plugin.cli",
                    ":kotlin-assignment-compiler-plugin.common",
                    ":kotlin-assignment-compiler-plugin.k1",
                    ":kotlin-assignment-compiler-plugin.k2",

                    ":plugins:parcelize:parcelize-compiler:parcelize.backend",
                    ":plugins:parcelize:parcelize-compiler:parcelize.cli",
                    ":plugins:parcelize:parcelize-compiler:parcelize.common",
                    ":plugins:parcelize:parcelize-compiler:parcelize.k1",
                    ":plugins:parcelize:parcelize-compiler:parcelize.k2",
                    ":plugins:parcelize:parcelize-runtime",

                    ":plugins:compose-compiler-plugin:compiler-hosted",

                    ":kotlin-sam-with-receiver-compiler-plugin.cli",
                    ":kotlin-sam-with-receiver-compiler-plugin.common",
                    ":kotlin-sam-with-receiver-compiler-plugin.k1",
                    ":kotlin-sam-with-receiver-compiler-plugin.k2",

                    ":kotlinx-serialization-compiler-plugin.cli",
                    ":kotlinx-serialization-compiler-plugin.common",
                    ":kotlinx-serialization-compiler-plugin.k1",
                    ":kotlinx-serialization-compiler-plugin.k2",
                    ":kotlinx-serialization-compiler-plugin.backend",

                    ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli",
                    ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.common",
                    ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.k2",
                    ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.backend",

                    ":kotlin-lombok-compiler-plugin.cli",
                    ":kotlin-lombok-compiler-plugin.common",
                    ":kotlin-lombok-compiler-plugin.k1",
                    ":kotlin-lombok-compiler-plugin.k2",

                    ":kotlin-noarg-compiler-plugin.cli",
                    ":kotlin-noarg-compiler-plugin.common",
                    ":kotlin-noarg-compiler-plugin.k1",
                    ":kotlin-noarg-compiler-plugin.k2",
                    ":kotlin-noarg-compiler-plugin.backend",

                    ":kotlin-sam-with-receiver-compiler-plugin.cli",
                    ":kotlin-sam-with-receiver-compiler-plugin.common",
                    ":kotlin-sam-with-receiver-compiler-plugin.k1",
                    ":kotlin-sam-with-receiver-compiler-plugin.k2",

                    ":kotlin-dataframe-compiler-plugin.cli",
                    ":kotlin-dataframe-compiler-plugin.common",
                    ":kotlin-dataframe-compiler-plugin.k2",
                    ":kotlin-dataframe-compiler-plugin.backend",

                    ":kotlin-compiler-runner-unshaded",
                    ":kotlin-preloader",
                    ":daemon-common",
                    ":kotlin-daemon-client",

                    ":kotlin-scripting-jvm",

                    ":kotlin-scripting-compiler",
                    ":kotlin-gradle-statistics",
                    ":jps:jps-common",
                ) +
                arrayOf(
                    ":compiler:ir.serialization.native",
                    ":libraries:tools:analysis-api-based-klib-reader",
                    ":native:base",
                    ":native:objcexport-header-generator",
                    ":native:objcexport-header-generator-analysis-api",
                    ":native:objcexport-header-generator-k1",
                    ":native:analysis-api-based-export-common",
                ) +
                arrayOf(
                    ":native:swift:sir",
                    ":native:swift:sir-light-classes",
                    ":native:swift:sir-printer",
                    ":native:swift:sir-providers",
                    ":native:swift:swift-export-ide",
                ) +
                arrayOf(
                    ":analysis:analysis-tools:deprecated-k1-frontend-internals-for-ide-generated",
                )

    val mppProjects = listOf(
        ":kotlin-stdlib",
        ":kotlin-test",
    )

    val projectsWithOptInToUnsafeCastFunctionsFromAddToStdLib = listOf(
        ":analysis:analysis-api-fe10",
        ":analysis:analysis-api-fir",
        ":analysis:decompiled:light-classes-for-decompiled",
        ":analysis:symbol-light-classes",
        ":compiler",
        ":compiler:backend.js",
        ":compiler:light-classes",
        ":jps:jps-common",
        ":js:js.tests",
        ":kotlin-build-common",
        ":kotlin-gradle-plugin",
        ":kotlin-scripting-jvm-host-test",
        ":native:kotlin-klib-commonizer",
    )
}