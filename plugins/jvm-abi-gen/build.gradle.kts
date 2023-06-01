import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "ABI generation for Kotlin/JVM"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val embedded by configurations
embedded.isTransitive = false
configurations.getByName("compileOnly").extendsFrom(embedded)
configurations.getByName("testApi").extendsFrom(embedded)

dependencies {
    // Should come before dependency on proguarded compiler because StringUtil methods are deleted from it
    testRuntimeOnly(intellijPlatformUtil()) { isTransitive = false }

    testRuntimeOnly(project(":kotlin-compiler"))

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":kotlin-build-common"))

    // Include kotlinx.metadata for metadata stripping.
    // Note that kotlinx-metadata-jvm already includes kotlinx-metadata, core:metadata, core:metadata.jvm,
    // and protobuf-lite, so we only need to include kotlinx-metadata-jvm in the shadow jar.
    compileOnly(project(":kotlinx-metadata"))
    embedded(project(":kotlinx-metadata-jvm"))

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    testImplementation(commonDependency("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:incremental-compilation-impl"))
}

optInToExperimentalCompilerApi()

publish()

runtimeJarWithRelocation {
    from(mainSourceSet.output)
    relocate("kotlinx.metadata", "org.jetbrains.kotlin.jvm.abi.kotlinx.metadata")
    mergeServiceFiles() // This is needed to relocate the services files for kotlinx.metadata
}

sourcesJar()

javadocJar()

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":dist")
}

testsJar()
