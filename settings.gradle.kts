import java.io.File
import java.util.Properties

pluginManagement {
    includeBuild("repo/gradle-settings-conventions")
    includeBuild("repo/gradle-build-conventions")

    repositories {
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
            content {
                includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps(\\..+)?")
                includeGroupByRegex("com.intellij.platform.*")
                includeGroupByRegex("org.jetbrains.jps.*")
            }
        }
        maven {
            url = uri("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
            content {
                includeGroup("org.jetbrains.kotlin")
                includeGroup("org.spdx")
                includeGroup("org.jetbrains.kotlinx")
                includeGroup("org.jetbrains.kotlinx.benchmark")
            }
        }
        google {
            url = uri("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2")
            content {
                includeGroup("com.android.tools")
            }
        }
        mavenCentral {
            url = uri("https://cache-redirector.jetbrains.com/maven-central")
        }
        gradlePluginPortal()
    }

    plugins {
        id("de.undercouch.download") version "5.1.0"
    }
}

plugins {
    id("internal-gradle-setup") // it's recommended to apply this plugin at first, as it changes the local.properties file
    id("kotlin-bootstrap")
    id("develocity")
    id("jvm-toolchain-provisioning")
    id("kotlin-daemon-config")
    id("cache-redirector")
}

val versionPropertiesFile = File(rootProject.projectDir, "gradle/versions.properties")
val versionProperties = Properties()
versionPropertiesFile.inputStream().use {
    versionProperties.load(it)
}
dependencyResolutionManagement {
    components {
        withModule("com.google.code.gson:gson") {
            allVariants {
                withDependencies {
                    add("com.google.code.gson:gson") {
                        version {
                            require(versionProperties["versions.gson"] as String)
                        }
                    }
                }
            }
        }

        withModule("org.apache.commons:commons-compress") {
            allVariants {
                withDependencies {
                    add("org.apache.commons:commons-compress") {
                        version {
                            require(versionProperties["versions.commons-compress"] as String)
                        }
                    }
                }
            }
        }

        withModule("commons-io:commons-io") {
            allVariants {
                withDependencies {
                    add("commons-io:commons-io") {
                        version {
                            require(versionProperties["versions.commons-io"] as String)
                        }
                    }
                }
            }
        }
    }

    versionCatalogs {
        create("composeRuntimeSnapshot") {
            from(files("plugins/compose/compose-runtime-snapshot-versions.toml"))
        }
    }
}

val buildProperties = getKotlinBuildPropertiesForSettings(settings)

// modules
include(
    ":benchmarks",
    ":compiler",
    ":compiler:util",
    ":compiler:config",
    ":compiler:config.jvm",
    ":compiler:config:configuration-keys-generator",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-metadata",
    ":kotlin-util-klib-abi",
    ":daemon-common",
    ":kotlin-daemon",
    ":kotlin-daemon-client",
    ":kotlin-daemon-tests",
    ":kotlin-preloader",
    ":kotlin-runner",
    ":compiler:arguments.common",
    ":compiler:arguments",
    ":compiler:container",
    ":compiler:resolution.common",
    ":compiler:resolution.common.jvm",
    ":compiler:resolution",
    ":compiler:serialization",
    ":compiler:psi:psi-api",
    ":compiler:psi:psi-impl",
    ":compiler:psi:psi-utils",
    ":compiler:psi:psi-frontend-utils",
    ":compiler:psi:parser",
    ":compiler:multiplatform-parsing",
    ":compiler:frontend",
    ":compiler:frontend.common",
    ":compiler:frontend.common-psi",
    ":compiler:frontend.java",
    ":compiler:frontend:cfg",
    ":kotlin-compiler-runner-unshaded",
    ":kotlin-compiler-runner",
    ":compiler:ir.tree",
    ":compiler:ir.tree:tree-generator",
    ":compiler:ir.psi2ir",
    ":compiler:ir.objcinterop",
    ":compiler:ir.backend.common",
    ":compiler:ir.backend.native",
    ":compiler:ir.actualization",
    ":compiler:ir.serialization.common",
    ":compiler:ir.serialization.jvm",
    ":compiler:ir.serialization.js",
    ":compiler:ir.serialization.native",
    ":compiler:ir.serialization.jklib",
    ":compiler:ir.interpreter",
    ":compiler:ir.inline",
    ":compiler:ir.validation",
    ":compiler:backend.js",
    ":compiler:backend.wasm",
    ":compiler:backend.jvm",
    ":compiler:backend.jvm.lower",
    ":compiler:backend.jvm.codegen",
    ":compiler:backend.jvm.entrypoint",
    ":compiler:backend",
    ":compiler:plugin-api",
    ":compiler:light-classes",
    ":compiler:javac-wrapper",
    ":compiler:cli:cli-arguments-generator",
    ":compiler:cli-base",
    ":compiler:cli",
    ":compiler:cli-jvm",
    ":compiler:cli-jvm:javac-integration",
    ":compiler:cli-js",
    ":compiler:cli-jklib",
    ":compiler:cli-metadata",
    ":compiler:cli:cli-native-klib",
    ":compiler:incremental-compilation-impl",
    ":compiler:tests-compiler-utils",
    ":compiler:tests-common",
    ":compiler:tests-integration",
    ":compiler:tests-mutes",
    ":compiler:tests-mutes:mutes-junit4",
    ":compiler:tests-mutes:mutes-junit5",
    ":compiler:tests-against-klib",
    ":compiler:jklib.tests",
    ":js:js.ast",
    ":js:js.sourcemap",
    ":js:js.serializer",
    ":js:js.parser",
    ":js:js.config",
    ":js:js.frontend.common",
    ":js:js.frontend",
    ":js:js.translator",
    ":js:js.tests",
    ":js:js.tests:klib-compatibility",
    ":native:unsafe-mem",
    ":native:kotlin-native-utils",
    ":native:frontend.native",
    ":native:kotlin-klib-commonizer",
    ":native:kotlin-klib-commonizer-api",
    ":native:kotlin-klib-commonizer-embeddable",
    ":native:executors",
    ":native:base",
    ":native:native.config",
    ":native:binary-options",
    ":native:analysis-api-based-test-utils",
    ":native:analysis-api-based-export-common",
    ":native:external-projects-test-utils",
    ":native:objcexport-header-generator",
    ":native:objcexport-header-generator-k1",
    ":native:objcexport-header-generator-analysis-api",
    ":native:external-projects-test-utils:testLibraryA",
    ":native:external-projects-test-utils:testLibraryB",
    ":native:external-projects-test-utils:testLibraryC",
    ":native:external-projects-test-utils:testInternalLibrary",
    ":native:external-projects-test-utils:testExtensionsLibrary",
    ":core:names",
    ":core:language.model",
    ":core:language.targets",
    ":core:language.targets.jvm",
    ":core:language.version-settings",
    ":core:compiler.common",
    ":core:compiler.common.jvm",
    ":core:compiler.common.js",
    ":core:compiler.common.native",
    ":core:compiler.common.wasm",
    ":core:compiler.common.web",
    ":compiler:backend.common.jvm",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":core:deserialization",
    ":core:descriptors.runtime",
    ":core:metadata",
    ":core:metadata.jvm",
    ":core:deserialization.common",
    ":core:deserialization.common.jvm",
    ":compiler:frontend.common.jvm",
    ":core:util.runtime",
    ":compiler:compiler.version",
    ":dependencies:android-sdk",
    ":dependencies:tools-jar-api",
    ":dependencies:intellij-core",
    ":dependencies:bootstrap:kotlin-stdlib-bootstrap",
    ":dependencies:bootstrap:kotlin-build-tools-api-bootstrap",
    ":dependencies:bootstrap:kotlin-build-tools-impl-bootstrap",
    ":dependencies:bootstrap:kotlin-build-tools-compat-bootstrap",
    ":dependencies:bootstrap:kotlin-build-tools-cri-impl-bootstrap",
    ":dependencies:bootstrap:kotlin-script-runtime-bootstrap",
    ":dependencies:bootstrap:kotlin-scripting-common-bootstrap",
    ":dependencies:bootstrap:kotlin-scripting-jvm-bootstrap",
    ":dependencies:bootstrap:kotlin-daemon-client-bootstrap",
    ":dependencies:bootstrap:kotlin-reflect-bootstrap",
    ":dependencies:bootstrap:kotlin-tooling-core-bootstrap",
    ":dependencies:bootstrap:kotlin-compiler-embeddable-bootstrap",
    ":dependencies:bootstrap:kotlin-compiler-runner-bootstrap"
)

