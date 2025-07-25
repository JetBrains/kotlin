plugins {
    kotlin("jvm")
    id("compiler-tests-convention")
}

repositories {
    if (!kotlinBuildProperties.isTeamcityBuild) {
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

dependencies {
    implementation(project(":kotlin-stdlib"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:cli-base"))
    compileOnly(project(":compiler:ir.serialization.js"))
    compileOnly(project(":compiler:backend.jvm.codegen"))
    compileOnly(project(":compiler:fir:entrypoint"))

    compileOnly(intellijCore())

    testCompileOnly(project(":compiler:ir.tree"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(testFixtures(project(":analysis:analysis-api-fe10")))
    testImplementation(testFixtures(project(":analysis:analysis-api-fir")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:low-level-api-fir")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(testFixtures(project(":generators:analysis-api-generator")))
    testApi(project(":compiler:plugin-api"))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))

    // runtime tests
    testImplementation(composeRuntime()) { isTransitive = false }
    testImplementation(composeRuntimeTestUtils()) { isTransitive = false }
    testImplementation(composeRuntimeAnnotations()) { isTransitive = false }
    testImplementation(libs.androidx.collections)

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

    testApi(platform(libs.junit.bom))
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

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        dependsOn(runtimeJar)
        systemProperty("compose.compiler.hosted.jar.path", runtimeJar.get().outputs.files.singleFile.relativeTo(rootDir))
        workingDir = rootDir
    }

    testGenerator("androidx.compose.compiler.plugins.kotlin.TestGeneratorKt")
}
testsJar()
