plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(projectTests(":native:native.tests"))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

compilerTests {
    testData(project.isolated, "../testData")
}

val testTags = findProperty("kotlin.native.tests.tags")?.toString()
// Note: arbitrary JUnit tag expressions can be used in this property.
// See https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions
val test by nativeTest(
    "test", 
    testTags,
    requirePlatformLibs = true
) {
    extensions.configure<TestInputsCheckExtension> {
        isNative.set(true)
        useXcode.set(org.gradle.internal.os.OperatingSystem.current().isMacOsX)
    }
    // nativeTest sets workingDir to rootDir so here we need to override it
    workingDir = projectDir
}
