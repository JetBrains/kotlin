
plugins {
    kotlin("jvm")
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
