import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("jvm")
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
}


// WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
val nativeTargetName = HostManager.host.name
val sandboxAnnotationsNativeRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val sandboxPluginForTests by configurations.creating

dependencies {
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:plugin-utils"))
    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    testFixturesApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests")))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesApi(project(":compiler:fir:checkers"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.jvm"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.js"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.native"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.wasm"))
    testFixturesApi(project(":compiler:fir:plugin-utils"))
    testFixturesImplementation(testFixtures(project(":tools:kotlinp-jvm")))

    testFixturesApi(testFixtures(project(":native:native.tests")))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(intellijJDom())
    testRuntimeOnly(libs.intellij.fastutil)

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testRuntimeOnly(toolsJar())

    sandboxAnnotationsNativeRuntimeForTests(project(":plugins:plugin-sandbox:plugin-annotations"))
    sandboxPluginForTests(project(":plugins:plugin-sandbox"))
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {
        generatedTestDir()
    }
    "testFixtures" {
        projectDefault()
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir
        useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)
        useJUnitPlatform {
            excludeTags("sandbox-native")
        }
    }.also {
        confugureFirPluginAnnotationsDependency(it)
    }

    nativeTestTask(
        taskName = "nativeTest",
        tag = "sandbox-native", // Include all tests with the "sandbox-native" tag
        requirePlatformLibs = false,
        customTestDependencies = listOf(sandboxAnnotationsNativeRuntimeForTests),
        compilerPluginDependencies = listOf(sandboxPluginForTests)
    )

    testGenerator("org.jetbrains.kotlin.plugin.sandbox.TestGeneratorKt")

    withJvmStdlibAndReflect()
}
