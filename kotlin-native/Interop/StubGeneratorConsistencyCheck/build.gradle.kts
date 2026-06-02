plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    testFixturesImplementation(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.junit.jupiter.engine)
}

sourceSets {
    main { none() }
    testFixtures {
        projectDefault()
    }
}