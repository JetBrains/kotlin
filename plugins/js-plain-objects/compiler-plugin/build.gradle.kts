import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

description = "JavaScript Plain Objects Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("d8-configuration")
    id("compiler-tests-convention")
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

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli"))

    testApi(testFixtures(project(":compiler:test-infrastructure")))
    testApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testApi(testFixtures(project(":compiler:tests-common-new")))

    testApi(testFixtures(project(":js:js.tests")))
    testFixtures(testFixtures(project(":generators:test-generator")))

    testApi(platform(libs.junit.bom))
    testFixtures(libs.junit.jupiter.api)
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
    "test" {
        projectDefault()
        java.srcDirs("testFixtures")
        generatedTestDir()
    }
}

publish {
    artifactId = "kotlinx-js-plain-objects-compiler-plugin"
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)

        workingDir = rootDir

        dependsOn(jsoIrRuntimeForTests)

        val localJsPlainObjectsIrRuntimePath: FileCollection = jsoIrRuntimeForTests

        doFirst {
            systemProperty("jso.runtime.path", localJsPlainObjectsIrRuntimePath.asPath)
        }
    }

    testGenerator("org.jetbrains.kotlinx.jspo.TestGeneratorKt")
}
