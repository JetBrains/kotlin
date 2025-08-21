plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    // Reexport these dependencies to every user of nativeTest()
    testFixturesApi(kotlinStdlib())
    testFixturesApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesApi(intellijCore())
    testFixturesApi(commonDependency("commons-lang:commons-lang"))
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

// Tasks that run different sorts of tests. Most frequent use case: running specific tests at TeamCity.
val infrastructureTest = nativeTest("infrastructureTest", "infrastructure")
val stdlibTest = nativeTest("stdlibTest", "stdlib")
val kotlinTestLibraryTest = nativeTest("kotlinTestLibraryTest", "kotlin-test")
val partialLinkageTest = nativeTest("partialLinkageTest", "partial-linkage")
val cinteropTest = nativeTest("cinteropTest", "cinterop")
val debuggerTest = nativeTest("debuggerTest", "debugger")
val cachesTest = nativeTest("cachesTest", "caches")
val klibTest = nativeTest("klibTest", "klib")
val standaloneTest = nativeTest("standaloneTest", "standalone")
val gcTest = nativeTest("gcTest", "gc")

val testTags = findProperty("kotlin.native.tests.tags")?.toString()
// Note: arbitrary JUnit tag expressions can be used in this property.
// See https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions
val test by nativeTest(
    "test",
    testTags,
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

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}

optInToK1Deprecation()
