
description = "Kotlin Android Extensions Compiler"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val robolectricClasspath by configurations.creating
val androidExtensionsRuntimeForTests by configurations.creating
val layoutLib by configurations.creating
val layoutLibApi by configurations.creating

dependencies {
    testApi(intellijCore())

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    testApi(project(":compiler:util"))
    testApi(project(":compiler:backend"))
    testApi(project(":compiler:ir.backend.common"))
    testApi(project(":compiler:backend.jvm"))
    testApi(project(":compiler:cli"))
    testApi(project(":kotlin-android-extensions-runtime"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(project(":kotlin-test:kotlin-test-jvm"))
    testApi(commonDependency("junit:junit"))

    robolectricClasspath(commonDependency("org.robolectric", "robolectric"))
    robolectricClasspath("org.robolectric:android-all:4.4_r1-robolectric-1")
    robolectricClasspath(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    embedded(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    androidExtensionsRuntimeForTests(project(":kotlin-android-extensions-runtime"))  { isTransitive = false }

    layoutLib("org.jetbrains.intellij.deps.android.tools:layoutlib:26.5.0") { isTransitive = false }
    layoutLibApi("com.android.tools.layoutlib:layoutlib-api:26.5.0") { isTransitive = false }
}

optInToExperimentalCompilerApi()
optInToIrSymbolInternals()

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
    dependsOn(robolectricClasspath)
    dependsOn(":dist")
    workingDir = rootDir
    useAndroidJar()

    val androidExtensionsRuntimeProvider = project.provider {
        androidExtensionsRuntimeForTests.asPath
    }

    val robolectricClasspathProvider = project.provider {
        robolectricClasspath.asPath
    }

    val layoutLibConf: FileCollection = layoutLib
    val layoutLibApiConf: FileCollection = layoutLibApi

    doFirst {
        systemProperty("androidExtensionsRuntime.classpath", androidExtensionsRuntimeProvider.get())
        systemProperty("robolectric.classpath", robolectricClasspathProvider.get())
        systemProperty("layoutLib.path", layoutLibConf.singleFile.canonicalPath)
        systemProperty("layoutLibApi.path", layoutLibApiConf.singleFile.canonicalPath)
    }
}
