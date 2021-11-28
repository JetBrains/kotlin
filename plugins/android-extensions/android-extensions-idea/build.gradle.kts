
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

    testApi(project(":compiler:cli"))
    testApi(project(":compiler:frontend.java"))
    testApi(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testApi(project(":plugins:kapt3-idea"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":idea"))
    testApi(projectTests(":idea:idea-android"))
    testApi(project(":kotlin-test:kotlin-test-jvm"))
    testApi(commonDep("junit:junit"))
    testApi(project(":idea:idea-native")) { isTransitive = false }
    testApi(project(":idea:idea-gradle-native")) { isTransitive = false }
    testRuntime(project(":native:frontend.native"))
    testRuntime(project(":kotlin-reflect"))
    testApi(intellijPluginDep("android"))
    testApi(intellijPluginDep("Groovy"))
    testApi(intellijDep())

    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":kotlin-scripting-idea"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))
    testRuntime(project(":plugins:lint"))
    testRuntime(project(":plugins:parcelize:parcelize-ide"))
    testRuntime(project(":plugins:lombok:lombok-ide-plugin"))
    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("IntelliLang"))
    testRuntime(intellijPluginDep("properties"))
    testRuntime(intellijPluginDep("java-i18n"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("java-decompiler"))
    Ide.IJ {
        testRuntime(intellijPluginDep("maven"))
        testRuntime(intellijPluginDep("repository-search"))
    }
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("smali"))

    Ide.AS {
        testRuntime(intellijPluginDep("android-layoutlib"))
        testRuntime(intellijPluginDep("platform-images"))
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
