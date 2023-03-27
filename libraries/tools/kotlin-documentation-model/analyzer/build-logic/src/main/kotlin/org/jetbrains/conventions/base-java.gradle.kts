package org.jetbrains.conventions

/**
 * Base configuration for Java projects.
 *
 * This convention plugin contains shared Java config for both the [KotlinJvmPlugin] convention plugin and
 * the Gradle Plugin subproject (which cannot have the `kotlin("jvm")` plugin applied).
 */

plugins {
    id("org.jetbrains.conventions.base")
    java
}

java {
    toolchain {
        languageVersion.set(dokkaBuild.mainJavaVersion)
    }
}

java {
    withSourcesJar()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(dokkaBuild.testJavaLauncherVersion)
    })
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    // TODO Upgrade all subprojects to use JUnit Jupiter https://github.com/Kotlin/dokka/issues/2924
    //      Replace these dependencies with `testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")`
    //      See https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle-engines-configure
    // Ideally convention plugins should only provide sensible defaults that can be overridden by subprojects.
    // If a convention plugin defines dependencies, these cannot be easily overridden by subprojects, and so
    // this should be avoided. However, for now , both JUnit 4 and 5 must be supported, and since these are test
    // runtime-only dependencies they are not going to have a significant impact subprojects.
    // These dependencies should be revisited in #2924, and (for example) moved to each subproject (which is more
    // repetitive, but more declarative and clear), or some other solution.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    // kotlin-test asserts for all projects
    testImplementation(kotlin("test-junit"))
}
