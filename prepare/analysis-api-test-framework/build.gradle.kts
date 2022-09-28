plugins {
    java
}

val testModules = listOf(
    ":analysis:analysis-api-fir",
    ":analysis:low-level-api-fir",
    ":analysis:analysis-test-framework",
    ":analysis:analysis-api-impl-barebone",
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-standalone"
)

dependencies {
    testModules.forEach {
        embedded(projectTests(it)) { isTransitive = false }
    }
}

publish()
runtimeJar()
sourcesJar {
    from {
        testModules.map { project(it).testSourceSet.allSource }
    }
}
javadocJar()
