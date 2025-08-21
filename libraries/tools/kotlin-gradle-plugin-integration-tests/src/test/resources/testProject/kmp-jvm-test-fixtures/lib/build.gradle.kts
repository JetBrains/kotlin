plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

kotlin.jvmToolchain(21)

dependencies {
    api(project(":runtime"))

    testFixturesImplementation(testFixtures(project(":runtime")))
}
