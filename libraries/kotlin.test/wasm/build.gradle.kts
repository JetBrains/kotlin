import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
}

val commonMainSources by task<Sync> {
    from(
        "$rootDir/libraries/kotlin.test/common/src",
        "$rootDir/libraries/kotlin.test/annotations-common/src"
    )
    into("$buildDir/commonMainSources")
}

kotlin {
    @Suppress("DEPRECATION")
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasm("wasm") {
        nodejs()
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":kotlin-stdlib-wasm-js"))
            }
            kotlin.srcDir(commonMainSources.get().destinationDir)
        }

        named("wasmMain") {
            dependencies {
                api(project(":kotlin-stdlib-wasm-js"))
            }
            kotlin.srcDirs("$rootDir/libraries/kotlin.test/wasm/src")
            kotlin.srcDirs("$rootDir/libraries/kotlin.test/wasm/js/main/kotlin/kotlin/test")
        }
    }
}

tasks.withType<KotlinCompile<*>>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-opt-in=kotlin.ExperimentalMultiplatform",
        "-opt-in=kotlin.contracts.ExperimentalContracts",
        "-Xwasm-target=wasm-js"
    )
}

tasks.named("compileKotlinWasm", KotlinCompile::class.java) {
    kotlinOptions.freeCompilerArgs += "-Xir-module-name=kotlin-test"
    dependsOn(commonMainSources)
}

tasks.register<Jar>("sourcesJar") {
    dependsOn(commonMainSources)
    archiveClassifier.set("sources")
    from(kotlin.sourceSets["commonMain"].kotlin)
    from(kotlin.sourceSets["wasmMain"].kotlin)
}

tasks.register<Jar>("emptyJavadocJar") {
    archiveClassifier.set("javadoc")
}
