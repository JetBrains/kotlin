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
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasm {
        nodejs()
    }

    sourceSets {
         named("commonMain") {
            dependencies {
                api(project(":kotlin-stdlib-wasm"))
            }
            kotlin.srcDir(commonMainSources.get().destinationDir)
        }

        named("wasmMain") {
            dependencies {
                api(project(":kotlin-stdlib-wasm"))
            }
            kotlin.srcDir("$rootDir/libraries/kotlin.test/wasm/src")
        }
    }
}

tasks.withType<KotlinCompile<*>>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-opt-in=kotlin.ExperimentalMultiplatform",
        "-opt-in=kotlin.contracts.ExperimentalContracts"
    )
}

tasks.named("compileKotlinWasm") {
    (this as KotlinCompile<*>).kotlinOptions.freeCompilerArgs += "-Xir-module-name=kotlin-test"
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