include(
    ":kotlin-allopen-compiler-plugin",
    ":kotlin-allopen-compiler-plugin.embeddable",
    ":kotlin-allopen-compiler-plugin.common",
    ":kotlin-allopen-compiler-plugin.k1",
    ":kotlin-allopen-compiler-plugin.k2",
    ":kotlin-allopen-compiler-plugin.cli"
)

include(
    ":kotlin-noarg-compiler-plugin",
    ":kotlin-noarg-compiler-plugin.embeddable",
    ":kotlin-noarg-compiler-plugin.common",
    ":kotlin-noarg-compiler-plugin.k1",
    ":kotlin-noarg-compiler-plugin.k2",
    ":kotlin-noarg-compiler-plugin.backend",
    ":kotlin-noarg-compiler-plugin.cli"
)

include(
    ":kotlin-power-assert-compiler-plugin",
    ":kotlin-power-assert-compiler-plugin.embeddable",
    ":kotlin-power-assert-compiler-plugin.common",
    ":kotlin-power-assert-compiler-plugin.frontend",
    ":kotlin-power-assert-compiler-plugin.backend",
    ":kotlin-power-assert-compiler-plugin.cli",
    ":kotlin-power-assert-runtime"
)

include(
    ":kotlin-sam-with-receiver-compiler-plugin",
    ":kotlin-sam-with-receiver-compiler-plugin.embeddable",
    ":kotlin-sam-with-receiver-compiler-plugin.common",
    ":kotlin-sam-with-receiver-compiler-plugin.k1",
    ":kotlin-sam-with-receiver-compiler-plugin.k2",
    ":kotlin-sam-with-receiver-compiler-plugin.cli"
)

include(
    ":kotlin-assignment-compiler-plugin",
    ":kotlin-assignment-compiler-plugin.common",
    ":kotlin-assignment-compiler-plugin.k1",
    ":kotlin-assignment-compiler-plugin.k2",
    ":kotlin-assignment-compiler-plugin.cli",
    ":kotlin-assignment-compiler-plugin.embeddable"
)

include(
    ":plugins:test-plugins:before",
    ":plugins:test-plugins:middle",
    ":plugins:test-plugins:after",
    ":plugins:plugins-interactions-testing"
)

include(":gradle:generators:native-cache-kotlin-version")
project(":gradle:generators:native-cache-kotlin-version").projectDir =
    File("$rootDir/libraries/tools/gradle/generators/native-cache-kotlin-version")
project(":gradle:generators").projectDir = File("$rootDir/libraries/tools/gradle/generators")

include(
    ":kotlin-script-runtime",
    ":plugins:plugin-sandbox",
    ":plugins:plugin-sandbox:plugin-annotations",
    ":plugins:plugin-sandbox:plugin-sandbox-ic-test",
    ":kotlin-metadata",
    ":kotlin-metadata-jvm",
    ":kotlinx-metadata-klib",
    ":prepare:build.version",
    ":kotlin-build-common",
    ":prepare:kotlin-compiler-internal-test-framework",
    ":kotlin-jklib-compiler",
    ":kotlin-compiler",
    ":kotlin-compiler-embeddable",
    ":kotlin-compiler-client-embeddable",
    ":kotlin-klib-abi-reader",
    ":kotlin-reflect",
    ":compiler:tests-java8",
    ":compiler:tests-different-jdk",
    ":compiler:tests-spec",
    ":generators",
    ":generators:ide-iml-to-gradle-generator",
    ":generators:test-generator",
    ":generators:tree-generator-common",
    ":tools:kotlinp",
    ":tools:kotlinp-jvm",
    ":tools:kotlinp-klib",
    ":tools:stats-analyser",
    ":kotlin-build-tools-enum-compat",
    ":kotlin-gradle-compiler-types",
    ":kotlin-gradle-plugin-api",
    ":kotlin-gradle-plugin-annotations",
    ":kotlin-gradle-plugin-idea",
    ":kotlin-gradle-plugin-idea-proto",
    ":kotlin-gradle-plugin-idea-for-compatibility-tests",
    ":analysis:analysis-tools:deprecated-k1-frontend-internals-for-ide-generator",
    ":analysis:analysis-tools:deprecated-k1-frontend-internals-for-ide-generated",
    ":kotlin-gradle-plugin-dsl-codegen",
    ":kotlin-gradle-statistics",
    ":kotlin-gradle-build-metrics",
    ":kotlin-gradle-plugin",
    ":gradle:kotlin-gradle-ecosystem-plugin",
    ":kotlin-gradle-plugin-tcs-android",
    ":kotlin-gradle-plugin-test-utils-embeddable",
    ":kotlin-gradle-plugin-integration-tests",
    ":kotlin-gradle-plugins-bom",
    ":kotlin-privacy-manifests-plugin",
    ":compiler:build-tools:kotlin-build-statistics",
    ":gradle:android-test-fixes",
    ":gradle:documentation",
    ":gradle:gradle-warnings-detector",
    ":gradle:kotlin-compiler-args-properties",
    ":gradle:regression-benchmark-templates",
    ":gradle:regression-benchmarks",
    ":libraries:tools:gradle:fus-statistics-gradle-plugin",
    ":kotlin-tooling-metadata",
    ":kotlin-tooling-core",
    ":compose-compiler-gradle-plugin",
    ":kotlin-allopen",
    ":kotlin-noarg",
    ":kotlin-power-assert",
    ":kotlin-sam-with-receiver",
    ":kotlin-assignment",
    ":kotlin-gradle-subplugin-example",
    ":examples:annotation-processor-example",
    ":kotlin-annotation-processing",
    ":kotlin-annotation-processing-cli",
    ":kotlin-annotation-processing-base",
    ":kotlin-annotation-processing-runtime",
    ":kotlin-annotation-processing-embeddable",
    ":kotlin-daemon-embeddable",
    ":kotlin-annotations-jvm",
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-jvm-host-unshaded",
    ":kotlin-scripting-jvm-host-test",
    ":kotlin-scripting-jvm-host",
    ":kotlin-scripting-intellij",
    ":kotlin-scripting-compiler",
    ":kotlin-scripting-compiler-embeddable",
    ":kotlin-scripting-compiler-impl",
    ":kotlin-scripting-compiler-impl-embeddable",
    ":plugins:scripting:test-script-definition",
    ":plugins:scripting:scripting-tests",
    ":kotlin-scripting-dependencies",
    ":kotlin-scripting-dependencies-maven",
    ":kotlin-scripting-dependencies-maven-all",
    ":kotlin-scripting-jsr223-unshaded",
    ":kotlin-scripting-jsr223-test",
    ":kotlin-scripting-jsr223",
    ":kotlin-main-kts",
    ":kotlin-main-kts-test",
    ":examples:scripting-jvm-simple-script",
    ":examples:scripting-jvm-simple-script-host",
    ":examples:scripting-jvm-maven-deps",
    ":examples:scripting-jvm-maven-deps-host",
    ":examples:scripting-jvm-embeddable-host",
    ":libraries:kotlin-prepush-hook",
    ":libraries:tools:mutability-annotations-compat",
    ":plugins:jvm-abi-gen",
    ":plugins:jvm-abi-gen-embeddable",
    ":test-instrumenter",
    ":wasm:wasm.ir",
    ":wasm:wasm.tests",
    ":wasm:wasm.tests:klib-compatibility",
    ":wasm:wasm.frontend",
    ":wasm:wasm.config",
    ":wasm:wasm.debug.browsers",
    ":repo:test-federation-runtime",
    ":repo:codebase-tests",
    ":repo:artifacts-tests"
)

