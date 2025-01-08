plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    // Reexport these dependencies to every user of nativeTest()
    testApi(projectTests(":compiler:ir.backend.native"))
    testApi(projectTests(":compiler:tests-integration"))

    testImplementation(project(":compiler:ir.serialization.native"))
    testImplementation(project(":core:compiler.common.native"))
    testImplementation(project(":kotlin-util-klib-abi"))
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-metadata-klib"))
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
val codegenBoxTest = nativeTest("codegenBoxTest", "codegen & !frontend-fir")
val codegenBoxK2Test = nativeTest("codegenBoxK2Test", "codegen & frontend-fir")
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
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}
