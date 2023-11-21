plugins {
    java
}

val testModules = listOf(
    ":analysis:analysis-api-fir",
    ":analysis:low-level-api-fir",
    ":analysis:low-level-api-fir:tests-jdk11",
    ":analysis:analysis-test-framework",
    ":analysis:analysis-api-impl-barebone",
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-standalone",
    ":analysis:decompiled:decompiler-to-file-stubs",
)

val mainModules = listOf(
    ":kotlin-preloader",
)

dependencies {
    testModules.forEach {
        embedded(projectTests(it)) { isTransitive = false }
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
