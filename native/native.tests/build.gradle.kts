plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    // Reexport these dependencies to every user of nativeTest()
    testFixturesApi(kotlinStdlib())
    testFixturesApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesApi(intellijCore())
    testFixturesApi(commonDependency("org.apache.commons:commons-lang3"))
    testFixturesApi(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testFixturesApi(project(":kotlin-compiler-runner-unshaded"))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-integration")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(project(":native:kotlin-native-utils"))
    testFixturesApi(project(":native:executors"))
    testFixturesApi(project(":native:binary-options"))

    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesImplementation(project(":compiler:ir.serialization.native"))
    testFixturesImplementation(project(":compiler:fir:fir-native"))
    testFixturesImplementation(project(":core:compiler.common.native"))
    testFixturesImplementation(project(":kotlin-util-klib-abi"))
    testFixturesImplementation(project(":native:swift:swift-export-standalone"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(commonDependency("org.jetbrains.kotlinx", "kotlinx-metadata-klib"))
    testFixturesImplementation(libs.kotlinx.coroutines.core) { isTransitive = false }

    testFixturesImplementation(project(":compiler:cli:cli-native-klib"))
    testRuntimeOnly(libs.intellij.fastutil)
    testImplementation(testFixtures(project(":native:native.tests:klib-compatibility")))
    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        testImplementation(project(":native:cli-native"))
    }
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

testsJar {}

projectTests {
    testData(isolated, "testData")
    testData(project(":compiler").isolated, "testData")
    testData(project(":kotlin-test").isolated, "common/src/test/kotlin")

    // From StdlibTest
    testData(project(":kotlin-stdlib").isolated, "test")
    testData(project(":kotlin-stdlib").isolated, "common/test")
    testData(project(":kotlin-stdlib").isolated, "native-wasm/test")
    // :kotlin-native:runtime project availability depends on kotlin.native.enabled=true
    testData(rootProject.isolated, "kotlin-native/runtime/test")

    // Tasks that run different sorts of tests. Most frequent use case: running specific tests at TeamCity.
    nativeTestTask("infrastructureTest", "infrastructure")
    nativeTestTask("stdlibTest", "stdlib")
    nativeTestTask("kotlinTestLibraryTest", "kotlin-test")
    nativeTestTask("partialLinkageTest", "partial-linkage")
    nativeTestTask("cinteropTest", "cinterop")
    nativeTestTask("debuggerTest", "debugger")
    nativeTestTask("cachesTest", "caches")
    nativeTestTask("klibTest", "klib")
    nativeTestTask("standaloneTest", "standalone")
    nativeTestTask("gcTest", "gc")

    nativeTestTask(
        "test",
        requirePlatformLibs = true,
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_1_8,  // required in CompilerOutputTest via AbstractCliTest.getNormalizedCompilerOutput
            JdkMajorVersion.JDK_11_0, // required in CompilerOutputTest via AbstractCliTest.getNormalizedCompilerOutput
            JdkMajorVersion.JDK_17_0, // required in CompilerOutputTest via AbstractCliTest.getNormalizedCompilerOutput
            JdkMajorVersion.JDK_21_0,
        )
    ) {
        options {
            // See [org.jetbrains.kotlin.konan.test.KlibCrossCompilationIdentityTest.FULL_CROSS_DIST_ENABLED_PROPERTY]
            // See also kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/nativeFullCrossDist.kt
            systemProperty(
                "kotlin.native.internal.fullCrossDistEnabled",
                kotlinBuildProperties.stringProperty("kotlin.native.pathToDarwinDist").orNull != null
            )
        }

        // To workaround KTI-2421, we make these tests run on JDK 11 instead of the project-default JDK 8.
        // Kotlin test infra uses reflection to access JDK internals.
        // With JDK 11, some JVM args are required to silence the warnings caused by that:
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt", generateTestsInBuildDirectory = true) {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
}

optInToK1Deprecation()
