import org.jetbrains.kotlin.ideaExt.idea

description = "Parcelize compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val robolectricClasspath by configurations.creating
val parcelizeRuntimeForTests by configurations.creating

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
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }

    // FIR dependencies
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree.impl"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":kotlin-reflect-api"))

    testApiJUnit5()

    testApi(project(":compiler:util"))
    testApi(project(":compiler:backend"))
    testApi(project(":compiler:ir.backend.common"))
    testApi(project(":compiler:backend.jvm"))
    testApi(project(":compiler:cli"))
    testApi(project(":plugins:parcelize:parcelize-runtime"))
    testApi(project(":kotlin-android-extensions-runtime"))
    testApi(project(":kotlin-test:kotlin-test-jvm"))

    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))

    // FIR dependencies
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:checkers:checkers.jvm"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))

    testApi(commonDep("junit:junit"))

    testRuntimeOnly(intellijPluginDep("junit"))

    robolectricClasspath(commonDep("org.robolectric", "robolectric"))
    robolectricClasspath("org.robolectric:android-all:4.4_r1-robolectric-1")
    robolectricClasspath(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }
    robolectricClasspath(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    parcelizeRuntimeForTests(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }
    parcelizeRuntimeForTests(project(":kotlin-android-extensions-runtime")) { isTransitive = false }
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

runtimeJar()
javadocJar()
sourcesJar()

testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
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
    doLast {
        println(filter)
        println(filter.excludePatterns)
        println(filter.includePatterns)
    }
}
