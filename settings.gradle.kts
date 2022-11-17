pluginManagement {
    apply(from = "repo/scripts/cache-redirector.settings.gradle.kts")

    repositories {
        val pluginRepo = System.getProperty("bootstrap.kotlin.repo")
        if (pluginRepo != null) {
            maven {
                url = uri(pluginRepo)
            }
        }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("de.undercouch.download") version ("5.1.0")
    }
}

buildscript {
    val buildGradlePluginVersion = extra["kotlin.build.gradlePlugin.version"]
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${buildGradlePluginVersion}")
    }
}

plugins {
    id("com.gradle.enterprise") version ("3.11.2")
    id("com.gradle.common-custom-user-data-gradle-plugin") version ("1.8.1") apply false
}

val buildProperties = getKotlinBuildPropertiesForSettings(settings)

if (buildProperties.buildScanServer != null) {
    apply(plugin = "com.gradle.CommonCustomUserDataGradlePlugin")
}

gradleEnterprise {
    val buildScanServer = buildProperties.buildScanServer
    val isTeamCity = buildProperties.isTeamcityBuild

    buildScan {
        if (buildScanServer != null) {
            server = buildScanServer
            isCaptureTaskInputFiles = true
            publishAlways()
        } else {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }

        obfuscation {
            ipAddresses { return@ipAddresses arrayListOf("0.0.0.0") }
            hostname { return@hostname "concealed" }
            username { return@username if (isTeamCity) "TeamCity" else "concealed" }
        }
    }
}

buildCache {
    local {
        isEnabled = buildProperties.localBuildCacheEnabled
        if (buildProperties.localBuildCacheDirectory != null) {
            directory = buildProperties.localBuildCacheDirectory
        }
    }

    if (buildProperties.buildCacheUrl != null) {
        remote(HttpBuildCache::class.java) {
            url = java.net.URI(buildProperties.buildCacheUrl!!)
            isPush = buildProperties.pushToBuildCache
            if (buildProperties.buildCacheUser != null && buildProperties.buildCachePassword != null) {
                credentials.username = buildProperties.buildCacheUser
                credentials.password = buildProperties.buildCachePassword
            }
        }
    }
}

// modules
include(
    ":benchmarks",
    ":compiler",
    ":compiler:util",
    ":compiler:config",
    ":compiler:config.jvm",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-metadata",
    ":daemon-common",
    ":daemon-common-new",
    ":kotlin-daemon",
    ":kotlin-daemon-client",
    ":kotlin-daemon-client-new",
    ":kotlin-daemon-tests",
    ":kotlin-preloader",
    ":kotlin-runner",
    ":compiler:container",
    ":compiler:resolution.common",
    ":compiler:resolution.common.jvm",
    ":compiler:resolution",
    ":compiler:serialization",
    ":compiler:psi",
    ":compiler:visualizer",
    ":compiler:visualizer:common",
    ":compiler:visualizer:render-fir",
    ":compiler:visualizer:render-psi",
    ":compiler:frontend",
    ":compiler:frontend.common",
    ":compiler:frontend.common-psi",
    ":compiler:frontend.java",
    ":compiler:frontend:cfg",
    ":kotlin-compiler-runner-unshaded",
    ":kotlin-compiler-runner",
    ":compiler:cli-common",
    ":compiler:ir.tree",
    ":compiler:ir.tree:tree-generator",
    ":compiler:ir.psi2ir",
    ":compiler:ir.ir2cfg",
    ":compiler:ir.backend.common",
    ":compiler:ir.serialization.common",
    ":compiler:ir.serialization.jvm",
    ":compiler:ir.serialization.js",
    ":compiler:ir.interpreter",
    ":compiler:backend.js",
    ":compiler:backend.wasm",
    ":compiler:backend.jvm",
    ":compiler:backend.jvm.lower",
    ":compiler:backend.jvm.codegen",
    ":compiler:backend.jvm.entrypoint",
    ":compiler:backend-common",
    ":compiler:backend",
    ":compiler:plugin-api",
    ":compiler:light-classes",
    ":compiler:javac-wrapper",
    ":compiler:cli",
    ":compiler:cli-js",
    ":compiler:incremental-compilation-impl",
    ":compiler:tests-compiler-utils",
    ":compiler:tests-common",
    ":compiler:tests-mutes",
    ":compiler:tests-mutes:tc-integration",
    ":compiler:tests-against-klib",
    ":compiler:tests-for-compiler-generator",
    ":js:js.ast",
    ":js:js.sourcemap",
    ":js:js.serializer",
    ":js:js.parser",
    ":js:js.config",
    ":js:js.engines",
    ":js:js.frontend",
    ":js:js.translator",
    ":js:js.dce",
    ":js:js.tests",
    ":native:kotlin-native-utils",
    ":native:frontend.native",
    ":native:kotlin-klib-commonizer",
    ":native:kotlin-klib-commonizer-api",
    ":native:kotlin-klib-commonizer-embeddable",
    ":core:compiler.common",
    ":core:compiler.common.jvm",
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
    ":plugins:android-extensions-compiler",
    ":kotlin-android-extensions",
    ":kotlin-android-extensions-runtime"
)

include(
    ":kotlin-allopen-compiler-plugin",
    ":kotlin-allopen-compiler-plugin.common",
    ":kotlin-allopen-compiler-plugin.k1",
    ":kotlin-allopen-compiler-plugin.k2",
    ":kotlin-allopen-compiler-plugin.cli"
)

include(
    ":kotlin-noarg-compiler-plugin",
    ":kotlin-noarg-compiler-plugin.common",
    ":kotlin-noarg-compiler-plugin.k1",
    ":kotlin-noarg-compiler-plugin.k2",
    ":kotlin-noarg-compiler-plugin.backend",
    ":kotlin-noarg-compiler-plugin.cli"
)

