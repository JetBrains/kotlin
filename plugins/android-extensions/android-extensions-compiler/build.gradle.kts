
description = "Kotlin Android Extensions Compiler"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val robolectricClasspath by configurations.creating
val androidExtensionsRuntimeForTests by configurations.creating

dependencies {
    testApi(intellijCoreDep()) { includeJars("intellij-core") }

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree.impl"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }

    testApi(project(":compiler:util"))
    testApi(project(":compiler:backend"))
    testApi(project(":compiler:ir.backend.common"))
    testApi(project(":compiler:backend.jvm"))
    testApi(project(":compiler:cli"))
    testApi(project(":kotlin-android-extensions-runtime"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(project(":kotlin-test:kotlin-test-jvm"))
    testApi(commonDep("junit:junit"))

    testRuntimeOnly(intellijPluginDep("junit"))
    testRuntimeOnly(intellijDep())

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

sourcesJar()

javadocJar()

testsJar()

projectTest {
    dependsOn(androidExtensionsRuntimeForTests)
    dependsOn(":dist")
    workingDir = rootDir
    useAndroidJar()

    val androidPluginPath = File(intellijRootDir(), "plugins/android/lib").canonicalPath
    systemProperty("ideaSdk.androidPlugin.path", androidPluginPath)

    val androidExtensionsRuntimeProvider = project.provider {
        androidExtensionsRuntimeForTests.asPath
    }
    val robolectricClasspathProvider = project.provider {
        robolectricClasspath.asPath
    }
    doFirst {
        systemProperty("androidExtensionsRuntime.classpath", androidExtensionsRuntimeProvider.get())
        systemProperty("robolectric.classpath", robolectricClasspathProvider.get())
    }
}
