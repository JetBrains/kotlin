import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

description = "JavaScript Plain Objects Compiler Plugin"

plugins {
    kotlin("jvm")
    id("d8-configuration")
    id("project-tests-convention")
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

    testImplementation(project(":compiler:backend"))
    testImplementation(project(":compiler:cli"))
    testImplementation(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli"))

    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(testFixtures(project(":compiler:tests-compiler-utils")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))

    testImplementation(testFixtures(project(":js:js.tests")))
    testFixtures(testFixtures(project(":generators:test-generator")))

    testImplementation(platform(libs.junit.bom))
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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)

        workingDir = rootDir

        dependsOn(jsoIrRuntimeForTests)

        val localJsPlainObjectsIrRuntimePath: FileCollection = jsoIrRuntimeForTests

        doFirst {
            systemProperty("jso.runtime.path", localJsPlainObjectsIrRuntimePath.asPath)
        }
    }

    testGenerator("org.jetbrains.kotlinx.jspo.TestGeneratorKt", doNotSetFixturesSourceSetDependency = true)
}