include(":libraries:tools:abi-comparator")

include(
    ":libraries:tools:abi-validation:abi-tools",
    ":libraries:tools:abi-validation:abi-tools-api",
    ":libraries:tools:abi-validation:abi-tools-embeddable",
    ":libraries:tools:abi-validation:abi-tools-tests",
    ":libraries:tools:abi-validation:kgp-integration-tests"
)

include(
    ":libraries:tools:analysis-api-based-klib-reader",
    ":libraries:tools:analysis-api-based-klib-reader:testProject"
)

include(
    ":kotlin-atomicfu-compiler-plugin",
    ":kotlin-atomicfu-compiler-plugin-embeddable",
    ":kotlinx-atomicfu-runtime",
    ":atomicfu"
)

include(
    ":plugins:js-plain-objects:compiler-plugin",
    ":plugins:js-plain-objects:compiler-plugin-embeddable",
    ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.common",
    ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.k2",
    ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.backend",
    ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli"
)

include(
    ":plugins:js-plain-objects:runtime",
    ":libraries:tools:js-plain-objects"
)

include(
    ":compiler:fir",
    ":compiler:fir:cones",
    ":compiler:fir:tree",
    ":compiler:fir:tree:tree-generator",
    ":compiler:fir:raw-fir:raw-fir.common",
    ":compiler:fir:raw-fir:psi2fir",
    ":compiler:fir:raw-fir:light-tree2fir",
    ":compiler:fir:fir2ir",
    ":compiler:fir:fir2ir:jvm-backend",
    ":compiler:fir:providers",
    ":compiler:fir:semantics",
    ":compiler:fir:resolve",
    ":compiler:fir:plugin-utils",
    ":compiler:fir:fir-serialization",
    ":compiler:fir:fir-deserialization",
    ":compiler:fir:fir-jvm",
    ":compiler:fir:fir-js",
    ":compiler:fir:fir-native",
    ":compiler:fir:modularized-tests",
    ":compiler:fir:dump",
    ":compiler:fir:checkers",
    ":compiler:fir:checkers:checkers.jvm",
    ":compiler:fir:checkers:checkers.js",
    ":compiler:fir:checkers:checkers.native",
    ":compiler:fir:checkers:checkers.wasm",
    ":compiler:fir:checkers:checkers.web.common",
    ":compiler:fir:checkers:checkers-component-generator",
    ":compiler:fir:diagnostic-renderers",
    ":compiler:fir:entrypoint",
    ":compiler:fir:analysis-tests",
    ":compiler:fir:analysis-tests:legacy-fir-tests"
)

include(
    ":kotlin-scripting-ide-services-unshaded",
    ":kotlin-scripting-ide-services-test",
    ":kotlin-scripting-ide-services",
    ":kotlin-scripting-ide-common"
)

include(
    ":compiler:test-infrastructure",
    ":compiler:test-infrastructure:grouping-test-engine",
    ":compiler:test-infrastructure-utils",
    ":compiler:test-infrastructure-utils.common",
    ":compiler:tests-common-new",
)

include(
    ":plugins:parcelize:parcelize-compiler",
    ":plugins:parcelize:parcelize-compiler:parcelize.common",
    ":plugins:parcelize:parcelize-compiler:parcelize.k1",
    ":plugins:parcelize:parcelize-compiler:parcelize.k2",
    ":plugins:parcelize:parcelize-compiler:parcelize.backend",
    ":plugins:parcelize:parcelize-compiler:parcelize.cli",
    ":plugins:parcelize:parcelize-runtime",
    ":kotlin-parcelize-compiler"
)

include(
    ":kotlin-lombok-compiler-plugin",
    ":kotlin-lombok-compiler-plugin.embeddable",
    ":kotlin-lombok-compiler-plugin.common",
    ":kotlin-lombok-compiler-plugin.k1",
    ":kotlin-lombok-compiler-plugin.k2",
    ":kotlin-lombok-compiler-plugin.cli",
    ":kotlin-lombok"
)

include(
    ":kotlinx-serialization-compiler-plugin",
    ":kotlinx-serialization-compiler-plugin.embeddable",
    ":kotlinx-serialization-compiler-plugin.common",
    ":kotlinx-serialization-compiler-plugin.k1",
    ":kotlinx-serialization-compiler-plugin.k2",
    ":kotlinx-serialization-compiler-plugin.backend",
    ":kotlinx-serialization-compiler-plugin.cli",
    ":kotlin-serialization",
    ":kotlin-serialization-unshaded"
)

include(
    ":kotlin-dataframe-compiler-plugin",
    ":kotlin-dataframe-compiler-plugin.embeddable",
    ":kotlin-dataframe-compiler-plugin.common",
    ":kotlin-dataframe-compiler-plugin.k2",
    ":kotlin-dataframe-compiler-plugin.backend",
    ":kotlin-dataframe-compiler-plugin.cli",
    ":kotlin-dataframe"
)

