import org.gradle.kotlin.dsl.support.serviceOf

description = "Kotlin Assignment Compiler Plugin (Embeddable)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-assignment-compiler-plugin")) { isTransitive = false }
}

sourceSets {
    "main" { none() }
    "test" { none() }
}

val runtimeJar = runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

val sourcesJar = sourcesJar {
    val compilerTask = project(":kotlin-assignment-compiler-plugin").tasks.named<Jar>("sourcesJar")
    dependsOn(compilerTask)
    val archiveOperations = serviceOf<ArchiveOperations>()
    from(compilerTask.map { it.archiveFile }.map { archiveOperations.zipTree(it) })
}

val javadocJar = javadocJar {
    val compilerTask = project(":kotlin-assignment-compiler-plugin").tasks.named<Jar>("javadocJar")
    dependsOn(compilerTask)
    val archiveOperations = serviceOf<ArchiveOperations>()
    from(compilerTask.map { it.archiveFile }.map { archiveOperations.zipTree(it) })
}

publish {
    artifactId = artifactId.replace(".", "-")
    setArtifacts(listOf(runtimeJar, sourcesJar, javadocJar))
}
