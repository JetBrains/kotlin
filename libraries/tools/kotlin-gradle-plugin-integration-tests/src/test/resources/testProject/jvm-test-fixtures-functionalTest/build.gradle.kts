@file:Suppress("HasPlatformType")

plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

repositories {
    mavenLocal()
    mavenCentral()
}

/* Create functionalTest (java) source set */
val functionalTestSourceSet = sourceSets.create("functionalTest")
val functionalTestCompilation = kotlin.target.compilations.getByName("functionalTest")

/* Associate 'functionalTest' with 'test' */
functionalTestCompilation.associateWith(kotlin.target.compilations.getByName("test"))

/* Create test task */
val functionalTest = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
}