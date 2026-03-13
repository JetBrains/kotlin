plugins {
    base
    `dokka-convention`
}

val isTeamcityBuild = project.hasProperty("teamcity.version") || System.getenv("TEAMCITY_VERSION") != null

// kotlin/libraries/tools/kotlin-stdlib-docs  ->  kotlin
val kotlin_root = rootProject.file("../../../").absoluteFile.invariantSeparatorsPath
val kotlin_libs by extra("${layout.buildDirectory}/libs")

val rootProperties = java.util.Properties().apply {
    file(kotlin_root).resolve("gradle.properties").inputStream().use { stream -> load(stream) }
}
val defaultSnapshotVersion: String by rootProperties
val kotlinLanguageVersion: String by rootProperties

val githubRevision = if (isTeamcityBuild) project.property("githubRevision") else "master"
val artifactsVersion by extra(if (isTeamcityBuild) project.property("deployVersion") as String else defaultSnapshotVersion)
val artifactsRepo by extra(if (isTeamcityBuild) project.property("kotlinLibsRepo") as String else "$kotlin_root/build/repo")
val dokka_version: String by project

println("# Parameters summary:")
println("    isTeamcityBuild: $isTeamcityBuild")
println("    dokka version: $dokka_version")
println("    githubRevision: $githubRevision")
println("    language version: $kotlinLanguageVersion")
println("    artifacts version: $artifactsVersion")
println("    artifacts repo: $artifactsRepo")


val outputDir = file(findProperty("docsBuildDir") as String? ?: "${layout.buildDirectory}/doc")
val inputDirPrevious = file(findProperty("docsPreviousVersionsDir") as String? ?: "$outputDir/previous")
val outputDirPartial = outputDir.resolve("partial")
val kotlin_native_root = file("$kotlin_root/kotlin-native").absolutePath
val templatesDir = file(findProperty("templatesDir") as String? ?: "$projectDir/templates").invariantSeparatorsPath

val cleanDocs by tasks.registering(Delete::class) {
    delete(outputDir)
}

tasks.clean {
    dependsOn(cleanDocs)
}

val prepare by tasks.registering {
    dependsOn(":kotlin_big:extractLibs")
}

(getTasksByName("dokkaGenerateHtml", false) + getTasksByName("dokkaGenerate", false)).forEach {
    it.dependsOn(prepare)
}

dependencies {
    dokka(project(":kotlin-stdlib"))
    dokka(project(":kotlin-test"))
    dokka(project(":kotlin-reflect"))
}
version = (findProperty("version") as String?).takeIf { it != "unspecified"}  ?: kotlinLanguageVersion
