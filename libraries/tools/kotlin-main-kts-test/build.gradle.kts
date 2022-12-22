
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
    testApi(commonDependency("junit"))
    testApi(projectTests(":kotlin-scripting-compiler")) { isTransitive = false }
    testImplementation(project(":kotlin-compiler-embeddable"))
    kotlinxSerializationGradlePluginClasspath(project(":kotlin-serialization")) { isTransitive = false }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist", ":kotlin-serialization:jar")
    val localKotlinxSerializationPluginClasspath: FileCollection = kotlinxSerializationGradlePluginClasspath
    doFirst {
        systemProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath", localKotlinxSerializationPluginClasspath.asPath)
    }
    workingDir = rootDir
}

projectTest(taskName = "testWithK2", parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.script.base.compiler.arguments", "-Xuse-k2")
}
