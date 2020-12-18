
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val depenencyProjects = arrayOf(
    ":generators",
    ":compiler",
    ":compiler:tests-for-compiler-generator",
    ":js:js.tests",
    ":compiler:tests-java8",
    ":core:descriptors.runtime"
)

dependencies {
    depenencyProjects.forEach {
        testCompile(projectTests(it))
        jpsTest(project(it, configuration = "jpsTest"))
    }

    testRuntimeOnly(files("${rootProject.projectDir}/dist/kotlinc/lib/kotlin-reflect.jar"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}
