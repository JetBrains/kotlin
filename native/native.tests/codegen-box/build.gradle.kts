import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures(project(":native:native.tests")))
    testFixturesImplementation(testFixtures(project(":native:native.tests")))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(project(":compiler").isolated, "testData/codegen")

    nativeTestTask("test") {
        extensions.configure<TestInputsCheckExtension> {
            isNative.set(true)
            useXcode.set(OperatingSystem.current().isMacOsX)
        }
        // nativeTest sets workingDir to rootDir so here we need to override it
        workingDir = projectDir
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateNativeCodegenBoxTestsKt", generateTestsInBuildDirectory = true) {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
}
