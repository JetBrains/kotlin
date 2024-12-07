
description = "Kotlin \"main\" script definition tests"

plugins {
    kotlin("jvm")
}

val kotlinxSerializationGradlePluginClasspath by configurations.creating

dependencies {
    testApi(project(":kotlin-main-kts"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testApi(kotlinStdlib("jdk8"))
    testImplementation(libs.junit4)
    testApi(projectTests(":kotlin-scripting-compiler")) { isTransitive = false }
    testImplementation(project(":kotlin-compiler-embeddable"))
    kotlinxSerializationGradlePluginClasspath(project(":kotlinx-serialization-compiler-plugin.embeddable")) { isTransitive = false }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist", ":kotlinx-serialization-compiler-plugin.embeddable:embeddable")
    workingDir = rootDir
    val localKotlinxSerializationPluginClasspath: FileCollection = kotlinxSerializationGradlePluginClasspath
    doFirst {
        systemProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath", localKotlinxSerializationPluginClasspath.asPath)
    }
}

projectTest(taskName = "testWithK1", parallel = true) {
    dependsOn(":dist", ":kotlinx-serialization-compiler-plugin.embeddable:embeddable")
    workingDir = rootDir
    val localKotlinxSerializationPluginClasspath: FileCollection = kotlinxSerializationGradlePluginClasspath
    doFirst {
        systemProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath", localKotlinxSerializationPluginClasspath.asPath)
        systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
        systemProperty("kotlin.script.test.base.compiler.arguments", "-language-version 1.9")
    }
}
