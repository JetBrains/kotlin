plugins {
    java
}

val testFixturesModules = listOf(
    ":analysis:analysis-api-fir",
    ":analysis:low-level-api-fir",
    ":analysis:analysis-test-framework",
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-standalone",
    ":analysis:decompiled:decompiler-to-file-stubs",
)

val testModules = listOf(
    ":analysis:low-level-api-fir:tests-jdk11",
)

val mainModules = listOf(
    ":kotlin-preloader",
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
    testModules.registerDependencies { projectTests(it) }
}

publish()
runtimeJar()
sourcesJar {
    from {
        mainModules.map { project(it).mainSourceSet.allSource } +
                testFixturesModules.map { project(it).testFixturesSourceSet.allSource } +
                testModules.map { project(it).testSourceSet.allSource }
    }
}
javadocJar()
