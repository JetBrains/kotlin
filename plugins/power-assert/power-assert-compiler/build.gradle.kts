description = "Kotlin Power-Assert Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val junit5Classpath: Configuration by configurations.creating
val powerAssertRuntimeClasspath: Configuration by configurations.creating

dependencies {
    embedded(project(":kotlin-power-assert-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-power-assert-compiler-plugin.cli")) { isTransitive = false }

    testImplementation(project(":kotlin-power-assert-compiler-plugin.backend"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(projectTests(":compiler:tests-common-new"))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    junit5Classpath(libs.junit.jupiter.api)
    powerAssertRuntimeClasspath(project(":kotlin-power-assert-runtime")) { isTransitive = false }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    dependsOn(":kotlin-power-assert-runtime:jvmJar")
    workingDir = rootDir
    useJUnitPlatform()

    val localJunit5Classpath: FileCollection = junit5Classpath
    val localPowerAssertRuntimeClasspath: FileCollection = powerAssertRuntimeClasspath

    doFirst {
        systemProperty("junit5.classpath", localJunit5Classpath.asPath)
        systemProperty("powerAssertRuntime.classpath", localPowerAssertRuntimeClasspath.asPath)
    }
}
