
plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

dependencies {
    testApi(project(":kotlin-scripting-jvm"))
    testApi(project(":kotlin-scripting-compiler-impl"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

testsJar()