include(
    ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:scripting-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-tests-for-ide",
    ":prepare:ide-plugin-dependencies:compose-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:js-plain-objects-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:incremental-compilation-impl-tests-for-ide",
    ":prepare:ide-plugin-dependencies:js-ir-runtime-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-build-common-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-cli-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-dist-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-gradle-statistics-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-common-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-classpath",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-testdata-for-ide",
    ":prepare:ide-plugin-dependencies:kotlinx-serialization-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-dataframe-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:noarg-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:sam-with-receiver-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:assignment-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:parcelize-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:lombok-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-objcexport-header-generator-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-swift-export-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-tests-for-ide",
    ":prepare:ide-plugin-dependencies:low-level-api-fir-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-impl-base-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-k2-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-fe10-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-platform-interface-for-ide",
    ":prepare:ide-plugin-dependencies:symbol-light-classes-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-standalone-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-ir-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-common-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-fe10-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-fir-for-ide"
)

include(
    ":compiler:build-tools:kotlin-build-tools-api",
    ":compiler:build-tools:kotlin-build-tools-impl",
    ":compiler:build-tools:kotlin-build-tools-compat",
    ":compiler:build-tools:kotlin-build-tools-api-tests",
    ":compiler:build-tools:kotlin-build-tools-api-forward-compatibility-tests",
    ":compiler:build-tools:kotlin-build-tools-jdk-utils",
    ":compiler:build-tools:kotlin-build-tools-generator",
    ":compiler:build-tools:util-kotlinpoet",
    ":compiler:build-tools:kotlin-build-tools-cri-impl"
)

include(
    ":plugins:compose-compiler-plugin",
    ":plugins:compose-compiler-plugin:compiler",
    ":plugins:compose-compiler-plugin:compiler-hosted",
    ":plugins:compose-compiler-plugin:compiler-hosted:integration-tests",
    ":plugins:compose-compiler-plugin:compiler-hosted:integration-tests:protobuf-test-classes",
    ":plugins:compose-compiler-plugin:group-mapping"
)

if (buildProperties.isInIdeaSync.get()) {
    include(":plugins:compose-compiler-plugin:compiler-hosted:runtime-tests")
}

// Swift Export modules
include(
    ":native:swift:sir",
    ":native:swift:sir:tree-generator",
    ":native:swift:sir-light-classes",
    ":native:swift:sir-printer",
    ":native:swift:sir-providers",
    ":native:swift:swift-export-standalone",
    ":native:swift:swift-export-ide",
    ":native:swift:swift-export-standalone-integration-tests",
    ":native:swift:swift-export-standalone-integration-tests:simple",
    ":native:swift:swift-export-standalone-integration-tests:external",
    ":native:swift:swift-export-standalone-integration-tests:coroutines",
    ":generators:sir-tests-generator"
)

include(":native:swift:swift-export-embeddable")

// TypeScript Export modules
include(
    ":js:typescript-export-model",
    ":js:typescript-printer",
    ":js:typescript-export-standalone"
)

include(
    ":jps:jps-common",
    ":jps:jps-plugin",
    ":prepare:kotlin-jps-plugin",
    ":jps:jps-platform-api-signatures"
)

include(
    ":analysis",
    ":analysis:low-level-api-fir",
    ":analysis:low-level-api-fir:tests-jdk11",
    ":analysis:low-level-api-fir:low-level-api-fir-compiler-tests",
    ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests",
    ":analysis:analysis-api-fir:analysis-api-fir-generator",
    ":analysis:analysis-api-fir",
    ":analysis:analysis-api",
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-platform-interface",
    ":analysis:analysis-internal-utils",
    ":analysis:analysis-test-framework",
    ":analysis:test-data-manager",
    ":analysis:kt-references",
    ":analysis:stubs",
    ":analysis:symbol-light-classes",
    ":analysis:light-classes-base",
    ":analysis:analysis-api-standalone",
    ":analysis:analysis-api-standalone:analysis-api-standalone-base",
    ":analysis:analysis-api-standalone:analysis-api-fir-standalone-base",
    ":analysis:analysis-api-fe10",
    ":analysis:decompiled:decompiler-to-psi",
    ":analysis:decompiled:decompiler-to-stubs",
    ":analysis:decompiled:decompiler-to-file-stubs",
    ":analysis:decompiled:decompiler-js",
    ":analysis:decompiled:decompiler-native",
    ":analysis:decompiled:light-classes-for-decompiled",
    ":prepare:analysis-api-test-framework",
    ":tools:ide-plugin-dependencies-validator"
)

if (buildProperties.isKotlinNativeEnabled.get()) {
    include(":analysis:analysis-api-standalone:analysis-api-standalone-native")
}


// modules that we are currently cannot compile with jps
include(
    ":kotlin-stdlib-common",
    ":kotlin-stdlib",
    ":kotlin-dom-api-compat",
    ":kotlin-stdlib-js-ir-minimal-for-test",
    ":kotlin-stdlib-jdk7",
    ":kotlin-stdlib-jdk8",
    ":kotlin-stdlib:samples",
    ":kotlin-stdlib-jvm-minimal-for-test",
    ":kotlin-stdlib-jklib-for-test",
    ":tools:binary-compatibility-validator",
    ":tools:jdk-api-validator",
    ":tools:kotlin-stdlib-gen",

    ":kotlin-test",
    ":kotlin-test:kotlin-test-js-it",
    ":native:native.tests",
    ":native:native.tests:codegen-box",
    ":native:native.tests:driver",
    ":native:native.tests:gc-fuzzing-tests",
    ":native:native.tests:gc-fuzzing-tests:engine",
    ":native:native.tests:stress",
    ":native:native.tests:klib-ir-inliner",
    ":native:native.tests:klib-compatibility",
    ":native:native.tests:litmus-tests"
)

project(":kotlin-stdlib-common").projectDir = File("$rootDir/libraries/stdlib/common")
project(":kotlin-stdlib").projectDir = File("$rootDir/libraries/stdlib")
project(":kotlin-dom-api-compat").projectDir = File("$rootDir/libraries/kotlin-dom-api-compat")
project(":kotlin-stdlib-js-ir-minimal-for-test").projectDir = File("$rootDir/libraries/stdlib/js-ir-minimal-for-test")
project(":kotlin-stdlib-jdk7").projectDir = File("$rootDir/libraries/stdlib/jdk7")
project(":kotlin-stdlib-jdk8").projectDir = File("$rootDir/libraries/stdlib/jdk8")
project(":kotlin-stdlib:samples").projectDir = File("$rootDir/libraries/stdlib/samples")
project(":kotlin-stdlib-jvm-minimal-for-test").projectDir = File("$rootDir/libraries/stdlib/jvm-minimal-for-test")
project(":kotlin-stdlib-jklib-for-test").projectDir = File("$rootDir/libraries/stdlib/jklib-for-test")

