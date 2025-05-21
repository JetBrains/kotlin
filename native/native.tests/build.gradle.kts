plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    // Reexport these dependencies to every user of nativeTest()
    testApi(kotlinStdlib())
    testApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testApi(intellijCore())
    testApi(commonDependency("commons-lang:commons-lang"))
    testApi(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testApi(project(":kotlin-compiler-runner-unshaded"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:tests-integration"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(project(":native:kotlin-native-utils"))
    testApi(project(":native:executors"))

    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(project(":compiler:ir.serialization.native"))
    testImplementation(project(":compiler:fir:fir-native"))
    testImplementation(project(":core:compiler.common.native"))
    testImplementation(project(":kotlin-util-klib-abi"))
    testImplementation(project(":native:swift:swift-export-standalone"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-metadata-klib"))
    testImplementation(libs.kotlinx.coroutines.core) { isTransitive = false }

    testRuntimeOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
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

// Note: arbitrary JUnit tag expressions can be used in this property.
// See https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions
val test by nativeTest(
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
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}
