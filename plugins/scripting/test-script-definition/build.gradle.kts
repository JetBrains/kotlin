
plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":kotlin-scripting-jvm"))
    testFixturesApi(project(":kotlin-scripting-compiler-impl"))
}

sourceSets {
    "main" {}
    "test" { none() }
    "testFixtures" { projectDefault() }
}

testsJar()
