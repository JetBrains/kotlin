plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
}

description = "A set of integration tests for Swift Import Standalone"

kotlin {
    explicitApi()
}

// swift-import-standalone (and swiftextract it depends on) requires JDK 25.
jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_25_0
    targetBytecodeVersion = JdkMajorVersion.JDK_25_0
}

dependencies {
    compileOnly(kotlinStdlib())

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(testFixtures(project(":native:swift:swift-import-standalone-integration-tests")))
    // Provides the IntelliJ platform runtime that the standalone Analysis API session of Swift Import requires.
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))

    testFixturesImplementation(testFixtures(project(":native:swift:swift-import-standalone-integration-tests")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
}

sourceSets {
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(isolated, "testData")

    nativeTestTask(
        "test",
        allowUnsafe = true, // KT-85212
    ) {
        dependsOn(":kotlin-native:distInvalidateStaleCaches")
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_25_0))
        // swiftextract loads its native libraries via JNI.
        jvmArgs("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
    }

    testGenerator(
        "org.jetbrains.kotlin.swiftimport.standalone.test.simple.TestGeneratorKt",
        generateTestsInBuildDirectory = true,
    ) {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_25_0))
    }
}
