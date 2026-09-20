plugins {
    id("common-configuration")
    kotlin("jvm")
    // Required to make the test task cacheable, which is the whole point of this module.
    id("test-inputs-check")
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

/*
 * Whether the fixture's 'failing()' test fails, off unless a test asks for it. The task tolerates
 * the failure, the way 'ignoreFailures' or a retried flaky test does, so that a *successful* task
 * records a failed test - which is what makes a replayed 'testFailed' possible at all.
 */
val fixtureFails = providers.gradleProperty("testInventoryFixture.failing").map(String::toBoolean).orElse(false)

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // A system property because it is a task input: the two outcomes cannot be served to each
    // other from the build cache.
    systemProperty("testInventoryFixture.failing", fixtureFails.get())
    ignoreFailures = fixtureFails.get()
}

/*
 * The fixture's second test task: replaying a build with more than one of them takes two. They run
 * disjoint classes, so what 'test' records - asserted in full by the functional tests - is unchanged
 * by this one existing.
 */
val secondTestClass = "org.jetbrains.kotlin.testInventory.SecondTestInventoryFixtureTest"

tasks.test {
    filter { excludeTestsMatching(secondTestClass) }
}

tasks.register<Test>("secondTest") {
    group = "verification"
    description = "Runs $secondTestClass, so that a build can have two recordings to replay"

    val testSourceSet = sourceSets.test.get()
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    filter { includeTestsMatching(secondTestClass) }
}