include(
    ":kotlin-sam-with-receiver-compiler-plugin",
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
    ":kotlin-imports-dumper-compiler-plugin",
    ":kotlin-script-runtime",
    ":plugins:fir-plugin-prototype",
    ":plugins:fir-plugin-prototype:plugin-annotations",
    ":plugins:fir-plugin-prototype:fir-plugin-ic-test",
    ":kotlin-test:kotlin-test-common",
    ":kotlin-test:kotlin-test-annotations-common",
    ":kotlin-test:kotlin-test-jvm",
    ":kotlin-test:kotlin-test-junit",
    ":kotlin-test:kotlin-test-junit5",
    ":kotlin-test:kotlin-test-testng",
    ":kotlin-test-js-runner",
    ":kotlinx-metadata",
    ":kotlinx-metadata-jvm",
    ":kotlinx-metadata-klib",
    ":prepare:build.version",
    ":kotlin-build-common",
    ":prepare:kotlin-compiler-internal-test-framework",
    ":kotlin-compiler",
    ":kotlin-compiler-embeddable",
    ":kotlin-compiler-client-embeddable",
    ":kotlin-reflect",
    ":kotlin-reflect-api",
    ":kotlin-ant",
    ":compiler:tests-java8",
    ":compiler:tests-different-jdk",
    ":compiler:tests-spec",
    ":generators",
    ":generators:ide-iml-to-gradle-generator",
    ":generators:test-generator",
    ":tools:kotlinp",
    ":kotlin-project-model",
    ":kotlin-project-model-tests-generator",
    ":kotlin-gradle-compiler-types",
    ":kotlin-gradle-plugin-api",
    ":kotlin-gradle-plugin-annotations",
    ":kotlin-gradle-plugin-idea",
    ":kotlin-gradle-plugin-idea-proto",
    ":kotlin-gradle-plugin-idea-for-compatibility-tests",
    ":kotlin-gradle-plugin-dsl-codegen",
    ":kotlin-gradle-plugin-npm-versions-codegen",
    ":kotlin-gradle-statistics",
    ":kotlin-gradle-build-metrics",
    ":kotlin-gradle-plugin",
    ":kotlin-gradle-plugin-kpm-android",
    ":kotlin-gradle-plugin-tcs-android",
    ":kotlin-gradle-plugin-model",
    ":kotlin-gradle-plugin-test-utils-embeddable",
    ":kotlin-gradle-plugin-integration-tests",
    ":gradle:android-test-fixes",
    ":gradle:regression-benchmark-templates",
    ":gradle:regression-benchmarks",
    ":kotlin-tooling-metadata",
    ":kotlin-tooling-core",
    ":kotlin-allopen",
    ":kotlin-noarg",
    ":kotlin-sam-with-receiver",
    ":kotlin-assignment",
    ":kotlin-gradle-subplugin-example",
    ":examples:annotation-processor-example",
    ":kotlin-script-util",
    ":kotlin-annotation-processing",
    ":kotlin-annotation-processing-cli",
    ":kotlin-annotation-processing-base",
    ":kotlin-annotation-processing-runtime",
    ":kotlin-annotation-processing-gradle",
    ":kotlin-annotation-processing-embeddable",
    ":kotlin-daemon-embeddable",
    ":examples:kotlin-jsr223-local-example",
    ":examples:kotlin-jsr223-daemon-local-eval-example",
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
    ":pill:pill-importer",
    ":pill:generate-all-tests",
    ":libraries:kotlin-prepush-hook",
    ":libraries:tools:mutability-annotations-compat",
    ":plugins:jvm-abi-gen",
    ":plugins:jvm-abi-gen-embeddable",
    ":test-instrumenter",
    ":wasm:wasm.ir",
    ":repo:codebase-tests"
)

include(
    ":kotlinx-atomicfu-compiler-plugin",
    ":kotlinx-atomicfu-runtime",
    ":atomicfu"
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
    ":compiler:fir:fir-serialization",
    ":compiler:fir:fir-deserialization",
    ":compiler:fir:java",
    ":compiler:fir:modularized-tests",
    ":compiler:fir:dump",
    ":compiler:fir:checkers",
    ":compiler:fir:checkers:checkers.jvm",
    ":compiler:fir:checkers:checkers.js",
    ":compiler:fir:checkers:checkers.native",
    ":compiler:fir:checkers:checkers-component-generator",
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
    ":compiler:test-infrastructure-utils",
    ":compiler:tests-common-new"
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
    ":kotlin-lombok-compiler-plugin.common",
    ":kotlin-lombok-compiler-plugin.k1",
    ":kotlin-lombok-compiler-plugin.k2",
    ":kotlin-lombok-compiler-plugin.cli",
    ":kotlin-lombok"
)

include(
    ":kotlinx-serialization-compiler-plugin",
    ":kotlinx-serialization-compiler-plugin.common",
    ":kotlinx-serialization-compiler-plugin.k1",
    ":kotlinx-serialization-compiler-plugin.k2",
    ":kotlinx-serialization-compiler-plugin.backend",
    ":kotlinx-serialization-compiler-plugin.cli",
    ":kotlin-serialization",
    ":kotlin-serialization-unshaded"
)

