plugins {
    kotlin("multiplatform")
}

// WARNING: This module is attached to the project ONLY during the IDEA import.
//
// This module serves primarily as test data for `androidx.compose.compiler.plugins.kotlin.RuntimeTests`.
// It improves the file editing experience but files may contain compilation errors because the module isn't expected to be compiled
// by the bootstrap compiler.
//
// Attempting to run any tasks in this module, or adding it as a dependency, will cause ERRORS:
//
// Error: Cannot locate tasks matching `:plugins:compose-compiler-plugin:compiler-hosted:runtime-tests:*`
// or
// Error: Project directory '?/plugins/compose/compiler-hosted/runtime-tests' is not part of the build defined by the settings file

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