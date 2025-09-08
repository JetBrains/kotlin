plugins {
    kotlin("jvm")
}

dependencies {
    testApi(testFixtures(project(":compiler:tests-common-new")))
    testApi(libs.junit.jupiter.api)
    testApi(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
    }
}

testsJar {}