if (!buildProperties.isInJpsBuildIdeaSync) {
    include(
        ":prepare:ide-plugin-dependencies:android-extensions-compiler-plugin-for-ide",
        ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-for-ide",
        ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-tests-for-ide",
        ":prepare:ide-plugin-dependencies:incremental-compilation-impl-tests-for-ide",
        ":prepare:ide-plugin-dependencies:js-ir-runtime-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-build-common-tests-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-compiler-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-compiler-cli-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-dist-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-gradle-statistics-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-jps-common-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-classpath",
        ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-tests-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-testdata-for-ide",
        ":prepare:ide-plugin-dependencies:kotlinx-serialization-compiler-plugin-for-ide",
        ":prepare:ide-plugin-dependencies:noarg-compiler-plugin-for-ide",
        ":prepare:ide-plugin-dependencies:sam-with-receiver-compiler-plugin-for-ide",
        ":prepare:ide-plugin-dependencies:assignment-compiler-plugin-for-ide",
        ":prepare:ide-plugin-dependencies:parcelize-compiler-plugin-for-ide",
        ":prepare:ide-plugin-dependencies:lombok-compiler-plugin-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-backend-native-for-ide",
        ":prepare:ide-plugin-dependencies:tests-common-tests-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-compiler-testdata-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-stdlib-minimal-for-test-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-compiler-tests-for-ide",
        ":prepare:ide-plugin-dependencies:low-level-api-fir-for-ide",
        ":prepare:ide-plugin-dependencies:high-level-api-for-ide",
        ":prepare:ide-plugin-dependencies:high-level-api-impl-base-for-ide",
        ":prepare:ide-plugin-dependencies:high-level-api-impl-base-tests-for-ide",
        ":prepare:ide-plugin-dependencies:high-level-api-fir-for-ide",
        ":prepare:ide-plugin-dependencies:high-level-api-fir-tests-for-ide",
        ":prepare:ide-plugin-dependencies:high-level-api-fe10-for-ide",
        ":prepare:ide-plugin-dependencies:high-level-api-fe10-tests-for-ide",
        ":prepare:ide-plugin-dependencies:kt-references-fe10-for-ide",
        ":prepare:ide-plugin-dependencies:analysis-api-providers-for-ide",
        ":prepare:ide-plugin-dependencies:analysis-project-structure-for-ide",
        ":prepare:ide-plugin-dependencies:symbol-light-classes-for-ide",
        ":prepare:ide-plugin-dependencies:analysis-api-standalone-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-compiler-ir-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-compiler-common-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-compiler-fe10-for-ide",
        ":prepare:ide-plugin-dependencies:kotlin-compiler-fir-for-ide"
    )
}

fun intellij(imlPath: String) {
    var imlFile = File("${rootDir}/intellij/community/plugins/kotlin/${imlPath}")
    imlFile = if (imlFile.exists()) imlFile else File("${rootDir}/intellij/plugins/kotlin/${imlPath}")
    imlFile = if (imlFile.exists()) imlFile else File("${rootDir}/intellij/community/${imlPath}")
    imlFile = if (imlFile.exists()) imlFile else File("${rootDir}/intellij/${imlPath}")
    if (!imlFile.exists()) {
        throw IllegalStateException(
            "$imlFile doesn't exist. Please, update mapping in settings.gradle. And regenerate " +
                    "build.gradle files in kt-branch in 'intellij' repo (./gradlew generateIdePluginGradleFiles). Or you can " +
                    "remove 'attachedIntellijVersion' in your 'local.properties' if you don't care about being possible to" +
                    "browse Kotlin plugin sources in scope of Kotlin compiler sources"
        )
    }
    val fileName = imlFile.name
    val projectName = ":kotlin-ide.${fileName.substring(0, fileName.length - ".iml".length)}"
    include(projectName)
    project(projectName).projectDir = imlFile.parentFile
}

