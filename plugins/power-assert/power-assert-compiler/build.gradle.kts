import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages

description = "Kotlin Power-Assert Compiler Plugin"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

val junit5Classpath = configurations.create("junit5Classpath")

val powerAssertRuntimeClasspath = configurations.dependencyScope("powerAssertRuntimeClasspath").get()

val powerAssertJvmRuntimeClasspath = configurations.resolvable("powerAssertJvmRuntimeClasspath") {
    extendsFrom(powerAssertRuntimeClasspath)
}.get()

val powerAssertJsRuntimeClasspath = configurations.resolvable("powerAssertJsRuntimeClasspath") {
    extendsFrom(powerAssertRuntimeClasspath)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}.get()

dependencies {
    embedded(project(":kotlin-power-assert-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-power-assert-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-power-assert-compiler-plugin.frontend")) { isTransitive = false }
    embedded(project(":kotlin-power-assert-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":kotlin-power-assert-compiler-plugin.backend"))
    testFixturesApi(project(":kotlin-power-assert-compiler-plugin.frontend"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    junit5Classpath(libs.junit.jupiter.api)
    powerAssertRuntimeClasspath(project(":kotlin-power-assert-runtime")) { isTransitive = false }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        generatedTestDir()
        projectDefault()
    }
    "testFixtures" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTests {
    testTask {
        addClasspathProperty(junit5Classpath, "junit5.classpath")
        addClasspathProperty(powerAssertJvmRuntimeClasspath, "powerAssertRuntime.jvm.classpath")
        addClasspathProperty(powerAssertJsRuntimeClasspath, "powerAssertRuntime.js.classpath")
    }

    testGenerator("org.jetbrains.kotlin.powerassert.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()

    testData(project.isolated, "testData")
}
