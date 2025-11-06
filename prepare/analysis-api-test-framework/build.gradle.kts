plugins {
    java
}

val testModules = listOf(
    ":analysis:analysis-api-fir",
    ":analysis:low-level-api-fir",
    ":analysis:low-level-api-fir:tests-jdk11",
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-standalone",
    ":analysis:decompiled:decompiler-to-file-stubs",
)

val testFixturesModules = listOf(
    ":analysis:analysis-test-framework",
)

val mainModules = listOf(
    ":kotlin-preloader",
)

dependencies {
    testModules.forEach {
        embedded(projectTests(it)) { isTransitive = false }
    }

    testFixturesModules.forEach {
        embedded(testFixtures(project(it))) { (this as ModuleDependency).isTransitive = false }
    }

    mainModules.forEach {
        embedded(project(it)) { isTransitive = false }
    }
}

publish()
runtimeJar()
sourcesJar {
    from {
        mainModules.map { project(it).mainSourceSet.allSource } + testModules.map { project(it).testSourceSet.allSource }
    }
}
javadocJar()