val attachedIntellijVersion = buildProperties.getOrNull("attachedIntellijVersion")
if (attachedIntellijVersion == "212") { // Latest available platform in scope of KT release cycle
    logger.info("Including kotlin-ide modules in settings.gradle")
    intellij("java/compiler/intellij.java.compiler.tests.iml")
    intellij("platform/testFramework/extensions/intellij.platform.testExtensions.iml")
    intellij("platform/lang-impl/intellij.platform.lang.tests.iml")
    intellij("platform/xdebugger-testFramework/intellij.platform.debugger.testFramework.iml")
    intellij("platform/external-system-impl/intellij.platform.externalSystem.tests.iml")
    intellij("jvm-debugger/core/kotlin.jvm-debugger.core.iml")
    intellij("jvm-debugger/util/kotlin.jvm-debugger.util.iml")
    intellij("jvm-debugger/eval4j/kotlin.eval4j.iml")
    intellij("jvm-debugger/test/kotlin.jvm-debugger.test.iml")
    intellij("jvm-debugger/evaluation/kotlin.jvm-debugger.evaluation.iml")
    intellij("jvm-debugger/coroutines/kotlin.jvm-debugger.coroutines.iml")
    intellij("jvm-debugger/sequence/kotlin.jvm-debugger.sequence.iml")
    intellij("jvm/kotlin.jvm.iml")
    intellij("core/kotlin.core.iml")
    intellij("common/kotlin.common.iml")
    intellij("tests-common/kotlin.tests-common.iml")
    intellij("j2k/services/kotlin.j2k.services.iml")
    intellij("j2k/old/kotlin.j2k.old.iml")
    intellij("j2k/old/tests/kotlin.j2k.old.tests.iml")
    intellij("j2k/idea/kotlin.j2k.idea.iml")
    intellij("j2k/new/tests/kotlin.j2k.new.tests.iml")
    intellij("j2k/new/kotlin.j2k.new.iml")
    intellij("fir-low-level-api-ide-impl/kotlin.fir.fir-low-level-api-ide-impl.iml")
    intellij("fir-analysis-project-structure-ide-impl/kotlin.fir.analysis-project-structure-ide-impl.iml")
    intellij("analysis-api-providers-ide-impl/kotlin.fir.analysis-api-providers-ide-impl.iml")
    intellij("native/kotlin.native.iml")
    intellij("performance-tests/kotlin.performance-tests.iml")
    intellij("injection/kotlin.injection.iml")
    intellij("resources-fe10/kotlin.resources-fe10.iml")
    intellij("git/kotlin.git.iml")
    intellij("idea/tests/kotlin.idea.tests.iml")
    intellij("idea/kotlin.idea.iml")
    intellij("project-wizard/core/kotlin.project-wizard.core.iml")
    intellij("project-wizard/idea/kotlin.project-wizard.idea.iml")
    intellij("project-wizard/cli/kotlin.project-wizard.cli.iml")
    intellij("resources-fir/kotlin.resources-fir.iml")
    intellij("kotlin.all-tests/kotlin.all-tests.iml")
    intellij("i18n/kotlin.i18n.iml")
    intellij("uast/uast-kotlin-idea-fir/kotlin.uast.uast-kotlin-idea-fir.iml")
    intellij("uast/uast-kotlin-idea/kotlin.uast.uast-kotlin-idea.iml")
    intellij("uast/uast-kotlin-fir/kotlin.uast.uast-kotlin-fir.iml")
    intellij("uast/uast-kotlin-base/kotlin.uast.uast-kotlin-base.iml")
    intellij("uast/uast-kotlin-idea-base/kotlin.uast.uast-kotlin-idea-base.iml")
    intellij("uast/uast-kotlin/kotlin.uast.uast-kotlin.iml")
    intellij("test-framework/kotlin.test-framework.iml")
    intellij("generators/kotlin.generators.iml")
    intellij("gradle/gradle-native/kotlin.gradle.gradle-native.iml")
    intellij("gradle/gradle-idea/kotlin.gradle.gradle-idea.iml")
    intellij("gradle/gradle-tooling/kotlin.gradle.gradle-tooling.iml")
    intellij("scripting/kotlin.scripting.iml")
    intellij("compiler-plugins/allopen/common/kotlin.compiler-plugins.allopen.common.iml")
    intellij("compiler-plugins/allopen/tests/kotlin.compiler-plugins.allopen.tests.iml")
    intellij("compiler-plugins/allopen/gradle/kotlin.compiler-plugins.allopen.gradle.iml")
    intellij("compiler-plugins/allopen/maven/kotlin.compiler-plugins.allopen.maven.iml")
    intellij("compiler-plugins/kapt/kotlin.compiler-plugins.kapt.iml")
    intellij("compiler-plugins/kotlinx-serialization/common/kotlin.compiler-plugins.kotlinx-serialization.common.iml")
    intellij("compiler-plugins/kotlinx-serialization/gradle/kotlin.compiler-plugins.kotlinx-serialization.gradle.iml")
    intellij("compiler-plugins/kotlinx-serialization/maven/kotlin.compiler-plugins.kotlinx-serialization.maven.iml")
    intellij("compiler-plugins/parcelize/common/kotlin.compiler-plugins.parcelize.common.iml")
    intellij("compiler-plugins/parcelize/tests/kotlin.compiler-plugins.parcelize.tests.iml")
    intellij("compiler-plugins/parcelize/gradle/kotlin.compiler-plugins.parcelize.gradle.iml")
    intellij("compiler-plugins/scripting/kotlin.compiler-plugins.scripting.iml")
    intellij("compiler-plugins/noarg/common/kotlin.compiler-plugins.noarg.common.iml")
    intellij("compiler-plugins/noarg/tests/kotlin.compiler-plugins.noarg.tests.iml")
    intellij("compiler-plugins/noarg/gradle/kotlin.compiler-plugins.noarg.gradle.iml")
    intellij("compiler-plugins/noarg/maven/kotlin.compiler-plugins.noarg.maven.iml")
    intellij("compiler-plugins/lombok/kotlin.compiler-plugins.lombok.iml")
    intellij("compiler-plugins/sam-with-receiver/common/kotlin.compiler-plugins.sam-with-receiver.common.iml")
    intellij("compiler-plugins/sam-with-receiver/gradle/kotlin.compiler-plugins.sam-with-receiver.gradle.iml")
    intellij("compiler-plugins/sam-with-receiver/maven/kotlin.compiler-plugins.sam-with-receiver.maven.iml")
    intellij("compiler-plugins/base-compiler-plugins-ide-support/kotlin.compiler-plugins.base-compiler-plugins-ide-support.iml")
    intellij("line-indent-provider/kotlin.line-indent-provider.iml")
    intellij("scripting-support/kotlin.scripting-support.iml")
    intellij("formatter/kotlin.formatter.iml")
    intellij("fir/kotlin.fir.iml")
    intellij("fir-fe10-binding/kotlin.fir.fir-fe10-binding.iml")
    intellij("maven/tests/kotlin.maven.tests.iml")
    intellij("maven/kotlin.maven.iml")
    intellij("frontend-independent/tests/kotlin.fir.frontend-independent.tests.iml")
    intellij("frontend-independent/kotlin.fir.frontend-independent.iml")
    intellij("kotlin-compiler-classpath/kotlin.util.compiler-classpath.iml")
    intellij("repl/kotlin.repl.iml")
    intellij("plugins/gradle/tooling-extension-impl/intellij.gradle.toolingExtension.tests.iml")
    intellij("plugins/gradle/intellij.gradle.tests.iml")
    intellij("plugins/maven/intellij.maven.iml")
    intellij("jvm-run-configurations/kotlin.jvm-run-configurations.iml")
}

