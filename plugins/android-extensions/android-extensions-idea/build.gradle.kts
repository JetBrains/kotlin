
description = "Kotlin Android Extensions IDEA"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":plugins:android-extensions-compiler"))

    testRuntime(intellijDep())

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:light-classes"))
    compileOnly(project(":idea:idea-core"))
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-jvm"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijPluginDep("android"))
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("Groovy"))
    compileOnly(intellijDep())

    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:kapt3-idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-android"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":idea:idea-native")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle-native")) { isTransitive = false }
    testRuntime(project(":kotlin-native:kotlin-native-library-reader")) { isTransitive = false }
    testRuntime(project(":kotlin-native:kotlin-native-utils")) { isTransitive = false }
    testRuntime(project(":kotlin-reflect"))
    testCompile(intellijPluginDep("android"))
    testCompile(intellijPluginDep("Groovy"))
    testCompile(intellijDep())

    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":kotlin-scripting-idea"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))
    testRuntime(project(":plugins:lint"))
    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("IntelliLang"))
    testRuntime(intellijPluginDep("properties"))
    testRuntime(intellijPluginDep("java-i18n"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("java-decompiler"))
    Ide.IJ {
        testRuntime(intellijPluginDep("maven"))
    }
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("smali"))

    if (Ide.AS36.orHigher()) {
        testRuntime(intellijPluginDep("android-layoutlib"))
    }

    if (Ide.AS36()) {
        testRuntime(intellijPluginDep("android-wizardTemplate-plugin"))
    }
}

sourceSets {
    "main" { }
    "test" { }
}

testsJar {}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    useAndroidSdk()
    useAndroidJar()
}

runtimeJar()

sourcesJar()

javadocJar()

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
