import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
}

description = "Kotlin Test for JS"
base.archivesName = "kotlin-test-js"

val commonMainSources by task<Sync> {
    from(
        "$rootDir/libraries/kotlin.test/common/src/main/kotlin",
        "$rootDir/libraries/kotlin.test/annotations-common/src/main/kotlin"
    )
    into("$buildDir/commonMainSources")
}

val commonTestSources by task<Sync> {
    from("$rootDir/libraries/kotlin.test/common/src/test/kotlin")
    into("$buildDir/commonTestSources")
}

val jsMainSources by task<Sync> {
    from("$rootDir/libraries/kotlin.test/js/src/main/kotlin")
    into("$buildDir/jsMainSources")
}

kotlin {
    js(IR) {
        nodejs()
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(kotlinStdlib())
            }
            kotlin.srcDir(commonMainSources)
        }
        named("commonTest") {
            kotlin.srcDir(commonTestSources)
        }
        named("jsMain") {
            kotlin.srcDir(jsMainSources)
        }
    }
}

tasks.withType<KotlinCompile<*>>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-opt-in=kotlin.ExperimentalMultiplatform",
        "-opt-in=kotlin.contracts.ExperimentalContracts",
        "-Xexpect-actual-classes"
    )
}

tasks.named("compileKotlinJs") {
    (this as KotlinCompile<*>).kotlinOptions.freeCompilerArgs += "-Xir-module-name=kotlin-test"
}

@Suppress("UNUSED_VARIABLE")
tasks {
    val jsJar by existing(Jar::class) {
        archiveAppendix = null
        manifestAttributes(manifest, "Test")
    }
    val jsSourcesJar by existing(org.gradle.jvm.tasks.Jar::class) {
        archiveAppendix = null
    }
}