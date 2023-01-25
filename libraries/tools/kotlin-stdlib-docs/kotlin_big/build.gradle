apply plugin: 'base'

final boolean isTeamcityBuild = project.hasProperty("teamcity.version")

// kotlin/libraries/tools/kotlin-stdlib-docs  ->  kotlin
final String kotlinRootDir = rootProject.file("../../../").absolutePath.replace('\\', '/')
final String kotlinLibsDir = "$buildDir/libs"

final String githubRevision = isTeamcityBuild ? project.property("githubRevision") : "master"
final String kotlinVersion = isTeamcityBuild ? project.property("deployVersion") : "1.6.255-SNAPSHOT"
final String repo = isTeamcityBuild ? project.property("kotlinLibsRepo") : "$kotlinRootDir/build/repo"

println("# Extracting info:")
println("    isTeamcityBuild: $isTeamcityBuild")
println("    githubRevision: $githubRevision")
println("    kotlinVersion: $kotlinVersion")
println("    repo: $repo")

repositories {
  maven { url = repo }
  mavenCentral()
}

final List<String> modules = [
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
]


task extractLibs() { }


modules.forEach { module ->
  final String lib = "kotlin_lib_$module"

  final Configuration  lib_src = configurations.create(lib)

  if (module == "kotlin-test-js") {
    lib_src.attributes {attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, "kotlin-runtime")) }
  }

  dependencies {
    "$lib"(group: 'org.jetbrains.kotlin', name: module, version: kotlinVersion)
  }

  final Task libsTask = tasks.create("extract_lib_$module", Sync) {
    dependsOn lib_src

    from { lib_src }
    into "$kotlinLibsDir/$module"
  }

  extractLibs.dependsOn libsTask
}

project.extensions.github_revision = githubRevision
project.extensions.kotlin_root = kotlinRootDir
project.extensions.kotlin_libs = kotlinLibsDir
