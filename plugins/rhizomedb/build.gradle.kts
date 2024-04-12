import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

description = "Fleet RhizomeDB Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

publish {}

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
    embedded(project(":rhizomedb-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":rhizomedb-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":rhizomedb-compiler-plugin.cli")) { isTransitive = false }

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

    testImplementation(project(":rhizomedb-compiler-plugin.k2"))
    testImplementation(project(":rhizomedb-compiler-plugin.backend"))
    testImplementation(project(":rhizomedb-compiler-plugin.cli"))

    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

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

val archiveName = "rhizomedb-compiler-plugin"
val archiveCompatName = "rhizomedb-compiler-plugin"

val runtimeJar = runtimeJar {
    archiveBaseName.set(archiveName)
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
    add(distCompat.name, layout.buildDirectory.dir("libsCompat").map { it.file("$archiveCompatName-$version.jar") }) {
        builtBy(runtimeJar, compatJar)
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform()
}

val generateTests by generator("org.jetbrains.rhizomedb.TestGeneratorKt")
