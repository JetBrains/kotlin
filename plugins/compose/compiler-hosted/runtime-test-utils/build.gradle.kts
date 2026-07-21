import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
    kotlin("plugin.compose")
}

repositories {
    if (!kotlinBuildProperties.isTeamcityBuild.get()) {
        androidXMavenLocal(androidXMavenLocalPath)
    }
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
            implementation(kotlinTest("junit5"))
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

