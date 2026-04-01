plugins {
    java
}

val testFixturesModules = listOf(
    ":compiler:test-infrastructure",
    ":compiler:test-infrastructure-utils.common",
    ":compiler:test-infrastructure-utils",
    ":compiler:tests-compiler-utils",
    ":compiler:tests-common-new",
    ":generators:test-generator",
    ":kotlin-build-common",
    ":js:js.tests",
)

val mainModules = listOf(
    ":generators",
    ":compiler:tests-mutes",
    ":compiler:tests-mutes:mutes-junit5",
)

dependencies {
    fun List<String>.registerDependencies(notation: (String) -> Dependency) {
        this.forEach {
            embedded(notation(it)) {
                if (this is ModuleDependency) isTransitive = false
            }
        }
    }

    mainModules.registerDependencies { project(it) }
    testFixturesModules.registerDependencies { testFixtures(project(it)) }
}

publish()
runtimeJar()
sourcesJar {
    from {
        mainModules.map { project(it).mainSourceSet.allSource } +
                testFixturesModules.map { project(it).testFixturesSourceSet.allSource }
    }
}

javadocJar()
