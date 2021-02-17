description = "Parcelize compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val robolectricClasspath by configurations.creating
val parcelizeRuntimeForTests by configurations.creating

dependencies {
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree.impl"))
    compileOnly(project(":plugins:parcelize:parcelize-runtime"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }

    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:ir.backend.common"))
    testCompile(project(":compiler:backend.jvm"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":plugins:parcelize:parcelize-runtime"))
    testCompile(project(":kotlin-android-extensions-runtime"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(commonDep("junit:junit"))

    testRuntime(intellijPluginDep("junit"))

    robolectricClasspath(commonDep("org.robolectric", "robolectric"))
    robolectricClasspath("org.robolectric:android-all:4.4_r1-robolectric-1")
    robolectricClasspath(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }
    robolectricClasspath(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    embedded(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }
    embedded(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    parcelizeRuntimeForTests(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }
    parcelizeRuntimeForTests(project(":kotlin-android-extensions-runtime")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
javadocJar()
sourcesJar()

testsJar()

projectTest {
    dependsOn(parcelizeRuntimeForTests)
    dependsOn(":dist")
    workingDir = rootDir
    useAndroidJar()

    val androidPluginPath = File(intellijRootDir(), "plugins/android/lib").canonicalPath
    systemProperty("ideaSdk.androidPlugin.path", androidPluginPath)

    val parcelizeRuntimeForTestsProvider = project.provider { parcelizeRuntimeForTests.asPath }
    val robolectricClasspathProvider = project.provider { robolectricClasspath.asPath }
    doFirst {
        systemProperty("parcelizeRuntime.classpath", parcelizeRuntimeForTestsProvider.get())
        systemProperty("robolectric.classpath", robolectricClasspathProvider.get())
    }
}

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
