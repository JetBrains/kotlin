
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val dependency = arrayOf(
    ":generators",
    ":compiler",
    ":compiler:test-infrastructure",
    ":compiler:tests-for-compiler-generator",
    ":compiler:tests-java8",
    ":core:descriptors.runtime",
    ":generators:analysis-api-generator"
)

val dependencyFixturesProjects = arrayOf(
    ":js:js.tests",
    ":compiler:tests-common-new",
)

dependencies {
    dependency.forEach {
        testApi(projectTests(it))
        jpsTest(project(it, configuration = "jpsTest"))
    }

    dependencyFixturesProjects.forEach {
        testApi(testFixtures(project(it)))
        jpsTest(project(it, configuration = "jpsTest"))
    }

    testRuntimeOnly(files("${rootProject.projectDir}/dist/kotlinc/lib/kotlin-reflect.jar"))
    testRuntimeOnly(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)

    if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testRuntimeOnly(project(":core:descriptors.runtime"))
    }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}
