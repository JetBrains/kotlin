/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute

private fun Project.createCommonMainSources() = tasks.register("commonMainSources", Sync::class.java) {
    from(
        "$rootDir/libraries/kotlin.test/common/src",
        "$rootDir/libraries/kotlin.test/annotations-common/src",
        "$rootDir/libraries/kotlin.test/wasm/src"
    )
    into("$buildDir/commonMainSources")
}

fun Project.configureWasmKotlinTest(
    wasmTargetParameter: String,
    wasmTargetAttribute: KotlinWasmTargetAttribute,
    targetSourceDir: String,
    stdDependencyName: String,
    withKotlinMPP: (KotlinMultiplatformExtension.() -> Unit) -> Unit
) {
    val commonMainSources = createCommonMainSources()

    withKotlinMPP {
        @Suppress("DEPRECATION")
        @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
        wasm("wasm") {
            nodejs()
            attributes {
                attribute(KotlinWasmTargetAttribute.wasmTargetAttribute, wasmTargetAttribute)
            }
        }

        sourceSets {
            sourceSets.named("commonMain") {
                dependencies {
                    api(project(stdDependencyName))
                }
                kotlin.srcDir(commonMainSources.get().destinationDir)
            }

            sourceSets.named("wasmMain") {
                dependencies {
                    api(project(stdDependencyName))
                }
                kotlin.srcDir(targetSourceDir)
            }
        }

        tasks.register<Jar>("sourcesJar") {
            dependsOn(commonMainSources)
            archiveClassifier.set("sources")
            from(sourceSets["commonMain"].kotlin)
            from(sourceSets["wasmMain"].kotlin)
        }
    }

    tasks.withType<KotlinCompile<*>>().configureEach {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xallow-kotlin-package",
            "-opt-in=kotlin.ExperimentalMultiplatform",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-Xwasm-target=$wasmTargetParameter"
        )
    }

    tasks.named("compileKotlinWasm", KotlinCompile::class.java) {
        kotlinOptions.freeCompilerArgs += "-Xir-module-name=kotlin-test"
        dependsOn(commonMainSources)
    }

    tasks.register<Jar>("emptyJavadocJar") {
        archiveClassifier.set("javadoc")
    }
}