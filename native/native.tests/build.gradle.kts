plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("project-tests-convention")
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

    testRuntimeOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

testsJar {}

projectTests {
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
                kotlinBuildProperties.getOrNull("kotlin.native.pathToDarwinDist") != null
            )
        }

        // To workaround KTI-2421, we make these tests run on JDK 11 instead of the project-default JDK 8.
        // Kotlin test infra uses reflection to access JDK internals.
        // With JDK 11, some JVM args are required to silence the warnings caused by that:
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt") {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
}

optInToK1Deprecation()
