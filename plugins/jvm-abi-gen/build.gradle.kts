import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "ABI generation for Kotlin/JVM"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

val embedded by configurations
embedded.isTransitive = false
configurations.getByName("compileOnly").extendsFrom(embedded)
configurations.getByName("testApi").extendsFrom(embedded)

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

    // Include kotlin.metadata for metadata stripping.
    // Note that kotlin-metadata-jvm already includes kotlin-metadata, core:metadata, core:metadata.jvm,
    // and protobuf-lite, so we only need to include kotlin-metadata-jvm in the shadow jar.
    compileOnly(project(":kotlin-metadata"))
    embedded(project(":kotlin-metadata-jvm"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    testFixturesApi(libs.junit4)
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:incremental-compilation-impl")))
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
    testTask(jUnitMode = JUnitMode.JUnit4) {
        workingDir = rootDir
        dependsOn(":dist")
    }

    testGenerator("org.jetbrains.kotlin.jvm.abi.TestGeneratorKt")

    withJvmStdlibAndReflect()
}

testsJar()
