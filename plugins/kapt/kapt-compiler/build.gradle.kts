
description = "Annotation Processor for Kotlin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:backend.jvm.entrypoint"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:fir2ir:jvm-backend"))
    compileOnly(project(":kotlin-annotation-processing-cli"))
    compileOnly(project(":kotlin-annotation-processing-base"))
    compileOnly(project(":kotlin-annotation-processing-runtime"))
    compileOnly(intellijCore())
    compileOnly(toolsJarApi())
    compileOnly(libs.intellij.asm)

    testFixturesImplementation(intellijCore())
    testRuntimeOnly(intellijResources()) { isTransitive = false }

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))

    testFixturesApi(project(":kotlin-annotation-processing-base"))
    testFixturesApi(testFixtures(project(":kotlin-annotation-processing-base")))
    testFixturesApi(project(":kotlin-annotation-processing-runtime"))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testFixturesCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())

    embedded(project(":kotlin-annotation-processing-runtime")) { isTransitive = false }
    embedded(project(":kotlin-annotation-processing-cli")) { isTransitive = false }
    embedded(project(":kotlin-annotation-processing-base")) { isTransitive = false }

    testFixturesApi(project(":tools:kotlinp-jvm"))
    testFixturesApi(project(":kotlin-metadata-jvm"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

testsJar {}

projectTests {
    fun Project.kaptTestTask(name: String, javaLanguageVersion: JavaLanguageVersion) {
        val service = extensions.getByType<JavaToolchainService>()

        testTask(taskName = name, jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
            useJUnitPlatform {
                excludeTags = setOf("IgnoreJDK11")
            }
            workingDir = rootDir
            dependsOn(":dist")
            javaLauncher.set(service.launcherFor { languageVersion.set(javaLanguageVersion) })
        }
    }

    kaptTestTask("test", JavaLanguageVersion.of(8))
    kaptTestTask("testJdk11", JavaLanguageVersion.of(11))
    kaptTestTask("testJdk17", JavaLanguageVersion.of(17))
    kaptTestTask("testJdk21", JavaLanguageVersion.of(21))

    testGenerator("org.jetbrains.kotlin.kapt.test.TestGeneratorKt")

    withJvmStdlibAndReflect()
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
