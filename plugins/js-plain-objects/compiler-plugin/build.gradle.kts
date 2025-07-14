import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

description = "JavaScript Plain Objects Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("d8-configuration")
    id("java-test-fixtures")
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

    testFixturesApi(project(":compiler:backend"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli"))

    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))

    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesImplementation(projectTests(":generators:test-generator"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    if (!project.kotlinBuildProperties.isInJpsBuildIdeaSync) {
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

    testRuntimeOnly(project(":core:descriptors.runtime"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

publish {
    artifactId = "kotlinx-js-plain-objects-compiler-plugin"
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
    useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)

    workingDir = rootDir

    dependsOn(jsoIrRuntimeForTests)

    val localJsPlainObjectsIrRuntimePath: FileCollection = jsoIrRuntimeForTests

    doFirst {
        systemProperty("jso.runtime.path", localJsPlainObjectsIrRuntimePath.asPath)
    }
}

val generateTests by generator("org.jetbrains.kotlinx.jspo.TestGeneratorKt")
