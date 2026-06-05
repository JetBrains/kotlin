import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
val nativeTargetName = HostManager.host.name
val sandboxAnnotationsNativeRuntimeForTests = configurations.create("sandboxAnnotationsNativeRuntimeForTests") {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val sandboxPluginForTests = configurations.create("sandboxPluginForTests")

dependencies {
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":core:descriptors"))

    implementation(project(":compiler:fir:cones"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:checkers"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:fir:plugin-utils"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    testFixturesApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests")))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesApi(project(":compiler:fir:plugin-utils"))
    testFixturesImplementation(testFixtures(project(":tools:kotlinp-jvm")))

    testFixturesApi(testFixtures(project(":native:native.tests")))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testRuntimeOnly(toolsJar())

    sandboxAnnotationsNativeRuntimeForTests(project(":plugins:plugin-sandbox:plugin-annotations"))
    sandboxPluginForTests(project(":plugins:plugin-sandbox"))
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask() {
        useJsIrBoxTests(buildDir = layout.buildDirectory)
        useJUnitPlatform {
            excludeTags("sandbox-native")
        }
    }

    nativeTestTask(
        taskName = "nativeTest",
        tag = "sandbox-native", // Include all tests with the "sandbox-native" tag
        requirePlatformLibs = false,
        customTestDependencies = listOf(sandboxAnnotationsNativeRuntimeForTests),
        compilerPluginDependencies = listOf(sandboxPluginForTests)
    )

    testGenerator("org.jetbrains.kotlin.plugin.sandbox.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withTestJar()
    withStdlibCommon()
    withJsRuntime()
    withPluginSandboxAnnotations()

    testData(project(":plugins:plugin-sandbox").isolated, "testData")
    testData(project(":js:js.translator").isolated, "testData/_commonFiles")
}
