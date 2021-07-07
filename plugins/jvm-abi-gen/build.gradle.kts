import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "ABI generation for Kotlin/JVM"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val shadows: Configuration by configurations.creating {
    isTransitive = false
}
configurations.getByName("compileOnly").extendsFrom(shadows)
configurations.getByName("testCompile").extendsFrom(shadows)

dependencies {
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
    shadows(project(":kotlinx-metadata-jvm"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }

    testRuntimeOnly(project(":kotlin-compiler"))

    testImplementation(commonDep("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:incremental-compilation-impl"))
    testRuntimeOnly(intellijCoreDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

noDefaultJar()
runtimeJar(tasks.register<ShadowJar>("shadowJar")) {
    callGroovy("manifestAttributes", manifest, project)
    manifest.attributes["Implementation-Version"] = archiveVersion

    from(mainSourceSet.output)
    configurations = listOf(shadows)
    relocate("kotlinx.metadata", "org.jetbrains.kotlin.jvm.abi.kotlinx.metadata")
    mergeServiceFiles() // This is needed to relocate the services files for kotlinx.metadata
}

val test by tasks
test.dependsOn("shadowJar")

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":dist")
}

publish()

sourcesJar()
javadocJar()

testsJar()