if (attachedIntellijVersion == "master") {
    apply(from = "repo/scripts/includeKotlinIdeModules.gradle")
    //// These modules are used in Kotlin plugin and IDEA doesn't publish artifact of these modules
    intellij("plugins/gradle/intellij.gradle.tests.iml")
    intellij("plugins/gradle/java/intellij.gradle.java.iml")
    intellij("plugins/gradle/jps-plugin/intellij.gradle.jps.iml")
    intellij("xml/relaxng/intellij.relaxng.iml")
    intellij("plugins/maven/intellij.maven.iml")
    intellij("jvm-run-configurations/kotlin.jvm-run-configurations.iml")
    intellij("java/compiler/intellij.java.compiler.tests.iml")
    intellij("platform/testFramework/extensions/intellij.platform.testExtensions.iml")
    intellij("platform/lang-impl/intellij.platform.lang.tests.iml")
    intellij("platform/xdebugger-testFramework/intellij.platform.debugger.testFramework.iml")
    intellij("platform/external-system-impl/intellij.platform.externalSystem.tests.iml")
    intellij("plugins/gradle/tooling-extension-impl/intellij.gradle.toolingExtension.tests.iml")
    intellij("jvm/jvm-analysis-tests/intellij.jvm.analysis.testFramework.iml")
    intellij("jvm/jvm-analysis-kotlin-tests/intellij.jvm.analysis.kotlin.tests.iml")
    intellij("platform/configuration-store-impl/intellij.platform.configurationStore.tests.iml")
    intellij("plugins/stats-collector/intellij.statsCollector.tests.iml")
    intellij("plugins/groovy/groovy-uast-tests/intellij.groovy.uast.tests.iml")
}

include(
    ":jps:jps-common",
    ":jps:jps-plugin",
    ":prepare:kotlin-jps-plugin",
    ":jps:jps-platform-api-signatures"
)

include(
    ":generators:analysis-api-generator",
    ":analysis",
    ":analysis:low-level-api-fir",
    ":analysis:analysis-api-fir:analysis-api-fir-generator",
    ":analysis:analysis-api-fir",
    ":analysis:analysis-api",
    ":analysis:analysis-api-impl-barebone",
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-providers",
    ":analysis:analysis-internal-utils",
    ":analysis:analysis-test-framework",
    ":analysis:kt-references",
    ":analysis:kt-references:kt-references-fe10",
    ":analysis:symbol-light-classes",
    ":analysis:light-classes-base",
    ":analysis:project-structure",
    ":analysis:analysis-api-standalone",
    ":analysis:analysis-api-standalone:analysis-api-standalone-base",
    ":analysis:analysis-api-fe10",
    ":analysis:decompiled:decompiler-to-psi",
    ":analysis:decompiled:decompiler-to-stubs",
    ":analysis:decompiled:decompiler-to-file-stubs",
    ":analysis:decompiled:light-classes-for-decompiled",
    ":analysis:decompiled:light-classes-for-decompiled-fe10",
    ":prepare:analysis-api-test-framework"
)


if (buildProperties.isInJpsBuildIdeaSync) {
    include(":kotlin-stdlib:jps-build")
    project(":kotlin-stdlib:jps-build").projectDir = File("$rootDir/libraries/stdlib/jps-build")
} else {
    // modules that we are currently cannot compile with jps

    include(
        ":core:builtins",
        ":kotlin-stdlib-common",
        ":kotlin-stdlib",
        ":kotlin-stdlib-js",
        ":kotlin-stdlib-js-ir",
        ":kotlin-stdlib-js-ir-minimal-for-test",
        ":kotlin-stdlib-wasm",
        ":kotlin-stdlib-jdk7",
        ":kotlin-stdlib-jdk8",
        ":kotlin-stdlib:samples",
        ":kotlin-stdlib-jvm-minimal-for-test",
        ":tools:binary-compatibility-validator",
        ":tools:kotlin-stdlib-gen",

        ":kotlin-test",
        ":kotlin-test:kotlin-test-js",
        ":kotlin-test:kotlin-test-js-ir",
        ":kotlin-test:kotlin-test-js:kotlin-test-js-it",
        ":kotlin-test:kotlin-test-js-ir:kotlin-test-js-ir-it",
        ":kotlin-test:kotlin-test-wasm",
        ":native:native.tests"
    )

    project(":kotlin-stdlib-common").projectDir = File("$rootDir/libraries/stdlib/common")
    project(":kotlin-stdlib").projectDir = File("$rootDir/libraries/stdlib/jvm")
    project(":kotlin-stdlib-js").projectDir = File("$rootDir/libraries/stdlib/js-v1")
    project(":kotlin-stdlib-js-ir").projectDir = File("$rootDir/libraries/stdlib/js-ir")
    project(":kotlin-stdlib-wasm").projectDir = File("$rootDir/libraries/stdlib/wasm")
    project(":kotlin-stdlib-js-ir-minimal-for-test").projectDir = File("$rootDir/libraries/stdlib/js-ir-minimal-for-test")
    project(":kotlin-stdlib-jdk7").projectDir = File("$rootDir/libraries/stdlib/jdk7")
    project(":kotlin-stdlib-jdk8").projectDir = File("$rootDir/libraries/stdlib/jdk8")
    project(":kotlin-stdlib:samples").projectDir = File("$rootDir/libraries/stdlib/samples")
    project(":kotlin-stdlib-jvm-minimal-for-test").projectDir = File("$rootDir/libraries/stdlib/jvm-minimal-for-test")

    project(":tools:binary-compatibility-validator").projectDir = File("$rootDir/libraries/tools/binary-compatibility-validator")
    project(":tools:kotlin-stdlib-gen").projectDir = File("$rootDir/libraries/tools/kotlin-stdlib-gen")

    project(":kotlin-test").projectDir = File("$rootDir/libraries/kotlin.test")
    project(":kotlin-test:kotlin-test-js").projectDir = File("$rootDir/libraries/kotlin.test/js")
    project(":kotlin-test:kotlin-test-js-ir").projectDir = File("$rootDir/libraries/kotlin.test/js-ir")
    project(":kotlin-test:kotlin-test-js:kotlin-test-js-it").projectDir = File("$rootDir/libraries/kotlin.test/js/it")
    project(":kotlin-test:kotlin-test-js-ir:kotlin-test-js-ir-it").projectDir = File("$rootDir/libraries/kotlin.test/js-ir/it")
    project(":kotlin-test:kotlin-test-wasm").projectDir = File("$rootDir/libraries/kotlin.test/wasm")
    project(":native:native.tests").projectDir = File("$rootDir/native/native.tests")
}
include(":compiler:android-tests")

