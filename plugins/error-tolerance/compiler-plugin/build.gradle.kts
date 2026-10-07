description = "Kotlin Error Tolerance Compiler Plugin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
}

dependencies {
    embedded(project(":plugins:error-tolerance:compiler-plugin:error-tolerance.k2")) { isTransitive = false }
    embedded(project(":plugins:error-tolerance:compiler-plugin:error-tolerance.backend")) { isTransitive = false }
    embedded(project(":plugins:error-tolerance:compiler-plugin:error-tolerance.cli")) { isTransitive = false }

    // Should come before dependency on proguarded compiler because StringUtil methods are deleted from it
    testRuntimeOnly(intellijPlatformUtil()) { isTransitive = false }
    testRuntimeOnly(project(":kotlin-compiler"))

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:incremental-compilation-impl")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        generatedTestDir()
        projectDefault()
    }
    "testFixtures" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()

projectTests {
    testTask {
        // Required by the persistent storage of incremental compilation caches (IntelliJ platform)
        jvmArgs(
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
        )
        addClasspathProperty("errorTolerance.jar.path") {
            from(tasks.jar.map { it.archiveFile.get() })
        }
    }

    testGenerator("org.jetbrains.kotlin.errortolerance.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()

    testData(isolated, "testData")
}
