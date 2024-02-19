plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(intellijCore())
    testImplementation(commonDependency("commons-lang:commons-lang"))
    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testImplementation(project(":kotlin-compiler-runner-unshaded"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(project(":compiler:ir.serialization.native"))
    testImplementation(project(":compiler:fir:native"))
    testImplementation(project(":native:kotlin-native-utils"))
    testApi(project(":native:executors")) // Reexport this dependency to every user of nativeTest()
    testImplementation(project(":kotlin-util-klib-abi"))
    testImplementation(project(":native:swift:swift-export-standalone"))
    testImplementation(projectTests(":kotlin-util-klib-abi"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-metadata-klib"))
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
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
val stdlibTest = nativeTest("stdlibTest", "stdlib & !frontend-fir")
val stdlibK2Test = nativeTest("stdlibK2Test", "stdlib & frontend-fir")
val kotlinTestLibraryTest = nativeTest("kotlinTestLibraryTest", "kotlin-test & !frontend-fir")
val kotlinTestLibraryK2Test = nativeTest("kotlinTestLibraryK2Test", "kotlin-test & frontend-fir")
val partialLinkageTest = nativeTest("partialLinkageTest", "partial-linkage")
val cinteropTest = nativeTest("cinteropTest", "cinterop")
val debuggerTest = nativeTest("debuggerTest", "debugger")
val cachesTest = nativeTest("cachesTest", "caches")
val klibTest = nativeTest("klibTest", "klib")
val standaloneTest = nativeTest("standaloneTest", "standalone")
val driverTest = nativeTest("driverTest", "driver")

val testTags = findProperty("kotlin.native.tests.tags")?.toString()
// Note: arbitrary JUnit tag expressions can be used in this property.
// See https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions
val test by nativeTest("test", testTags, requirePlatformLibs = true)

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}
