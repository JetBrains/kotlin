import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

description = "JavaScript Plain Objects Compiler Plugin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("d8-configuration")
    id("project-tests-convention")
    id("test-inputs-check")
}

val jsoIrRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
    }
}

dependencies {
    embedded(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.common")) { isTransitive = false }
    embedded(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.k2")) { isTransitive = false }
    embedded(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.backend")) { isTransitive = false }
    embedded(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli")) { isTransitive = false }

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":js:js.tests")))

    testFixturesImplementation(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli"))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testFixturesApi(libs.junit.jupiter.api)
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)

    jsoIrRuntimeForTests(project(":plugins:js-plain-objects:runtime")) { isTransitive = false }

    embedded(project(":plugins:js-plain-objects:runtime")) {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
            attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        }
        isTransitive = false
    }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "testFixtures" { projectDefault() }
}

publish {
    artifactId = "kotlinx-js-plain-objects-compiler-plugin"
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useJsIrBoxTests(buildDir = layout.buildDirectory)
        addClasspathProperty(jsoIrRuntimeForTests, "jso.runtime.path")
    }

    testGenerator("org.jetbrains.kotlinx.jspo.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJsRuntime()

    testData(project(":js:js.translator").isolated, "testData/_commonFiles")
    testData(project(":plugins:js-plain-objects:compiler-plugin").isolated, "testData")
}
