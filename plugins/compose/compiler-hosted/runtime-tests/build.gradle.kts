import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
}

repositories {
    if (!kotlinBuildProperties.isTeamcityBuild) {
        androidXMavenLocal(androidXMavenLocalPath)
    }
    androidxSnapshotRepo(libs.versions.compose.snapshot.id.get())
    composeGoogleMaven(libs.versions.compose.stable.get())
}

kotlin {
    jvm()

    jvmToolchain(11)

    sourceSets {
        commonTest.dependencies {
            implementation(project(":kotlin-stdlib-common"))
            implementation(kotlinTest("junit"))
        }

        val jvmTest by getting {
            dependsOn(commonTest.get())

            dependencies {
                // junit
                implementation(libs.junit4)
                implementation(project.dependencies.platform(libs.junit.bom))
                implementation(libs.junit.jupiter.api)
                runtimeOnly(libs.junit.jupiter.engine)

                // kotlin deps
                implementation(project(":kotlin-stdlib"))

                // coroutines
                implementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-test-jvm"))

                // external deps
                implementation(composeRuntime())
                implementation(composeRuntimeTestUtils())
            }
        }
    }
}

val reportRuntimeTests = tasks.register("reportRunningRuntimeTest") {
    doFirst {
        error(
            """
            The runtime tests are executed as part of integration-tests for now, as Kotlin repo does not support compiler dependency built from source.
            Use `androidx.compose.compiler.plugins.kotlin.RuntimeTests` to verify runtime tests.
            """.trimIndent()
        )
    }
}

// Hack to support editing these tests in IDE while running with the latest version of compiler/plugin.
// Compilation of this module should not be allowed since bootstrap compiler cannot compile it properly.
// The tests are executed with a custom test runner from integration-tests module instead.
// (see androidx.compose.compiler.plugins.kotlin.RuntimeTests)

tasks.named("jvmTestClasses") {
    if (!kotlinBuildProperties.isTeamcityBuild) {
        dependsOn(reportRuntimeTests)
    }
}

tasks.withType(KotlinCompile::class.java) {
    enabled = false
    if (!kotlinBuildProperties.isTeamcityBuild) {
        dependsOn(reportRuntimeTests)
    }
}

tasks.withType(KotlinJvmTest::class.java) {
    if (!kotlinBuildProperties.isTeamcityBuild) {
        dependsOn(reportRuntimeTests)
    }
    this.enabled = false
}