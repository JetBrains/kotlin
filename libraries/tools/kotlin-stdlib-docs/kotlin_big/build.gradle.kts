plugins {
    base
}

val isTeamcityBuild = project.hasProperty("teamcity.version")

// kotlin/libraries/tools/kotlin-stdlib-docs  ->  kotlin
val kotlinRootDir = rootProject.file("../../../").absoluteFile.invariantSeparatorsPath
val kotlinLibsDir = "$buildDir/libs"

val githubRevision = if (isTeamcityBuild) project.property("githubRevision") else "master"
val kotlinVersion = if (isTeamcityBuild) project.property("deployVersion") as String else "1.8.255-SNAPSHOT"
val repo = if (isTeamcityBuild) project.property("kotlinLibsRepo") as String else "$kotlinRootDir/build/repo"

println("# Parameters summary:")
println("    isTeamcityBuild: $isTeamcityBuild")
println("    githubRevision: $githubRevision")
println("    kotlinVersion: $kotlinVersion")
println("    dokkaVersion: ${property("dokka_version")}")
println("    repo: $repo")

repositories {
    maven(url = repo)
    mavenCentral()
}

val modules = listOf(
    "kotlin-stdlib",
    "kotlin-stdlib-common",
    "kotlin-stdlib-jdk7",
    "kotlin-stdlib-jdk8",
    "kotlin-stdlib-js",
    "kotlin-test",
    "kotlin-test-js",
    "kotlin-test-junit5",
    "kotlin-test-junit",
    "kotlin-test-testng",
    "kotlin-test-common",
)


val extractLibs by tasks.registering(Task::class)


modules.forEach { module ->

    val library = configurations.create("kotlin_lib_$module")

    if (module == "kotlin-test-js") {
        library.attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, "kotlin-runtime")) }
    }

    dependencies {
        library(group = "org.jetbrains.kotlin", name = module, version = kotlinVersion)
    }

    val libsTask = tasks.register<Sync>("extract_lib_$module") {
        dependsOn(library)

        from({ library })
        into("$kotlinLibsDir/$module")
    }

    extractLibs.configure { dependsOn(libsTask) }
}

project.ext["github_revision"] = githubRevision
project.ext["kotlin_root"] = kotlinRootDir
project.ext["kotlin_libs"] = kotlinLibsDir
