import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
}

repositories {
    if (!kotlinBuildProperties.isTeamcityBuild.get()) {
        androidXMavenLocal(androidXMavenLocalPath)
    }
    composeGoogleMaven(libs.versions.compose.stable.get())
    androidxSnapshotRepo(composeRuntimeSnapshot.versions.snapshot.id.get())
}

kotlin {
    jvm()

    jvmToolchain(11)

    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-Xexpect-actual-classes",
            )
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(kotlinTest("junit"))
            // external deps
            implementation(composeRuntime()) { isTransitive = false }
            implementation(composeRuntimeAnnotations()) { isTransitive = false }
            implementation(libs.androidx.collections)
        }

        jvmMain.dependencies {
            // coroutines
            implementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-test-jvm"))
        }
    }
}

