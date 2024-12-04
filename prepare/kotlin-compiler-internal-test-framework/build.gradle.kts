plugins {
    java
}

val testModules = listOf(
    ":compiler:test-infrastructure",
    ":compiler:test-infrastructure-utils",
    ":compiler:tests-compiler-utils",
    ":compiler:tests-common-new",
    ":generators:test-generator"
)

val mainModules = listOf(
    ":generators",
    ":compiler:tests-mutes",
    ":compiler:tests-mutes:mutes-junit5",
)

dependencies {
    mainModules.forEach {
        embedded(project(it)) { isTransitive = false }
    }
    testModules.forEach {
        embedded(projectTests(it)) { isTransitive = false }
    }
}

publish()
runtimeJar()
sourcesJar {
    from {
        mainModules.map { project(it).mainSourceSet.allSource } + testModules.map { project(it).testSourceSet.allSource }
    }

    dependsOn(":compiler:fir:checkers:generateCheckersComponents")
}

javadocJar()
