import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink

plugins {
    `maven-publish`
    kotlin("multiplatform")
}

description = "Kotlin Standard Library for experimental WebAssembly platform"

val unimplementedNativeBuiltIns =
    (file("$rootDir/core/builtins/native/kotlin/").list().toSortedSet() - file("$rootDir/libraries/stdlib/wasm/builtins/kotlin/").list())
        .map { "core/builtins/native/kotlin/$it" }



val builtInsSources by task<Sync> {
    val sources = listOf(
        "core/builtins/src/kotlin/"
    ) + unimplementedNativeBuiltIns

    val excluded = listOf(
        // JS-specific optimized version of emptyArray() already defined
        "ArrayIntrinsics.kt",
        // Included with K/N collections
        "Collections.kt", "Iterator.kt", "Iterators.kt"
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
            excluded.forEach {
                exclude(it)
            }
        }
    }

    into("$buildDir/builtInsSources")
}

val commonMainSources by task<Sync> {
    val sources = listOf(
        "libraries/stdlib/common/src/",
        "libraries/stdlib/src/kotlin/",
        "libraries/stdlib/unsigned/"
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
        }
    }

    into("$buildDir/commonMainSources")

    dependsOn(":prepare:build.version:writeStdlibVersion")
}

val commonTestSources by task<Sync> {
    val sources = listOf(
        "libraries/stdlib/test/",
        "libraries/stdlib/common/test/"
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
            // exclusions due to KT-51647
            exclude("generated/minmax")
            exclude("collections/MapTest.kt")
        }
    }

    into("$buildDir/commonTestSources")
}

D8RootPlugin.apply(rootProject).version = v8Version

kotlin {
    wasm {
        d8()
    }

    sourceSets {
        val wasmMain by getting {
            kotlin.srcDirs("builtins", "internal", "runtime", "src", "stubs")
            kotlin.srcDirs("$rootDir/libraries/stdlib/native-wasm/src")
            kotlin.srcDirs(files(builtInsSources.map { it.destinationDir }))
        }

        val commonMain by getting {
            kotlin.srcDirs(files(commonMainSources.map { it.destinationDir }))
        }

        val commonTest by getting {
            dependencies {
                api(project(":kotlin-test:kotlin-test-wasm"))
            }
            kotlin.srcDir(files(commonTestSources.map { it.destinationDir }))
        }

        val wasmTest by getting {
            dependencies {
                api(project(":kotlin-test:kotlin-test-wasm"))
            }
            kotlin.srcDir("$rootDir/libraries/stdlib/wasm/test/")
            kotlin.srcDir("$rootDir/libraries/stdlib/native-wasm/test/")
        }
    }
}

tasks.named("compileKotlinWasm", KotlinCompile::class) {
    // TODO: enable explicit API mode
    kotlinOptions.allWarningsAsErrors = true
}

tasks.named("compileTestKotlinWasm", KotlinCompile::class) {
    // TODO: fix all warnings, enable and -Werror
    kotlinOptions.suppressWarnings = true
}

tasks.withType<KotlinCompile<*>>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-opt-in=kotlin.ExperimentalMultiplatform",
        "-opt-in=kotlin.contracts.ExperimentalContracts",
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=kotlin.ExperimentalUnsignedTypes",
        "-opt-in=kotlin.ExperimentalStdlibApi",
        "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi",
        "-XXLanguage:+RangeUntilOperator",
    )
}

tasks.named("compileKotlinWasm") {
    (this as KotlinCompile<*>).kotlinOptions.freeCompilerArgs += "-Xir-module-name=kotlin"
    dependsOn(commonMainSources)
    dependsOn(builtInsSources)
}

val compileTestKotlinWasm by tasks.existing(KotlinCompile::class) {
    val sources: FileCollection = kotlin.sourceSets["commonTest"].kotlin
    doFirst {
        // Note: common test sources are copied to the actual source directory by commonMainSources task,
        // so can't do this at configuration time:
        kotlinOptions.freeCompilerArgs += listOf("-Xcommon-sources=${sources.joinToString(",")}")
    }
}

val compileTestDevelopmentExecutableKotlinWasm = tasks.named<KotlinJsIrLink>("compileTestDevelopmentExecutableKotlinWasm") {
    (this as KotlinCompile<*>).kotlinOptions.freeCompilerArgs += listOf("-Xwasm-enable-array-range-checks")
}

val runtimeElements by configurations.creating {}
val apiElements by configurations.creating {}

publish {
    pom.packaging = "klib"
    org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories.getProvider(project)
    artifact(tasks.named("wasmJar")) {
        extension = "klib"
    }
}

afterEvaluate {
    // cleanup default publications
    // TODO: remove after mpp plugin allows avoiding their creation at all, KT-29273
    publishing {
        publications.removeAll { it.name != "Main" }
    }

    tasks.withType<AbstractPublishToMaven> {
        if (publication.name != "Main") this.enabled = false
    }

    tasks.named("publish") {
        doFirst {
            publishing.publications {
                if (singleOrNull()?.name != "Main") {
                    throw GradleException("kotlin-stdlib-wasm should have only one publication, found $size: ${joinToString { it.name }}")
                }
            }
        }
    }
}

