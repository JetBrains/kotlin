
description = "Kotlin \"main\" script definition tests"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
}

val kotlinxSerializationGradlePluginClasspath by configurations.creating

dependencies {
    testImplementation(project(":kotlin-main-kts"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testImplementation(kotlinStdlib("jdk8"))
    testImplementation(libs.junit4)
    testImplementation(kotlinTest("junit"))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(projectTests(":kotlin-scripting-compiler"))
    testImplementation(project(":kotlin-scripting-common"))
    testImplementation(project(":kotlin-scripting-jvm"))
    testRuntimeOnly(project(":kotlin-scripting-compiler"))
    testRuntimeOnly(project(":kotlin-compiler"))
    kotlinxSerializationGradlePluginClasspath(project(":kotlinx-serialization-compiler-plugin.embeddable")) { isTransitive = false }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTests {
    testTask(parallel = true, jUnitMode = JUnitMode.JUnit4) {
        dependsOn(":dist", ":kotlinx-serialization-compiler-plugin.embeddable:embeddable")
        workingDir = rootDir
        val localKotlinxSerializationPluginClasspath: FileCollection = kotlinxSerializationGradlePluginClasspath
        doFirst {
            systemProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath", localKotlinxSerializationPluginClasspath.asPath)
        }
    }

    withJvmStdlibAndReflect()
    @OptIn(KotlinCompilerDistUsage::class)
    withDist()
    withMainKtsJar()
}
