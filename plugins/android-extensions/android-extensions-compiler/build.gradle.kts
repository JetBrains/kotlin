
description = "Kotlin Android Extensions Compiler"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val robolectricClasspath by configurations.creating
val androidExtensionsRuntimeForTests by configurations.creating

dependencies {
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    compile(project(":compiler:util"))
    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }

    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":kotlin-android-extensions-runtime"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(commonDep("junit:junit"))

    testRuntime(intellijPluginDep("junit"))

    robolectricClasspath(commonDep("org.robolectric", "robolectric"))
    robolectricClasspath("org.robolectric:android-all:4.4_r1-robolectric-1")
    robolectricClasspath(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    embedded(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    androidExtensionsRuntimeForTests(project(":kotlin-android-extensions-runtime"))  { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

dist()

testsJar()

projectTest {
    dependsOn(androidExtensionsRuntimeForTests)
    dependsOn(":dist")
    workingDir = rootDir
    useAndroidJar()
    doFirst {
        systemProperty("androidExtensionsRuntime.classpath", androidExtensionsRuntimeForTests.asPath)
        val androidPluginPath = File(intellijRootDir(), "plugins/android").canonicalPath
        systemProperty("ideaSdk.androidPlugin.path", androidPluginPath)
        systemProperty("robolectric.classpath", robolectricClasspath.asPath)
    }
}
