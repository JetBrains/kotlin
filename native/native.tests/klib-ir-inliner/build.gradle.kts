import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

val llvmDevBinaryDataUsage by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(project(":compiler:ir.objcinterop"))
    testFixturesApi(project(":compiler:ir.backend.native"))
    testFixturesApi(project(":compiler:ir.serialization.native"))
    testFixturesApi(project(":compiler:test-infrastructure"))
    testFixturesApi(project(":kotlin-util-klib-abi"))
    testFixturesApi(testFixtures(project(":native:kotlin-native-utils")))
    testFixturesApi(testFixtures(project(":native:native.tests")))
    testFixturesApi(testFixtures(project(":kotlin-util-klib-abi")))

    if (project.kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        llvmDevBinaryDataUsage(project(":kotlin-native:dependencies", configuration = "llvmDevBinaryData"))
    }
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
    }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(project(":compiler").isolated, "testData/klib")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/ir")
    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/loadJava")
    testData(project(":native:native.tests").isolated, "testData/codegen")
    testData(project(":native:native.tests").isolated, "testData/klib")
    testData(project(":native:native.tests").isolated, "testData/irProvidersMismatch")
    testData(project(":native:native.tests").isolated, "testData/oneStageCompilation")

    nativeTestTask(
        "test",
        allowParallelExecution = true,
        requirePlatformLibs = true,
        enableGroupingTestEngine = true,
    ) {
        val testTargetName = providers.gradleProperty("kotlin.internal.native.test.target")
            .orElse(providers.gradleProperty("kn.target"))
            .getOrElse(HostManager.hostName)

        val testTarget = KonanTarget.predefinedTargets[testTargetName] ?: error("Test target $testTargetName is not defined")
        if (!testTarget.family.isAppleFamily) {
            // KT-85080: make sure Llvm-dev native dependency is downloaded, so Clang tool is available
            // Clang is used via `compileWithClangToStaticLibrary()` in ObjCInteropFacade for CInterop tests with source files of types: c, cpp m, mm
            // Note: For Apple targets, clang is invoked from XCode toolchain instead, see `Settings.defaultClangDistribution()` in Clang.kt
            inputs.files(llvmDevBinaryDataUsage)
                .withPropertyName("llvmDevBinaryDataUsage")
                .withPathSensitivity(PathSensitivity.NONE)
        }

        // To workaround KTI-2421, we make these tests run on JDK 11 instead of the project-default JDK 8.
        // Kotlin test infra uses reflection to access JDK internals.
        // With JDK 11, some JVM args are required to silence the warnings caused by that:
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")

        systemProperty("user.dir", layout.buildDirectory.asFile.get().absolutePath)
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateKlibNativeTestsKt", generateTestsInBuildDirectory = true) {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
}

testsJar {}