rootProject.name = "kotlin"

project(":kotlin-script-runtime").projectDir = File("$rootDir/libraries/tools/script-runtime")
project(":kotlin-test:kotlin-test-common").projectDir = File("$rootDir/libraries/kotlin.test/common")
project(":kotlin-test:kotlin-test-annotations-common").projectDir = File("$rootDir/libraries/kotlin.test/annotations-common")
project(":kotlin-test:kotlin-test-jvm").projectDir = File("$rootDir/libraries/kotlin.test/jvm")
project(":kotlin-test:kotlin-test-junit").projectDir = File("$rootDir/libraries/kotlin.test/junit")
project(":kotlin-test:kotlin-test-junit5").projectDir = File("$rootDir/libraries/kotlin.test/junit5")
project(":kotlin-test:kotlin-test-testng").projectDir = File("$rootDir/libraries/kotlin.test/testng")
project(":kotlin-test-js-runner").projectDir = File("$rootDir/libraries/tools/kotlin-test-js-runner")
project(":kotlin-reflect").projectDir = File("$rootDir/libraries/reflect")
project(":kotlin-reflect-api").projectDir = File("$rootDir/libraries/reflect/api")
project(":kotlinx-metadata").projectDir = File("$rootDir/libraries/kotlinx-metadata")
project(":kotlinx-metadata-jvm").projectDir = File("$rootDir/libraries/kotlinx-metadata/jvm")
project(":kotlinx-metadata-klib").projectDir = File("$rootDir/libraries/kotlinx-metadata/klib")
project(":kotlin-compiler").projectDir = File("$rootDir/prepare/compiler")
project(":kotlin-compiler-embeddable").projectDir = File("$rootDir/prepare/compiler-embeddable")
project(":kotlin-compiler-client-embeddable").projectDir = File("$rootDir/prepare/compiler-client-embeddable")
project(":kotlin-preloader").projectDir = File("$rootDir/compiler/preloader")
project(":kotlin-build-common").projectDir = File("$rootDir/build-common")
project(":compiler:cli-common").projectDir = File("$rootDir/compiler/cli/cli-common")
project(":compiler:cli-js").projectDir = File("$rootDir/compiler/cli/cli-js")
project(":kotlin-runner").projectDir = File("$rootDir/compiler/cli/cli-runner")
project(":kotlin-daemon").projectDir = File("$rootDir/compiler/daemon")
project(":daemon-common").projectDir = File("$rootDir/compiler/daemon/daemon-common")
project(":daemon-common-new").projectDir = File("$rootDir/compiler/daemon/daemon-common-new")
project(":kotlin-daemon-client").projectDir = File("$rootDir/compiler/daemon/daemon-client")
project(":kotlin-daemon-client-new").projectDir = File("$rootDir/compiler/daemon/daemon-client-new")
project(":kotlin-daemon-tests").projectDir = File("$rootDir/compiler/daemon/daemon-tests")
project(":kotlin-compiler-runner-unshaded").projectDir = File("$rootDir/compiler/compiler-runner-unshaded")
project(":kotlin-compiler-runner").projectDir = File("$rootDir/compiler/compiler-runner")
project(":kotlin-ant").projectDir = File("$rootDir/ant")
project(":compiler:ir.tree").projectDir = File("$rootDir/compiler/ir/ir.tree")
project(":compiler:ir.tree:tree-generator").projectDir = File("$rootDir/compiler/ir/ir.tree/tree-generator")
project(":compiler:ir.psi2ir").projectDir = File("$rootDir/compiler/ir/ir.psi2ir")
project(":compiler:ir.ir2cfg").projectDir = File("$rootDir/compiler/ir/ir.ir2cfg")
project(":compiler:ir.backend.common").projectDir = File("$rootDir/compiler/ir/backend.common")
project(":compiler:backend.js").projectDir = File("$rootDir/compiler/ir/backend.js")
project(":compiler:backend.wasm").projectDir = File("$rootDir/compiler/ir/backend.wasm")
project(":compiler:backend.jvm").projectDir = File("$rootDir/compiler/ir/backend.jvm")
project(":compiler:backend.jvm.lower").projectDir = File("$rootDir/compiler/ir/backend.jvm/lower")
project(":compiler:backend.jvm.codegen").projectDir = File("$rootDir/compiler/ir/backend.jvm/codegen")
project(":compiler:backend.jvm.entrypoint").projectDir = File("$rootDir/compiler/ir/backend.jvm/entrypoint")
project(":compiler:ir.serialization.common").projectDir = File("$rootDir/compiler/ir/serialization.common")
project(":compiler:ir.serialization.jvm").projectDir = File("$rootDir/compiler/ir/serialization.jvm")
project(":compiler:ir.serialization.js").projectDir = File("$rootDir/compiler/ir/serialization.js")
project(":compiler:ir.interpreter").projectDir = File("$rootDir/compiler/ir/ir.interpreter")
project(":kotlin-util-io").projectDir = File("$rootDir/compiler/util-io")
project(":kotlin-util-klib").projectDir = File("$rootDir/compiler/util-klib")
project(":kotlin-util-klib-metadata").projectDir = File("$rootDir/compiler/util-klib-metadata")
project(":native:kotlin-native-utils").projectDir = File("$rootDir/native/utils")
project(":native:frontend.native").projectDir = File("$rootDir/native/frontend")
project(":native:kotlin-klib-commonizer").projectDir = File("$rootDir/native/commonizer")
project(":native:kotlin-klib-commonizer-api").projectDir = File("$rootDir/native/commonizer-api")
project(":native:kotlin-klib-commonizer-embeddable").projectDir = File("$rootDir/native/commonizer-embeddable")
project(":plugins:android-extensions-compiler").projectDir = File("$rootDir/plugins/android-extensions/android-extensions-compiler")
project(":kotlin-android-extensions").projectDir = File("$rootDir/prepare/android-extensions-compiler-gradle")
project(":kotlin-parcelize-compiler").projectDir = File("$rootDir/prepare/parcelize-compiler-gradle")
project(":kotlin-android-extensions-runtime").projectDir = File("$rootDir/plugins/android-extensions/android-extensions-runtime")