project(":tools").projectDir = File("$rootDir/libraries/tools")
project(":tools:binary-compatibility-validator").projectDir = File("$rootDir/libraries/tools/binary-compatibility-validator")
project(":tools:jdk-api-validator").projectDir = File("$rootDir/libraries/tools/jdk-api-validator")
project(":tools:kotlin-stdlib-gen").projectDir = File("$rootDir/libraries/tools/kotlin-stdlib-gen")
project(":tools:ide-plugin-dependencies-validator").projectDir = File("$rootDir/libraries/tools/ide-plugin-dependencies-validator")

project(":kotlin-test").projectDir = File("$rootDir/libraries/kotlin.test")
project(":kotlin-test:kotlin-test-js-it").projectDir = File("$rootDir/libraries/kotlin.test/js/it")
include(":compiler:android-tests")

rootProject.name = "kotlin"

project(":kotlin-script-runtime").projectDir = File("$rootDir/libraries/tools/script-runtime")
project(":kotlin-reflect").projectDir = File("$rootDir/libraries/reflect")
project(":kotlin-metadata").projectDir = File("$rootDir/libraries/kotlinx-metadata")
project(":kotlin-metadata-jvm").projectDir = File("$rootDir/libraries/kotlinx-metadata/jvm")
project(":kotlinx-metadata-klib").projectDir = File("$rootDir/libraries/kotlinx-metadata/klib")
project(":kotlin-compiler").projectDir = File("$rootDir/prepare/compiler")
project(":kotlin-jklib-compiler").projectDir = File("$rootDir/prepare/jklib-compiler")
project(":kotlin-compiler-embeddable").projectDir = File("$rootDir/prepare/compiler-embeddable")
project(":kotlin-compiler-client-embeddable").projectDir = File("$rootDir/prepare/compiler-client-embeddable")
project(":kotlin-klib-abi-reader").projectDir = File("$rootDir/libraries/tools/klib-abi-reader")
project(":kotlin-preloader").projectDir = File("$rootDir/compiler/preloader")
project(":kotlin-build-common").projectDir = File("$rootDir/build-common")
project(":compiler:cli-base").projectDir = File("$rootDir/compiler/cli/cli-base")
project(":compiler:cli-jvm").projectDir = File("$rootDir/compiler/cli/cli-jvm")
project(":compiler:cli-jvm:javac-integration").projectDir = File("$rootDir/compiler/cli/cli-jvm/javac-integration")
project(":compiler:cli-js").projectDir = File("$rootDir/compiler/cli/cli-js")
project(":compiler:cli-metadata").projectDir = File("$rootDir/compiler/cli/cli-metadata")
project(":compiler:cli-jklib").projectDir = File("$rootDir/compiler/cli/cli-jklib")
project(":kotlin-runner").projectDir = File("$rootDir/compiler/cli/cli-runner")
project(":kotlin-daemon").projectDir = File("$rootDir/compiler/daemon")
project(":daemon-common").projectDir = File("$rootDir/compiler/daemon/daemon-common")
project(":kotlin-daemon-client").projectDir = File("$rootDir/compiler/daemon/daemon-client")
project(":kotlin-daemon-tests").projectDir = File("$rootDir/compiler/daemon/daemon-tests")
project(":kotlin-compiler-runner-unshaded").projectDir = File("$rootDir/compiler/compiler-runner-unshaded")
project(":kotlin-compiler-runner").projectDir = File("$rootDir/compiler/compiler-runner")
project(":compiler:ir.tree").projectDir = File("$rootDir/compiler/ir/ir.tree")
project(":compiler:ir.tree:tree-generator").projectDir = File("$rootDir/compiler/ir/ir.tree/tree-generator")
project(":compiler:ir.psi2ir").projectDir = File("$rootDir/compiler/ir/ir.psi2ir")
project(":compiler:ir.objcinterop").projectDir = File("$rootDir/compiler/ir/ir.objcinterop")
project(":compiler:ir.backend.common").projectDir = File("$rootDir/compiler/ir/backend.common")
project(":compiler:ir.backend.native").projectDir = File("$rootDir/compiler/ir/backend.native")
project(":compiler:ir.actualization").projectDir = File("$rootDir/compiler/ir/ir.actualization")
project(":compiler:ir.inline").projectDir = File("$rootDir/compiler/ir/ir.inline")
project(":compiler:ir.validation").projectDir = File("$rootDir/compiler/ir/ir.validation")
project(":compiler:backend.js").projectDir = File("$rootDir/compiler/ir/backend.js")
project(":compiler:backend.wasm").projectDir = File("$rootDir/compiler/ir/backend.wasm")
project(":compiler:backend.jvm").projectDir = File("$rootDir/compiler/ir/backend.jvm")
project(":compiler:backend.jvm.lower").projectDir = File("$rootDir/compiler/ir/backend.jvm/lower")
project(":compiler:backend.jvm.codegen").projectDir = File("$rootDir/compiler/ir/backend.jvm/codegen")
project(":compiler:backend.jvm.entrypoint").projectDir = File("$rootDir/compiler/ir/backend.jvm/entrypoint")
project(":compiler:ir.serialization.common").projectDir = File("$rootDir/compiler/ir/serialization.common")
project(":compiler:ir.serialization.jvm").projectDir = File("$rootDir/compiler/ir/serialization.jvm")
project(":compiler:ir.serialization.js").projectDir = File("$rootDir/compiler/ir/serialization.js")
project(":compiler:ir.serialization.native").projectDir = File("$rootDir/compiler/ir/serialization.native")
project(":compiler:ir.serialization.jklib").projectDir = File("$rootDir/compiler/ir/serialization.jklib")
project(":compiler:ir.interpreter").projectDir = File("$rootDir/compiler/ir/ir.interpreter")
project(":kotlin-util-io").projectDir = File("$rootDir/compiler/util-io")
project(":kotlin-util-klib").projectDir = File("$rootDir/compiler/util-klib")
project(":kotlin-util-klib-metadata").projectDir = File("$rootDir/compiler/util-klib-metadata")
project(":kotlin-util-klib-abi").projectDir = File("$rootDir/compiler/util-klib-abi")
project(":native:kotlin-native-utils").projectDir = File("$rootDir/native/utils")
project(":native:frontend.native").projectDir = File("$rootDir/native/frontend")
project(":native:kotlin-klib-commonizer").projectDir = File("$rootDir/native/commonizer")
project(":native:kotlin-klib-commonizer-api").projectDir = File("$rootDir/native/commonizer-api")
project(":native:kotlin-klib-commonizer-embeddable").projectDir = File("$rootDir/native/commonizer-embeddable")
project(":native:objcexport-header-generator-k1").projectDir = File("$rootDir/native/objcexport-header-generator/impl/k1")
project(":native:objcexport-header-generator-analysis-api").projectDir =
    File("$rootDir/native/objcexport-header-generator/impl/analysis-api")
project(":native:external-projects-test-utils:testLibraryA").projectDir =
    File("$rootDir/native/external-projects-test-utils/testDependencies/testLibraryA")
