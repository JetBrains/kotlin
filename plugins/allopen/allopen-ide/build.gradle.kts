
description = "Kotlin AllOpen IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-allopen-compiler-plugin"))

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-jvm"))
    compileOnly(project(":idea:idea-jps-common"))
    compileOnly(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(intellijDep())
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) }
    compileOnly(intellijPluginDep("gradle"))

    testRuntime(project(":kotlin-reflect"))

    testCompileOnly(project(":kotlin-serialization"))
    testCompileOnly(project(":plugins:lint"))
    testCompileOnly(project(":plugins:kapt3-idea"))
    testCompileOnly(project(":plugins:android-extensions-compiler"))
    testCompileOnly(project(":kotlin-android-extensions"))
    testCompileOnly(project(":kotlin-android-extensions-runtime"))
    testCompileOnly(project(":plugins:android-extensions-ide"))
    testCompileOnly(project(":kotlin-allopen-compiler-plugin"))
    testCompileOnly(project(":allopen-ide-plugin"))
    testCompileOnly(project(":kotlin-imports-dumper-compiler-plugin"))
    testCompileOnly(project(":kotlin-source-sections-compiler-plugin"))
    testCompileOnly(project(":kotlinx-serialization-compiler-plugin"))
    testCompileOnly(project(":kotlinx-serialization-ide-plugin"))
    testCompileOnly(project(":kotlin-sam-with-receiver-compiler-plugin"))
    testCompileOnly(project(":noarg-ide-plugin"))
    testCompileOnly(project(":sam-with-receiver-ide-plugin"))
    testCompileOnly(project(":idea:idea-native"))
    testCompileOnly(project(":idea:idea-gradle-native"))
    testCompileOnly(projectTests(":idea:idea-test-framework"))
    testCompileOnly(intellijDep())
    testRuntimeOnly(intellijDep())

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
        testCompileOnly(intellijPluginDep("java"))
        testRuntimeOnly(intellijPluginDep("java"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

sourcesJar()

javadocJar()

projectTest(parallel = true)

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
