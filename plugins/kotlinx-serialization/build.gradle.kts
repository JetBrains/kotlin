import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val jsonJsIrRuntimeForTests: Configuration by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val coreJsIrRuntimeForTests: Configuration by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

dependencies {
    embedded(project(":kotlinx-serialization-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.cli")) { isTransitive = false }

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(project(":kotlinx-serialization-compiler-plugin.cli"))

    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(project(":compiler:fir:plugin-utils"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(projectTests(":js:js.tests"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(project(":kotlinx-serialization-compiler-plugin.common"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.k1"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.k2"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.backend"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.cli"))

    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.4.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    coreJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-core:1.4.1") { isTransitive = false }
    jsonJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1") { isTransitive = false }

    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

optInToExperimentalCompilerApi()

publish {
    artifactId = artifactId.replace("kotlinx-", "kotlin-")
}

val runtimeJar = runtimeJar {
    archiveBaseName.set("kotlin-serialization-compiler-plugin")
}
sourcesJar()
javadocJar()
testsJar()
useD8Plugin()

val distCompat by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val compatJar = tasks.register<Copy>("compatJar") {
    from(runtimeJar)
    into(layout.buildDirectory.dir("libsCompat"))
    rename {
        it.replace("kotlin-", "kotlinx-")
    }
}

artifacts {
    add(distCompat.name, compatJar) {
        builtBy(runtimeJar, compatJar)
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform()
    setUpJsIrBoxTests()
}

val generateTests by generator("org.jetbrains.kotlinx.serialization.TestGeneratorKt")

fun Test.setUpJsIrBoxTests() {
    useJsIrBoxTests(version = version, buildDir = "$buildDir/")

    val localJsCoreRuntimeForTests: FileCollection = coreJsIrRuntimeForTests
    val localJsJsonRuntimeForTests: FileCollection = jsonJsIrRuntimeForTests

    doFirst {
        systemProperty("serialization.core.path", localJsCoreRuntimeForTests.asPath)
        systemProperty("serialization.json.path", localJsJsonRuntimeForTests.asPath)
    }
}