project(":kotlin-allopen-compiler-plugin").projectDir = File("$rootDir/plugins/allopen")
project(":kotlin-allopen-compiler-plugin.common").projectDir = File("$rootDir/plugins/allopen/allopen.common")
project(":kotlin-allopen-compiler-plugin.k1").projectDir = File("$rootDir/plugins/allopen/allopen.k1")
project(":kotlin-allopen-compiler-plugin.k2").projectDir = File("$rootDir/plugins/allopen/allopen.k2")
project(":kotlin-allopen-compiler-plugin.cli").projectDir = File("$rootDir/plugins/allopen/allopen.cli")

project(":kotlin-lombok-compiler-plugin").projectDir = File("$rootDir/plugins/lombok")
project(":kotlin-lombok-compiler-plugin.cli").projectDir = File("$rootDir/plugins/lombok/lombok.cli")
project(":kotlin-lombok-compiler-plugin.k1").projectDir = File("$rootDir/plugins/lombok/lombok.k1")
project(":kotlin-lombok-compiler-plugin.k2").projectDir = File("$rootDir/plugins/lombok/lombok.k2")
project(":kotlin-lombok-compiler-plugin.common").projectDir = File("$rootDir/plugins/lombok/lombok.common")

project(":kotlin-noarg-compiler-plugin").projectDir = File("$rootDir/plugins/noarg")
project(":kotlin-noarg-compiler-plugin.common").projectDir = File("$rootDir/plugins/noarg/noarg.common")
project(":kotlin-noarg-compiler-plugin.k1").projectDir = File("$rootDir/plugins/noarg/noarg.k1")
project(":kotlin-noarg-compiler-plugin.k2").projectDir = File("$rootDir/plugins/noarg/noarg.k2")
project(":kotlin-noarg-compiler-plugin.backend").projectDir = File("$rootDir/plugins/noarg/noarg.backend")
project(":kotlin-noarg-compiler-plugin.cli").projectDir = File("$rootDir/plugins/noarg/noarg.cli")

project(":kotlin-sam-with-receiver-compiler-plugin").projectDir = File("$rootDir/plugins/sam-with-receiver")
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
project(":kotlin-project-model").projectDir = File("$rootDir/libraries/tools/kotlin-project-model")
project(":kotlin-project-model-tests-generator").projectDir = File("$rootDir/libraries/tools/kotlin-project-model-tests-generator")
project(":kotlin-gradle-compiler-types").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-compiler-types")
project(":kotlin-gradle-plugin-api").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-api")
project(":kotlin-gradle-plugin-annotations").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-annotations")
project(":kotlin-gradle-plugin-idea").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-idea")
project(":kotlin-gradle-plugin-idea-proto").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-idea-proto")
project(":kotlin-gradle-plugin-idea-for-compatibility-tests").projectDir =
    File("$rootDir/libraries/tools/kotlin-gradle-plugin-idea-for-compatibility-tests")
project(":kotlin-gradle-plugin-dsl-codegen").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-dsl-codegen")
project(":kotlin-gradle-plugin-npm-versions-codegen").projectDir =
    File("$rootDir/libraries/tools/kotlin-gradle-plugin-npm-versions-codegen")
project(":kotlin-gradle-statistics").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-statistics")
project(":kotlin-gradle-build-metrics").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-build-metrics")
project(":kotlin-gradle-plugin").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin")
project(":kotlin-gradle-plugin-kpm-android").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-kpm-android")
project(":kotlin-gradle-plugin-tcs-android").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-tcs-android")
project(":kotlin-gradle-plugin-model").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-model")
project(":kotlin-gradle-plugin-test-utils-embeddable").projectDir =
    File("$rootDir/libraries/tools/kotlin-gradle-plugin-test-utils-embeddable")
