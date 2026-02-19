
description = "Kotlin \"main\" script definition tests"

plugins {
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
    testImplementation(projectTests(":kotlin-scripting-compiler"))
    testImplementation(project(":kotlin-compiler-embeddable"))
    testImplementation(project(":kotlin-scripting-common"))
    testImplementation(project(":kotlin-scripting-jvm"))
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

    testTask("testWithK1", parallel = true, jUnitMode = JUnitMode.JUnit4, skipInLocalBuild = true) {
        dependsOn(":dist", ":kotlinx-serialization-compiler-plugin.embeddable:embeddable")
        workingDir = rootDir
        val localKotlinxSerializationPluginClasspath: FileCollection = kotlinxSerializationGradlePluginClasspath
        doFirst {
            systemProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath", localKotlinxSerializationPluginClasspath.asPath)
            systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
            systemProperty("kotlin.script.test.base.compiler.arguments", "-language-version 1.9")
        }
    }

    withJvmStdlibAndReflect()
}
