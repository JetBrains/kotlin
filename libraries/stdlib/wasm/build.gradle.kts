import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

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
        "core/builtins/src/kotlin/ArrayIntrinsics.kt"
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
            excluded.filter { it.startsWith(path) }.forEach {
                exclude(it.substring(path.length))
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
}

kotlin {
    js(IR) {
        nodejs()
    }
    sourceSets {
        val jsMain by getting {
            kotlin.srcDirs("builtins", "internal", "runtime", "src", "stubs")
            kotlin.srcDirs(files(builtInsSources.map { it.destinationDir }))
        }

        val commonMain by getting {
            kotlin.srcDirs(files(commonMainSources.map { it.destinationDir }))
        }
    }
}

tasks.withType<KotlinCompile<*>>().configureEach {
    // TODO: fix all warnings, enable explicit API mode and -Werror
    kotlinOptions.suppressWarnings = true

    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-Xopt-in=kotlin.ExperimentalMultiplatform",
        "-Xopt-in=kotlin.contracts.ExperimentalContracts",
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
        "-Xopt-in=kotlin.ExperimentalStdlibApi"
    )
}

tasks.named("compileKotlinJs") {
    (this as KotlinCompile<*>).kotlinOptions.freeCompilerArgs += "-Xir-module-name=kotlin"
    dependsOn(commonMainSources)
    dependsOn(builtInsSources)
}

val runtimeElements by configurations.creating {}
val apiElements by configurations.creating {}

publish {
    pom.packaging = "klib"
    artifact(tasks.named("jsJar")) {
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

