
description = "Kotlin AllOpen IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(project(":kotlin-reflect"))
    compile(project(":kotlin-allopen-compiler-plugin"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(intellijDep())
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) }
    compileOnly(intellijPluginDep("gradle"))

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
        testCompileOnly(intellijPluginDep("java"))
        testRuntimeOnly(intellijPluginDep("java"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

projectTest(parallel = true) {

}