plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
    }
}

testsJar {}