project(":native:external-projects-test-utils:testLibraryB").projectDir =
    File("$rootDir/native/external-projects-test-utils/testDependencies/testLibraryB")
project(":native:external-projects-test-utils:testLibraryC").projectDir =
    File("$rootDir/native/external-projects-test-utils/testDependencies/testLibraryC")
project(":native:external-projects-test-utils:testInternalLibrary").projectDir =
    File("$rootDir/native/external-projects-test-utils/testDependencies/testInternalLibrary")
project(":native:external-projects-test-utils:testExtensionsLibrary").projectDir =
    File("$rootDir/native/external-projects-test-utils/testDependencies/testExtensionsLibrary")
project(":kotlin-parcelize-compiler").projectDir = File("$rootDir/prepare/parcelize-compiler-gradle")

project(":kotlin-allopen-compiler-plugin").projectDir = File("$rootDir/plugins/allopen")
project(":kotlin-allopen-compiler-plugin.embeddable").projectDir = File("$rootDir/plugins/allopen/allopen.embeddable")
project(":kotlin-allopen-compiler-plugin.common").projectDir = File("$rootDir/plugins/allopen/allopen.common")
project(":kotlin-allopen-compiler-plugin.k1").projectDir = File("$rootDir/plugins/allopen/allopen.k1")
project(":kotlin-allopen-compiler-plugin.k2").projectDir = File("$rootDir/plugins/allopen/allopen.k2")
project(":kotlin-allopen-compiler-plugin.cli").projectDir = File("$rootDir/plugins/allopen/allopen.cli")

project(":kotlin-lombok-compiler-plugin").projectDir = File("$rootDir/plugins/lombok")
project(":kotlin-lombok-compiler-plugin.embeddable").projectDir = File("$rootDir/plugins/lombok/lombok.embeddable")
project(":kotlin-lombok-compiler-plugin.cli").projectDir = File("$rootDir/plugins/lombok/lombok.cli")
project(":kotlin-lombok-compiler-plugin.k1").projectDir = File("$rootDir/plugins/lombok/lombok.k1")
project(":kotlin-lombok-compiler-plugin.k2").projectDir = File("$rootDir/plugins/lombok/lombok.k2")
project(":kotlin-lombok-compiler-plugin.common").projectDir = File("$rootDir/plugins/lombok/lombok.common")

project(":kotlin-noarg-compiler-plugin").projectDir = File("$rootDir/plugins/noarg")
project(":kotlin-noarg-compiler-plugin.embeddable").projectDir = File("$rootDir/plugins/noarg/noarg.embeddable")
project(":kotlin-noarg-compiler-plugin.common").projectDir = File("$rootDir/plugins/noarg/noarg.common")
project(":kotlin-noarg-compiler-plugin.k1").projectDir = File("$rootDir/plugins/noarg/noarg.k1")
project(":kotlin-noarg-compiler-plugin.k2").projectDir = File("$rootDir/plugins/noarg/noarg.k2")
project(":kotlin-noarg-compiler-plugin.backend").projectDir = File("$rootDir/plugins/noarg/noarg.backend")
project(":kotlin-noarg-compiler-plugin.cli").projectDir = File("$rootDir/plugins/noarg/noarg.cli")

project(":kotlin-power-assert-compiler-plugin").projectDir = File("$rootDir/plugins/power-assert/power-assert-compiler")
project(":kotlin-power-assert-compiler-plugin.embeddable").projectDir =
    File("$rootDir/plugins/power-assert/power-assert-compiler/power-assert.embeddable")
project(":kotlin-power-assert-compiler-plugin.common").projectDir =
    File("$rootDir/plugins/power-assert/power-assert-compiler/power-assert.common")
project(":kotlin-power-assert-compiler-plugin.frontend").projectDir =
    File("$rootDir/plugins/power-assert/power-assert-compiler/power-assert.frontend")
project(":kotlin-power-assert-compiler-plugin.backend").projectDir =
    File("$rootDir/plugins/power-assert/power-assert-compiler/power-assert.backend")
project(":kotlin-power-assert-compiler-plugin.cli").projectDir =
    File("$rootDir/plugins/power-assert/power-assert-compiler/power-assert.cli")
project(":kotlin-power-assert-runtime").projectDir = File("$rootDir/plugins/power-assert/power-assert-runtime")

project(":kotlin-sam-with-receiver-compiler-plugin").projectDir = File("$rootDir/plugins/sam-with-receiver")
project(":kotlin-sam-with-receiver-compiler-plugin.embeddable").projectDir =
    File("$rootDir/plugins/sam-with-receiver/sam-with-receiver.embeddable")
project(":kotlin-sam-with-receiver-compiler-plugin.common").projectDir = File("$rootDir/plugins/sam-with-receiver/sam-with-receiver.common")
project(":kotlin-sam-with-receiver-compiler-plugin.k1").projectDir = File("$rootDir/plugins/sam-with-receiver/sam-with-receiver.k1")
project(":kotlin-sam-with-receiver-compiler-plugin.k2").projectDir = File("$rootDir/plugins/sam-with-receiver/sam-with-receiver.k2")
project(":kotlin-sam-with-receiver-compiler-plugin.cli").projectDir = File("$rootDir/plugins/sam-with-receiver/sam-with-receiver.cli")

project(":kotlin-assignment-compiler-plugin").projectDir = File("$rootDir/plugins/assign-plugin")
project(":kotlin-assignment-compiler-plugin.common").projectDir = File("$rootDir/plugins/assign-plugin/assign-plugin.common")
project(":kotlin-assignment-compiler-plugin.k1").projectDir = File("$rootDir/plugins/assign-plugin/assign-plugin.k1")
project(":kotlin-assignment-compiler-plugin.k2").projectDir = File("$rootDir/plugins/assign-plugin/assign-plugin.k2")
project(":kotlin-assignment-compiler-plugin.cli").projectDir = File("$rootDir/plugins/assign-plugin/assign-plugin.cli")
project(":kotlin-assignment-compiler-plugin.embeddable").projectDir = File("$rootDir/plugins/assign-plugin/assign-plugin.embeddable")

project(":tools:kotlinp").projectDir = File("$rootDir/libraries/tools/kotlinp")
project(":tools:kotlinp-jvm").projectDir = File("$rootDir/libraries/tools/kotlinp/jvm")
project(":tools:kotlinp-klib").projectDir = File("$rootDir/libraries/tools/kotlinp/klib")
project(":tools:stats-analyser").projectDir = File("$rootDir/libraries/tools/stats-analyser")
project(":kotlin-build-tools-enum-compat").projectDir = File("$rootDir/libraries/tools/kotlin-build-tools-enum-compat")
project(":kotlin-gradle-compiler-types").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-compiler-types")
project(":kotlin-gradle-plugin-api").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-api")
project(":kotlin-gradle-plugin-annotations").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-annotations")
project(":kotlin-gradle-plugin-idea").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-idea")
project(":kotlin-gradle-plugin-idea-proto").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-idea-proto")
project(":kotlin-gradle-plugin-idea-for-compatibility-tests").projectDir =
    File("$rootDir/libraries/tools/kotlin-gradle-plugin-idea-for-compatibility-tests")
