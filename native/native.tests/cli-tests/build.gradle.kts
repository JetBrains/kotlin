plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

dependencies {
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.vintage.engine)

    testFixturesImplementation(project(":native:cli-native"))

    testFixturesApi(testFixtures(project(":native:native.tests")))
}

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

testsJar {}

compilerTests {
    nativeTestTask(
        "test",
        tag = null,
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_1_8,
            JdkMajorVersion.JDK_11_0,
            JdkMajorVersion.JDK_17_0,
            JdkMajorVersion.JDK_21_0
        )
    ) {
        // Kotlin test infra and IntelliJ platform Disposer debug mode use reflection to access JDK internals.
        // With JDK 11, some JVM args are required to silence the warnings caused by that:
        jvmArgs(
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
        )
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateCliTestsKt") {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
}
