import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

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

fun KotlinDependencyHandler.implementationArtifactOnly(dependency: String) {
    implementation(dependency) {
        isTransitive = false
    }
}

optInToObsoleteDescriptorBasedAPI()

kotlin {
    jvmToolchain(11)

    jvm()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
    }

    sourceSets {
        commonTest.dependencies {
            implementation(project(":kotlin-stdlib-common"))
            implementation(kotlinTest("junit"))
        }

        jvmTest.configure {
            dependsOn(commonTest.get())

            dependencies {
                // junit
                implementation(libs.junit4)
                implementation(project.dependencies.platform(libs.junit.bom))
                implementation(libs.junit.jupiter.api)
                runtimeOnly(libs.junit.jupiter.engine)


                runtimeOnly(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
                runtimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
                runtimeOnly(jpsModelImpl())
                implementation(project(":compiler:backend-common"))
                implementation(project(":compiler:ir.backend.common"))
                implementation(project(":compiler:cli"))
                implementation(project(":compiler:backend.jvm"))
                implementation(project(":compiler:fir:fir2ir:jvm-backend"))
                implementation(project(":compiler:backend.jvm.entrypoint"))
                implementation(intellijCore())

                // kotlin deps
                implementation(project(":kotlin-stdlib"))
                implementation(project(":kotlin-reflect"))
                implementation(project(":kotlin-metadata-jvm"))

                // Compose compiler deps
                implementation(project(":plugins:compose-compiler-plugin:compiler-hosted"))
                implementation(project(":plugins:compose-compiler-plugin:compiler-hosted:integration-tests:protobuf-test-classes"))

                // coroutines for runtime tests
                implementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-test-jvm"))

                // runtime tests
                implementation(composeRuntime())
                implementation(composeRuntimeTestUtils())

                // other compose
                implementationArtifactOnly(compose("foundation", "foundation"))
                implementationArtifactOnly(compose("foundation", "foundation-layout"))
                implementationArtifactOnly(compose("animation", "animation"))
                implementationArtifactOnly(compose("ui", "ui"))
                implementationArtifactOnly(compose("ui", "ui-graphics"))
                implementationArtifactOnly(compose("ui", "ui-text"))
                implementationArtifactOnly(compose("ui", "ui-unit"))

                // external
                implementationArtifactOnly(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm"))
                implementationArtifactOnly("com.google.dagger:dagger:2.40.1")
            }
        }
    }
}

tasks.withType(Test::class.java).configureEach {
    this.workingDir = rootDir
    this.maxHeapSize = "1024m"
    this.jvmArgs("--add-opens=jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED")
    // ensure that debugger tests don't launch a separate window
    this.systemProperty("java.awt.headless", "true")
    // runtime tests are executed in this module with compiler built from source (see androidx.compose.compiler.plugins.kotlin.RuntimeTests)
    this.inputs.dir(File(rootDir, "plugins/compose/compiler-hosted/runtime-tests/src")).withPathSensitivity(PathSensitivity.RELATIVE)
}