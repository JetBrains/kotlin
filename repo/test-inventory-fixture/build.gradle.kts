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
 * Whether the fixture's 'failing()' test fails, off unless a test asks for it, so that an ordinary
 * run of this module stays green.
 *
 * The task tolerates the failure, the way a task with 'ignoreFailures' or a retried flaky test does,
 * so that a *successful* task records a failed test - which is what makes a replayed 'testFailed'
 * something that can happen at all.
 */
val fixtureFails = providers.gradleProperty("testInventoryFixture.failing").map(String::toBoolean).orElse(false)

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // A system property rather than a plain flag: the fixture reads it, and it is a task input, so
    // the two outcomes cannot be served to each other from the build cache.
    systemProperty("testInventoryFixture.failing", fixtureFails.get())
    ignoreFailures = fixtureFails.get()
}
