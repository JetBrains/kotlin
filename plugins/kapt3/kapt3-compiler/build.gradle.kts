
description = "Annotation Processor for Kotlin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:backend.jvm.entrypoint"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":kotlin-annotation-processing-cli"))
    compileOnly(project(":kotlin-annotation-processing-base"))
    compileOnly(project(":kotlin-annotation-processing-runtime"))
    compileOnly(intellijCore())
    compileOnly(toolsJarApi())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    testImplementation(intellijCore())
    testRuntimeOnly(intellijResources()) { isTransitive = false }

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))

    testApi(project(":kotlin-annotation-processing-base"))
    testApi(projectTests(":kotlin-annotation-processing-base"))
    testApi(project(":kotlin-annotation-processing-runtime"))

    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())

    embedded(project(":kotlin-annotation-processing-runtime")) { isTransitive = false }
    embedded(project(":kotlin-annotation-processing-cli")) { isTransitive = false }
    embedded(project(":kotlin-annotation-processing-base")) { isTransitive = false }

    testApi(project(":tools:kotlinp"))
    testApi(project(":kotlinx-metadata-jvm"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

kaptTestTask("test", JavaLanguageVersion.of(8))
kaptTestTask("testJdk11", JavaLanguageVersion.of(11))
kaptTestTask("testJdk17", JavaLanguageVersion.of(17))

fun Project.kaptTestTask(name: String, javaLanguageVersion: JavaLanguageVersion) {
    val service = extensions.getByType<JavaToolchainService>()

    projectTest(taskName = name, parallel = true) {
        useJUnitPlatform {
            excludeTags = setOf("IgnoreJDK11")
        }
        workingDir = rootDir
        dependsOn(":dist")
        javaLauncher.set(service.launcherFor { languageVersion.set(javaLanguageVersion) })
    }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
