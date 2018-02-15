
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    jpsTest(project(":generators", configuration = "jpsTest"))
    jpsTest(project(":compiler", configuration = "jpsTest"))
    jpsTest(project(":js:js.tests", configuration = "jpsTest"))
    jpsTest(project(":compiler:tests-java8", configuration = "jpsTest"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}