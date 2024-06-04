import org.jetbrains.kotlin.gradle.dsl.JvmTarget

description = "Fleet RhizomeDB Compiler Plugin"

repositories {
    maven { setUrl("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
}

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val rhizomedbClasspath by configurations.creating

dependencies {
    embedded(project(":rhizomedb-compiler-plugin.k2")) { isTransitive = false }

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(projectTests(":kotlinx-serialization-compiler-plugin"))

    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(project(":compiler:fir:plugin-utils"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(projectTests(":js:js.tests"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(project(":rhizomedb-compiler-plugin.k2"))

    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))
    testCompileOnly("org.jetbrains.fleet:rhizomedb:1.36.0-FL24724_a9c22bf7da121.11")

    rhizomedbClasspath("org.jetbrains.fleet:rhizomedb:1.36.0-FL24724_a9c22bf7da121.11")
    rhizomedbClasspath(project(":rhizomedb-compiler-plugin.testDeps"))
    rhizomedbClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
    rhizomedbClasspath("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.compileTestJava {
    targetCompatibility = "17"
}

tasks.compileTestKotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

runtimeJar()
sourcesJar()
javadocJar()

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform()
    dependsOn(project(":rhizomedb-compiler-plugin.testDeps").tasks.named<Jar>("jar"))

    val localRhizomeClasspath: FileCollection = rhizomedbClasspath
    doFirst {
        systemProperty("rhizome.classpath", localRhizomeClasspath.asPath)
    }
}

val generateTests by generator("org.jetbrains.rhizomedb.TestGeneratorKt")
