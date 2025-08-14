import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures(project(":native:native.tests")))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTests {
    testData(project(":compiler").isolated, "testData/codegen")

    val testTags = findProperty("kotlin.native.tests.tags")?.toString()
    // Note: arbitrary JUnit tag expressions can be used in this property.
    // See https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions
    nativeTestTask("test", testTags) {
        extensions.configure<TestInputsCheckExtension> {
            isNative.set(true)
            useXcode.set(OperatingSystem.current().isMacOsX)
        }
        // nativeTest sets workingDir to rootDir so here we need to override it
        workingDir = projectDir
    }
}
