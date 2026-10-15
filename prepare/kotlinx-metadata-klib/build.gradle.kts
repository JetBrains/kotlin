import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Library (KLIB) metadata manipulation library"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
}

group = "org.jetbrains.kotlinx"

base {
    archivesName.set("kotlinx-metadata-klib")
}

val deployVersion = findProperty("kotlinxMetadataKlibDeployVersion") as String?
version = deployVersion ?: "0.0.1-SNAPSHOT"

dependencies {
    api(kotlinStdlib())

    embedded(project(":kotlinx-metadata-klib")) { isTransitive = false }
    embedded(project(":kotlin-metadata")) { isTransitive = false }
    embedded(project(":core:compiler.common")) { isTransitive = false }
    embedded(project(":core:names")) { isTransitive = false }
    embedded(project(":core:deserialization")) { isTransitive = false }
    embedded(project(":core:deserialization.common")) { isTransitive = false }
    embedded(project(":compiler:serialization.common")) { isTransitive = false }
    embedded(project(":compiler:serialization")) { isTransitive = false }
    embedded(project(":kotlin-util-klib-metadata")) { isTransitive = false }
    embedded(project(":kotlin-util-klib")) { isTransitive = false }
    embedded(project(":kotlin-util-io")) { isTransitive = false }
    embedded(protobufLite()) { isTransitive = false }
}

val relocatedJar = tasks.register<ShadowJar>("relocatedJar") {
    archiveClassifier.set("shadow")
    configurations.add(project.configurations.getByName("embedded"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("**/*.proto")
    relocate("org.jetbrains.kotlin", "kotlin.metadata.internal")
}

val runtimeJar = tasks.register<Jar>("runtimeJar") {
    dependsOn(relocatedJar)
    from {
        zipTree(relocatedJar.get().outputs.files.singleFile)
    }
    setupPublicJar(base.archivesName.get())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("assemble").configure { dependsOn(runtimeJar) }

val sourcesJar = sourcesJar()
val javadocJar = javadocJar()

publish {
    setArtifacts(listOf(runtimeJar, sourcesJar, javadocJar))
}