project(":kotlin-gradle-plugin-dsl-codegen").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-dsl-codegen")
project(":kotlin-gradle-statistics").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-statistics")
project(":kotlin-gradle-build-metrics").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-build-metrics")
project(":kotlin-gradle-plugin").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin")
project(":gradle:kotlin-gradle-ecosystem-plugin").projectDir = File("$rootDir/libraries/tools/gradle/kotlin-gradle-ecosystem-plugin")
project(":kotlin-gradle-plugin-tcs-android").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-tcs-android")
project(":kotlin-gradle-plugin-test-utils-embeddable").projectDir =
    File("$rootDir/libraries/tools/kotlin-gradle-plugin-test-utils-embeddable")
project(":kotlin-gradle-plugin-integration-tests").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-integration-tests")
project(":kotlin-gradle-plugins-bom").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugins-bom")
project(":kotlin-privacy-manifests-plugin").projectDir = File("$rootDir/libraries/tools/kotlin-privacy-manifests-plugin")
project(":gradle:android-test-fixes").projectDir = File("$rootDir/libraries/tools/gradle/android-test-fixes")
project(":gradle:documentation").projectDir = File("$rootDir/libraries/tools/gradle/documentation")
project(":gradle:gradle-warnings-detector").projectDir = File("$rootDir/libraries/tools/gradle/gradle-warnings-detector")
project(":gradle:kotlin-compiler-args-properties").projectDir = File("$rootDir/libraries/tools/gradle/kotlin-compiler-args-properties")
project(":gradle:regression-benchmark-templates").projectDir = File("$rootDir/libraries/tools/gradle/regression-benchmark-templates")
project(":gradle:regression-benchmarks").projectDir = File("$rootDir/libraries/tools/gradle/regression-benchmarks")
project(":kotlin-tooling-metadata").projectDir = File("$rootDir/libraries/tools/kotlin-tooling-metadata")
project(":kotlin-tooling-core").projectDir = File("$rootDir/libraries/tools/kotlin-tooling-core")
project(":compose-compiler-gradle-plugin").projectDir = File("$rootDir/libraries/tools/kotlin-compose-compiler")
project(":kotlin-allopen").projectDir = File("$rootDir/libraries/tools/kotlin-allopen")
project(":kotlin-noarg").projectDir = File("$rootDir/libraries/tools/kotlin-noarg")
project(":kotlin-power-assert").projectDir = File("$rootDir/libraries/tools/kotlin-power-assert")
project(":kotlin-sam-with-receiver").projectDir = File("$rootDir/libraries/tools/kotlin-sam-with-receiver")
project(":kotlin-assignment").projectDir = File("$rootDir/libraries/tools/kotlin-assignment")
project(":kotlin-lombok").projectDir = File("$rootDir/libraries/tools/kotlin-lombok")
project(":kotlin-gradle-subplugin-example").projectDir = File("$rootDir/libraries/examples/kotlin-gradle-subplugin-example")
project(":examples").projectDir = File("$rootDir/libraries/examples")
project(":examples:annotation-processor-example").projectDir = File("$rootDir/libraries/examples/annotation-processor-example")
project(":kotlin-daemon-embeddable").projectDir = File("$rootDir/prepare/kotlin-daemon-embeddable")
project(":kotlin-annotation-processing-embeddable").projectDir = File("$rootDir/plugins/kapt/kotlin-annotation-processing-embeddable")
project(":kotlin-annotation-processing-cli").projectDir = File("$rootDir/plugins/kapt/kapt-cli")
project(":kotlin-annotation-processing-base").projectDir = File("$rootDir/plugins/kapt/kapt-base")
project(":kotlin-annotation-processing-runtime").projectDir = File("$rootDir/plugins/kapt/kapt-runtime")
project(":kotlin-annotation-processing").projectDir = File("$rootDir/plugins/kapt/kapt-compiler")
project(":kotlin-annotations-jvm").projectDir = File("$rootDir/libraries/tools/kotlin-annotations-jvm")
project(":kotlin-scripting-common").projectDir = File("$rootDir/libraries/scripting/common")
project(":kotlin-scripting-jvm").projectDir = File("$rootDir/libraries/scripting/jvm")
project(":kotlin-scripting-jvm-host-unshaded").projectDir = File("$rootDir/libraries/scripting/jvm-host")
project(":kotlin-scripting-jvm-host-test").projectDir = File("$rootDir/libraries/scripting/jvm-host-test")
project(":kotlin-scripting-jvm-host").projectDir = File("$rootDir/libraries/scripting/jvm-host-embeddable")
project(":kotlin-scripting-dependencies").projectDir = File("$rootDir/libraries/scripting/dependencies")
project(":kotlin-scripting-dependencies-maven").projectDir = File("$rootDir/libraries/scripting/dependencies-maven")
project(":kotlin-scripting-dependencies-maven-all").projectDir = File("$rootDir/libraries/scripting/dependencies-maven-all")
project(":kotlin-scripting-jsr223-unshaded").projectDir = File("$rootDir/libraries/scripting/jsr223")
project(":kotlin-scripting-jsr223-test").projectDir = File("$rootDir/libraries/scripting/jsr223-test")
project(":kotlin-scripting-jsr223").projectDir = File("$rootDir/libraries/scripting/jsr223-embeddable")
project(":kotlin-scripting-intellij").projectDir = File("$rootDir/libraries/scripting/intellij")
project(":kotlin-scripting-compiler").projectDir = File("$rootDir/plugins/scripting/scripting-compiler")
project(":kotlin-scripting-compiler-embeddable").projectDir = File("$rootDir/plugins/scripting/scripting-compiler-embeddable")
project(":kotlin-scripting-compiler-impl").projectDir = File("$rootDir/plugins/scripting/scripting-compiler-impl")
project(":kotlin-scripting-compiler-impl-embeddable").projectDir = File("$rootDir/plugins/scripting/scripting-compiler-impl-embeddable")
project(":kotlin-main-kts").projectDir = File("$rootDir/libraries/tools/kotlin-main-kts")
project(":kotlin-main-kts-test").projectDir = File("$rootDir/libraries/tools/kotlin-main-kts-test")
project(":examples:scripting-jvm-simple-script").projectDir = File("$rootDir/libraries/examples/scripting/jvm-simple-script/script")
project(":examples:scripting-jvm-simple-script-host").projectDir = File("$rootDir/libraries/examples/scripting/jvm-simple-script/host")
project(":examples:scripting-jvm-maven-deps").projectDir = File("$rootDir/libraries/examples/scripting/jvm-maven-deps/script")
project(":examples:scripting-jvm-maven-deps-host").projectDir = File("$rootDir/libraries/examples/scripting/jvm-maven-deps/host")
project(":examples:scripting-jvm-embeddable-host").projectDir = File("$rootDir/libraries/examples/scripting/jvm-embeddable-host")
project(":libraries:kotlin-prepush-hook").projectDir = File("$rootDir/libraries/tools/kotlin-prepush-hook")
project(":plugins:jvm-abi-gen").projectDir = File("$rootDir/plugins/jvm-abi-gen")
project(":plugins:jvm-abi-gen-embeddable").projectDir = File("$rootDir/plugins/jvm-abi-gen/embeddable")

