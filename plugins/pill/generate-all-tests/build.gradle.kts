
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val dependency = arrayOf(
    ":generators",
    ":compiler",
    ":core:descriptors.runtime",
)

val dependencyFixturesProjects = arrayOf(
    ":compiler:tests-java8",
    ":js:js.tests",
    ":compiler:tests-common-new",
    ":compiler:test-infrastructure",
    ":generators:analysis-api-generator",
)

dependencies {
    dependency.forEach {
        testImplementation(projectTests(it))
        jpsTest(project(it, configuration = "jpsTest"))
    }

    dependencyFixturesProjects.forEach {
        testImplementation(testFixtures(project(it)))
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
