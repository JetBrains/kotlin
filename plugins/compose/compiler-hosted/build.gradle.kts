import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("jvm")
    id("d8-configuration")
    id("project-tests-convention")
    id("test-inputs-check")
}

repositories {
    if (!kotlinBuildProperties.isTeamcityBuild.get()) {
        androidXMavenLocal(androidXMavenLocalPath)
    }
    androidxSnapshotRepo(composeRuntimeSnapshot.versions.snapshot.id.get())
    composeGoogleMaven(libs.versions.compose.stable.get())
}

fun DependencyHandler.testImplementationArtifactOnly(dependency: String) {
    testImplementation(dependency) {
        isTransitive = false
    }
}

description = "Contains the Kotlin compiler plugin for Compose used in Android Studio and IDEA"

val testJsRuntime: Configuration by configurations.creating {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

dependencies {
    implementation(project(":kotlin-stdlib"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:cli-base"))
    compileOnly(project(":compiler:ir.serialization.js"))
    compileOnly(project(":compiler:backend.jvm.codegen"))
    compileOnly(project(":compiler:fir:diagnostic-renderers"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":native:native.config"))

    compileOnly(intellijCore())

    testCompileOnly(project(":compiler:ir.tree"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(testFixtures(project(":analysis:analysis-api-fir")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:low-level-api-fir")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(project(":compiler:plugin-api"))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(testFixtures(project(":js:js.tests")))


    testImplementation(testFixtures(project(":kotlinx-serialization-compiler-plugin")))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    // compose runtime for tests
    testImplementation(composeRuntime()) { isTransitive = false }
    testImplementation(composeRuntimeAnnotations()) { isTransitive = false }
    testImplementation(libs.androidx.collections)

    // js runtimes for tests
    testJsRuntime(composeRuntime()) { isTransitive = false }
    testJsRuntime(composeRuntimeAnnotations()) { isTransitive = false }
    testJsRuntime(libs.androidx.collections) {
        // Avoid kotlin stdlib dependency since we are compiling against the newest one
        exclude(group = "org.jetbrains.kotlin")
    }
    testJsRuntime(libs.kotlinx.coroutines.core) { isTransitive = false }
    testJsRuntime("org.jetbrains.kotlinx:atomicfu-js:0.25.0") { isTransitive = false }

    // other compose
    testImplementationArtifactOnly(compose("foundation", "foundation"))
    testImplementationArtifactOnly(compose("foundation", "foundation-layout"))
    testImplementationArtifactOnly(compose("animation", "animation"))
    testImplementationArtifactOnly(compose("ui", "ui"))
    testImplementationArtifactOnly(compose("ui", "ui-graphics"))
    testImplementationArtifactOnly(compose("ui", "ui-text"))
    testImplementationArtifactOnly(compose("ui", "ui-unit"))

    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

optInToUnsafeDuringIrConstructionAPI()
optInToObsoleteDescriptorBasedAPI()

kotlin {
    jvmToolchain(8)
}

sourceSets {
    "test" {
        generatedTestDir()
    }
}

base {
    archivesName = "kotlin-compose-compiler-plugin"
}

publish {
    artifactId = "kotlin-compose-compiler-plugin"
    pom {
        name.set("AndroidX Compose Hosted Compiler Plugin")
        developers {
            developer {
                name.set("The Android Open Source Project")
            }
        }
    }
}

val runtimeJar = runtimeJar()
sourcesJar()
javadocJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0)) {
        addClasspathProperty(runtimeJar.get().outputs.files, "compose.compiler.hosted.jar.path")
        addClasspathProperty(testJsRuntime, "compose.compiler.test.js.classpath")
        useJsIrBoxTests(buildDir = layout.buildDirectory)

        testInputsCheck {
            allowFlightRecorder.set(true)
        }
    }

    testGenerator("androidx.compose.compiler.plugins.kotlin.TestGeneratorKt", doNotSetFixturesSourceSetDependency = true)

    testData(isolated, "testData")
    testData(project(":js:js.translator").isolated, "testData/_commonFiles")

    withJvmStdlibAndReflect()
    withJsRuntime()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()
}

testsJar()
