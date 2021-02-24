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

val jsMainSources by task<Sync> {
    from("$rootDir/libraries/kotlin.test/js/src")
    into("$buildDir/jsMainSources")
}

kotlin {
    js(IR) {
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlin-stdlib-js-ir"))
            }
            kotlin.srcDir(commonMainSources.get().destinationDir)
        }
        val jsMain by getting {
            dependencies {
                api(project(":kotlin-stdlib-js-ir"))
            }
            kotlin.srcDir(jsMainSources.get().destinationDir)
        }
    }
}

tasks.withType<KotlinCompile<*>>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-Xallow-result-return-type",
        "-Xuse-experimental=kotlin.Experimental",
        "-Xuse-experimental=kotlin.ExperimentalMultiplatform",
        "-Xuse-experimental=kotlin.contracts.ExperimentalContracts",
        "-Xinline-classes"
    )
}

tasks.named("compileKotlinJs") {
    (this as KotlinCompile<*>).kotlinOptions.freeCompilerArgs += "-Xir-module-name=kotlin-test"
    dependsOn(commonMainSources)
    dependsOn(jsMainSources)
}

