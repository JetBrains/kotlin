
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val depenencyProjects = arrayOf(
    ":generators", ":compiler", ":js:js.tests", ":compiler:tests-java8"
)

dependencies {
    depenencyProjects.forEach {
        testCompile(projectTests(it))
        jpsTest(project(it, configuration = "jpsTest"))
    }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}