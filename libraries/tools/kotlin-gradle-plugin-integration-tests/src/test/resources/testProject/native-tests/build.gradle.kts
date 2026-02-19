import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0"

kotlin {
    <SingleNativeTarget>("host") {
        val anotherTestComp = compilations.create("anotherTest")
        binaries {
            test("another", listOf(DEBUG)) {
                compilation = anotherTestComp
            }
        }
    }

    iosX64("ios")
    iosSimulatorArm64("iosArm64")

    sourceSets {
        val anotherTest by creating
        val hostAnotherTest by getting {
            dependsOn(anotherTest)
        }
    }
}

tasks.withType<KotlinNativeTest>().configureEach {
    trackEnvironment("ANDROID_HOME")

    // Check that test events are correctly reported in CLI.
    testLogging {
         events(
             "PASSED",
             "SKIPPED",
             "FAILED",
         )
    }
}

kotlin.targets.named<KotlinNativeTarget>("host").configure {
    println("Get test: ${binaries.getTest(DEBUG).outputFile.name}")
    println("Find test: ${binaries.findTest(DEBUG)?.outputFile?.name}")
    println("Get test: ${binaries.getTest("another", DEBUG).outputFile.name}")
    println("Find test: ${binaries.findTest("another", DEBUG)?.outputFile?.name}")
}