project(":js:js.tests").projectDir = File("$rootDir/js/js.tests")
project(":js:js.tests:klib-compatibility").projectDir = File("$rootDir/js/js.tests/klib-compatibility")

project(":kotlinx-serialization-compiler-plugin").projectDir = File("$rootDir/plugins/kotlinx-serialization")
project(":kotlinx-serialization-compiler-plugin.embeddable").projectDir =
    File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.embeddable")
project(":kotlinx-serialization-compiler-plugin.cli").projectDir = File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.cli")
project(":kotlinx-serialization-compiler-plugin.backend").projectDir =
    File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.backend")
project(":kotlinx-serialization-compiler-plugin.k1").projectDir = File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.k1")
project(":kotlinx-serialization-compiler-plugin.k2").projectDir = File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.k2")
project(":kotlinx-serialization-compiler-plugin.common").projectDir =
    File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.common")
project(":kotlin-serialization").projectDir = file("$rootDir/libraries/tools/kotlin-serialization")
project(":kotlin-serialization-unshaded").projectDir = file("$rootDir/libraries/tools/kotlin-serialization-unshaded")

project(":kotlin-atomicfu-compiler-plugin").projectDir = file("$rootDir/plugins/atomicfu/atomicfu-compiler")
project(":kotlin-atomicfu-compiler-plugin-embeddable").projectDir = file("$rootDir/plugins/atomicfu/atomicfu-compiler-embeddable")
project(":kotlinx-atomicfu-runtime").projectDir = file("$rootDir/plugins/atomicfu/atomicfu-runtime")
project(":atomicfu").projectDir = file("$rootDir/libraries/tools/atomicfu")

project(":kotlin-dataframe-compiler-plugin").projectDir = File("$rootDir/plugins/kotlin-dataframe")
project(":kotlin-dataframe-compiler-plugin.embeddable").projectDir = File("$rootDir/plugins/kotlin-dataframe/kotlin-dataframe.embeddable")
project(":kotlin-dataframe-compiler-plugin.cli").projectDir = File("$rootDir/plugins/kotlin-dataframe/kotlin-dataframe.cli")
project(":kotlin-dataframe-compiler-plugin.backend").projectDir = File("$rootDir/plugins/kotlin-dataframe/kotlin-dataframe.backend")
project(":kotlin-dataframe-compiler-plugin.k2").projectDir = File("$rootDir/plugins/kotlin-dataframe/kotlin-dataframe.k2")
project(":kotlin-dataframe-compiler-plugin.common").projectDir = File("$rootDir/plugins/kotlin-dataframe/kotlin-dataframe.common")
project(":kotlin-dataframe").projectDir = file("$rootDir/libraries/tools/kotlin-dataframe")

project(":plugins:compose-compiler-plugin").projectDir = file("$rootDir/plugins/compose")
project(":plugins:compose-compiler-plugin:compiler").projectDir = file("$rootDir/plugins/compose/compiler")
project(":plugins:compose-compiler-plugin:compiler-hosted").projectDir = file("$rootDir/plugins/compose/compiler-hosted")
project(":plugins:compose-compiler-plugin:compiler-hosted:integration-tests").projectDir =
    file("$rootDir/plugins/compose/compiler-hosted/integration-tests")
project(":plugins:compose-compiler-plugin:compiler-hosted:integration-tests:protobuf-test-classes").projectDir =
    file("$rootDir/plugins/compose/compiler-hosted/integration-tests/protobuf-test-classes")
project(":plugins:compose-compiler-plugin:group-mapping").projectDir = file("$rootDir/plugins/compose/group-mapping")

if (buildProperties.isInIdeaSync.get()) {
    project(":plugins:compose-compiler-plugin:compiler-hosted:runtime-tests").projectDir =
        file("$rootDir/plugins/compose/compiler-hosted/runtime-tests")
}

project(":kotlin-scripting-ide-services-unshaded").projectDir = File("$rootDir/plugins/scripting/scripting-ide-services")
project(":kotlin-scripting-ide-services-test").projectDir = File("$rootDir/plugins/scripting/scripting-ide-services-test")
project(":kotlin-scripting-ide-services").projectDir = File("$rootDir/plugins/scripting/scripting-ide-services-embeddable")
project(":kotlin-scripting-ide-common").projectDir = File("$rootDir/plugins/scripting/scripting-ide-common")
project(":kotlin-scripting-compiler").projectDir = File("$rootDir/plugins/scripting/scripting-compiler")
project(":kotlin-scripting-compiler-impl").projectDir = File("$rootDir/plugins/scripting/scripting-compiler-impl")

// Uncomment to use locally built protobuf-relocated
// includeBuild("dependencies/protobuf")
if (buildProperties.isKotlinNativeEnabled.get()) {
    includeBuild("kotlin-native/build-tools") {
        name = "native-build-tools"
    }
    include(":kotlin-native:dependencies")
    include(":kotlin-native:endorsedLibraries:kotlinx.cli")
    include(":kotlin-native:Interop:StubGenerator")
    include(":kotlin-native:Interop:StubGeneratorConsistencyCheck")
    include(":kotlin-native:backend.native")
    project(":kotlin-native:backend.native").projectDir = File("$rootDir/kotlin-native/backend.native/compiler/ir/backend.native")
    include(":kotlin-native:Interop:Runtime")
    include(":kotlin-native:Interop:Indexer")
    include(":kotlin-native:utilities:cli-runner")
    include(":kotlin-native:klib")
    include(":kotlin-native:common")
    include(":kotlin-native:common:env")
    include(":kotlin-native:common:files")
    include(":kotlin-native:runtime")
    include(":kotlin-native:libllvmext")
    include(":kotlin-native:llvmDebugInfoC")
    include(":kotlin-native:utilities")
    include(":kotlin-native:tools:kdumputil")
    include(":kotlin-native:platformLibs")
    include(":kotlin-native:libclangext")
    include(":kotlin-native:llvmInterop")
    include(":kotlin-native:libclangInterop")
    include(":kotlin-native:backend.native:tests")
    project(":kotlin-native:backend.native:tests").projectDir = File("$rootDir/kotlin-native/backend.native/tests")
    include(":kotlin-native:prepare:kotlin-native-compiler-embeddable")
    include(":kotlin-native:tools:compiler-cache-invalidator")
    include(":native:kotlin-test-native-xctest")
    include(":native:cli-native")
    include(":native:native.tests:cli-tests")
    include(":kotlin-native:tools:minidump-analyzer")
}

include("compiler:test-security-manager")
