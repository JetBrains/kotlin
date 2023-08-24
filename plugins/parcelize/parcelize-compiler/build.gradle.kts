description = "Parcelize compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val robolectricClasspath by configurations.creating
val parcelizeRuntimeForTests by configurations.creating
val layoutLib by configurations.creating
val layoutLibApi by configurations.creating

dependencies {
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.common")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.k1")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.k2")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.backend")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.cli")) { isTransitive = false }

    testApiJUnit5()

    testApi(intellijCore())

    testApi(project(":plugins:parcelize:parcelize-compiler:parcelize.cli"))

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
    testApi(project(":compiler:fir:plugin-utils"))
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:checkers:checkers.jvm"))
    testApi(project(":compiler:fir:checkers:checkers.js"))
    testApi(project(":compiler:fir:checkers:checkers.native"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(project(":core:descriptors.runtime"))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }

    testApi(commonDependency("junit:junit"))

    robolectricClasspath(commonDependency("org.robolectric", "robolectric"))
    robolectricClasspath("org.robolectric:android-all:4.4_r1-robolectric-r2")
    robolectricClasspath(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }
    robolectricClasspath(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    parcelizeRuntimeForTests(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }
    parcelizeRuntimeForTests(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    layoutLib("org.jetbrains.intellij.deps.android.tools:layoutlib:26.5.0") { isTransitive = false }
    layoutLibApi("com.android.tools.layoutlib:layoutlib-api:26.5.0") { isTransitive = false }
}

optInToExperimentalCompilerApi()
optInToIrSymbolInternals()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
    dependsOn(parcelizeRuntimeForTests)
    dependsOn(robolectricClasspath)
    dependsOn(":dist")
    workingDir = rootDir
    useAndroidJar()

    val parcelizeRuntimeForTestsConf: FileCollection = parcelizeRuntimeForTests
    val robolectricClasspathConf: FileCollection = robolectricClasspath
    val layoutLibConf: FileCollection = layoutLib
    val layoutLibApiConf: FileCollection = layoutLibApi
    doFirst {
        systemProperty("parcelizeRuntime.classpath", parcelizeRuntimeForTestsConf.asPath)
        systemProperty("robolectric.classpath", robolectricClasspathConf.asPath)
        systemProperty("layoutLib.path", layoutLibConf.singleFile.canonicalPath)
        systemProperty("layoutLibApi.path", layoutLibApiConf.singleFile.canonicalPath)
    }
    doLast {
        println(filter)
        println(filter.excludePatterns)
        println(filter.includePatterns)
    }
}
