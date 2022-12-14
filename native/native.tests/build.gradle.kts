import org.jetbrains.kotlin.konan.target.HostManager

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
    testImplementation(project(":native:kotlin-native-utils"))
    testImplementation(project(":native:executors"))
    testImplementation(project(":kotlin-util-klib-abi"))
    testImplementation(projectTests(":kotlin-util-klib-abi"))
    testApiJUnit5()
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

if (kotlinBuildProperties.isKotlinNativeEnabled &&
    HostManager.hostIsMac &&
    project.hasProperty(TestProperty.XCTEST_FRAMEWORK.fullName)
) {
    val xcTestConfig = configurations.detachedConfiguration(
        dependencies.project(path = ":kotlin-native:utilities:xctest-runner", configuration = "xcTestArtifactsConfig")
    )
    // Set test tasks dependency on this config
    tasks.withType<Test>().configureEach {
        if (name.endsWith("XCTest")) {
            dependsOn(xcTestConfig)
        }
    }

    val testTarget = project.findProperty(TestProperty.TEST_TARGET.fullName)?.toString() ?: HostManager.hostName
    // Set XCTest runner and cinterop klibs location
    project.extra.set(
        TestProperty.CUSTOM_KLIBS.fullName,
        xcTestConfig.resolvedConfiguration
            .resolvedArtifacts
            .filter { it.classifier == testTarget }
            .map { it.file }
            .joinToString(separator = File.pathSeparator)
    )
    // Set XCTest.framework location (Developer Frameworks directory)
    project.setProperty(
        TestProperty.XCTEST_FRAMEWORK.fullName,
        xcTestConfig.resolvedConfiguration
            .resolvedArtifacts
            .filter { it.classifier == "${testTarget}Frameworks" }
            .map { it.file }
            .singleOrNull()
    )
}

testsJar {}

// Tasks that run different sorts of tests. Most frequent use case: running specific tests at TeamCity.
val infrastructureTest = nativeTest("infrastructureTest", "infrastructure")
val codegenBoxTest = nativeTest("codegenBoxTest", "codegen & !frontend-fir")
val codegenBoxK2Test = nativeTest("codegenBoxK2Test", "codegen & frontend-fir")
val stdlibTest = nativeTest("stdlibTest", "stdlib & !frontend-fir & !xctest")
val stdlibK2Test = nativeTest("stdlibK2Test", "stdlib & frontend-fir & !xctest")
val kotlinTestLibraryTest = nativeTest("kotlinTestLibraryTest", "kotlin-test & !frontend-fir")
val kotlinTestLibraryK2Test = nativeTest("kotlinTestLibraryK2Test", "kotlin-test & frontend-fir")
val partialLinkageTest = nativeTest("partialLinkageTest", "partial-linkage")
val cinteropTest = nativeTest("cinteropTest", "cinterop")
val debuggerTest = nativeTest("debuggerTest", "debugger")
val cachesTest = nativeTest("cachesTest", "caches")
val klibTest = nativeTest("klibTest", "klib")

// xctest tasks
val stdlibTestWithXCTest = nativeTest("stdlibTestWithXCTest", "stdlib & !frontend-fir & xctest")
val stdlibK2TestWithXCTest = nativeTest("stdlibK2TestWithXCTest", "stdlib & frontend-fir & xctest")
val kotlinTestLibraryTestWithXCTest = nativeTest("kotlinTestLibraryTestWithXCTest", "kotlin-test & !frontend-fir & xctest")
val kotlinTestLibraryK2TestWithXCTest = nativeTest("kotlinTestLibraryK2TestWithXCTest", "kotlin-test & frontend-fir & xctest")

val testTags = findProperty("kotlin.native.tests.tags")?.toString()
// Note: arbitrary JUnit tag expressions can be used in this property.
// See https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions
val test by nativeTest("test", testTags)

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}
