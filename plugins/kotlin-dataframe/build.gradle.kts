plugins {
    kotlin("jvm")
}

dependencies {
    embedded(project(":kotlin-dataframe-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-dataframe-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-dataframe-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-dataframe-compiler-plugin.cli")) { isTransitive = false }

    testApi(project(":kotlin-dataframe-compiler-plugin.cli"))
    testRuntimeOnly(libs.dataframe.core.dev)
    testRuntimeOnly(libs.dataframe.csv.dev)
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:fir:analysis-tests"))
    testApi(projectTests(":js:js.tests"))
    testApi(project(":compiler:fir:plugin-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

publish {
    artifactId = "kotlin-dataframe-compiler-plugin-experimental"
}
runtimeJar()
sourcesJar()
javadocJar()
testsJar()

optInToExperimentalCompilerApi()