project(":kotlin-gradle-plugin-integration-tests").projectDir = File("$rootDir/libraries/tools/kotlin-gradle-plugin-integration-tests")
project(":gradle:android-test-fixes").projectDir = File("$rootDir/libraries/tools/gradle/android-test-fixes")
project(":gradle:regression-benchmark-templates").projectDir = File("$rootDir/libraries/tools/gradle/regression-benchmark-templates")
project(":gradle:regression-benchmarks").projectDir = File("$rootDir/libraries/tools/gradle/regression-benchmarks")
project(":kotlin-tooling-metadata").projectDir = File("$rootDir/libraries/tools/kotlin-tooling-metadata")
project(":kotlin-tooling-core").projectDir = File("$rootDir/libraries/tools/kotlin-tooling-core")
project(":kotlin-allopen").projectDir = File("$rootDir/libraries/tools/kotlin-allopen")
project(":kotlin-noarg").projectDir = File("$rootDir/libraries/tools/kotlin-noarg")
project(":kotlin-sam-with-receiver").projectDir = File("$rootDir/libraries/tools/kotlin-sam-with-receiver")
project(":kotlin-assignment").projectDir = File("$rootDir/libraries/tools/kotlin-assignment")
project(":kotlin-lombok").projectDir = File("$rootDir/libraries/tools/kotlin-lombok")
project(":kotlin-gradle-subplugin-example").projectDir = File("$rootDir/libraries/examples/kotlin-gradle-subplugin-example")
project(":examples:annotation-processor-example").projectDir = File("$rootDir/libraries/examples/annotation-processor-example")
project(":kotlin-script-util").projectDir = File("$rootDir/libraries/tools/kotlin-script-util")
project(":kotlin-annotation-processing-gradle").projectDir = File("$rootDir/libraries/tools/kotlin-annotation-processing")
project(":kotlin-annotation-processing-embeddable").projectDir = File("$rootDir/prepare/kotlin-annotation-processing-embeddable")
project(":kotlin-daemon-embeddable").projectDir = File("$rootDir/prepare/kotlin-daemon-embeddable")
project(":kotlin-annotation-processing").projectDir = File("$rootDir/plugins/kapt3/kapt3-compiler")
project(":kotlin-annotation-processing-cli").projectDir = File("$rootDir/plugins/kapt3/kapt3-cli")
project(":kotlin-annotation-processing-base").projectDir = File("$rootDir/plugins/kapt3/kapt3-base")
project(":kotlin-annotation-processing-runtime").projectDir = File("$rootDir/plugins/kapt3/kapt3-runtime")
project(":examples:kotlin-jsr223-local-example").projectDir = File("$rootDir/libraries/examples/kotlin-jsr223-local-example")
project(":examples:kotlin-jsr223-daemon-local-eval-example").projectDir =
    File("$rootDir/libraries/examples/kotlin-jsr223-daemon-local-eval-example")
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
project(":pill:pill-importer").projectDir = File("$rootDir/plugins/pill/pill-importer")
project(":pill:generate-all-tests").projectDir = File("$rootDir/plugins/pill/generate-all-tests")
project(":kotlin-imports-dumper-compiler-plugin").projectDir = File("$rootDir/plugins/imports-dumper")
project(":libraries:kotlin-prepush-hook").projectDir = File("$rootDir/libraries/tools/kotlin-prepush-hook")
project(":plugins:jvm-abi-gen").projectDir = File("$rootDir/plugins/jvm-abi-gen")
project(":plugins:jvm-abi-gen-embeddable").projectDir = File("$rootDir/plugins/jvm-abi-gen/embeddable")

project(":js:js.tests").projectDir = File("$rootDir/js/js.tests")
project(":js:js.engines").projectDir = File("$rootDir/js/js.engines")

project(":kotlinx-serialization-compiler-plugin").projectDir = File("$rootDir/plugins/kotlinx-serialization")
project(":kotlinx-serialization-compiler-plugin.cli").projectDir = File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.cli")
project(":kotlinx-serialization-compiler-plugin.backend").projectDir =
    File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.backend")
project(":kotlinx-serialization-compiler-plugin.k1").projectDir = File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.k1")
project(":kotlinx-serialization-compiler-plugin.k2").projectDir = File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.k2")
project(":kotlinx-serialization-compiler-plugin.common").projectDir =
    File("$rootDir/plugins/kotlinx-serialization/kotlinx-serialization.common")
project(":kotlin-serialization").projectDir = File("$rootDir/libraries/tools/kotlin-serialization")
project(":kotlin-serialization-unshaded").projectDir = File("$rootDir/libraries/tools/kotlin-serialization-unshaded")

project(":kotlinx-atomicfu-compiler-plugin").projectDir = File("$rootDir/plugins/atomicfu/atomicfu-compiler")
project(":kotlinx-atomicfu-runtime").projectDir = File("$rootDir/plugins/atomicfu/atomicfu-runtime")
project(":atomicfu").projectDir = File("$rootDir/libraries/tools/atomicfu")

project(":kotlin-scripting-ide-services-unshaded").projectDir = File("$rootDir/plugins/scripting/scripting-ide-services")
project(":kotlin-scripting-ide-services-test").projectDir = File("$rootDir/plugins/scripting/scripting-ide-services-test")
project(":kotlin-scripting-ide-services").projectDir = File("$rootDir/plugins/scripting/scripting-ide-services-embeddable")
project(":kotlin-scripting-ide-common").projectDir = File("$rootDir/plugins/scripting/scripting-ide-common")

// Uncomment to use locally built protobuf-relocated
// includeBuild("dependencies/protobuf")
if (buildProperties.isKotlinNativeEnabled) {
    include(":kotlin-native:dependencies")
    include(":kotlin-native:endorsedLibraries:kotlinx.cli")
    include(":kotlin-native:endorsedLibraries")
    include(":kotlin-native:Interop:StubGenerator")
    include(":kotlin-native:backend.native")
    include(":kotlin-native:Interop:Runtime")
    include(":kotlin-native:Interop:Indexer")
    include(":kotlin-native:Interop:JsRuntime")
    include(":kotlin-native:Interop:Skia")
    include(":kotlin-native:utilities:basic-utils")
    include(":kotlin-native:utilities:cli-runner")
    include(":kotlin-native:klib")
    include(":kotlin-native:common")
    include(":kotlin-native:runtime")
    include(":kotlin-native:libllvmext")
    include(":kotlin-native:llvmDebugInfoC")
    include(":kotlin-native:utilities")
    include(":kotlin-native:platformLibs")
    include(":kotlin-native:libclangext")
    include(":kotlin-native:backend.native:tests")
    include(":kotlin-native-compiler-embeddable")
    project(":kotlin-native-compiler-embeddable").projectDir = File("$rootDir/kotlin-native/prepare/kotlin-native-embeddable-compiler")
    include(":kotlin-native:build-tools")
    include(":kotlin-native-shared")
    project(":kotlin-native-shared").projectDir = File("$rootDir/kotlin-native/shared")
}
