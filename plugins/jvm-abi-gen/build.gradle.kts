description = "ABI generation for Kotlin/JVM"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

val embedded = configurations.embedded.get()
embedded.isTransitive = false
configurations.compileOnly.get().extendsFrom(embedded)
configurations.testApi.get().extendsFrom(embedded)

dependencies {
    // Should come before dependency on proguarded compiler because StringUtil methods are deleted from it
    testRuntimeOnly(intellijPlatformUtil()) { isTransitive = false }

    testRuntimeOnly(project(":kotlin-compiler"))

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":compiler:backend.common.jvm"))

    // Include kotlin.metadata for metadata stripping.
    // Note that kotlin-metadata-jvm already includes kotlin-metadata, core:metadata, core:metadata.jvm,
    // and protobuf-lite, so we only need to include kotlin-metadata-jvm in the shadow jar.
    compileOnly(project(":kotlin-metadata"))
    embedded(project(":kotlin-metadata-jvm"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:incremental-compilation-impl")))

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

optInToExperimentalCompilerApi()

publish()

runtimeJarWithRelocation {
    from(mainSourceSet.output)
    relocate("kotlinx.metadata", "org.jetbrains.kotlin.jvm.abi.kotlinx.metadata")
    mergeServiceFiles() // This is needed to relocate the services files for kotlinx.metadata
}

sourcesJar()

javadocJar()

projectTests {
    testTask(javaLauncher = JdkMajorVersion.JDK_1_8) {
        addClasspathProperty("kotlin.jvm.abi.jar.path") {
            from(tasks.jar.map { it.archiveFile.get() })
        }
    }

    testGenerator("org.jetbrains.kotlin.jvm.abi.TestGeneratorKt")

    testData(isolated, "testData")

    withJvmStdlibAndReflect()
}

testsJar()
