
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val depenencyProjects = arrayOf(
    ":generators",
    ":compiler",
    ":compiler:test-infrastructure",
    ":compiler:tests-common-new",
    ":compiler:tests-for-compiler-generator",
    ":compiler:visualizer",
    ":js:js.tests",
    ":compiler:tests-java8",
    ":core:descriptors.runtime",
    ":generators:analysis-api-generator"
)

dependencies {
    depenencyProjects.forEach {
        testApi(projectTests(it))
        jpsTest(project(it, configuration = "jpsTest"))
    }

    testRuntimeOnly(files("${rootProject.projectDir}/dist/kotlinc/lib/kotlin-reflect.jar"))
    testRuntimeOnly(platform(commonDep("org.junit:junit-bom")))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter")
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}
