plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("1.6")

configurations.implementation {
    /**
     * Files with the same FQNs may exist both in these excluded artifacts and in dependencies of
     * `org.apache.maven:maven-core`. If these classes fall into classpath together, it
     * may lead to unpredictable errors, especially if these classes have different APIs.
     *
     * Example. Class `org.eclipse.aether.repository.RemoteRepository` is presented in both
     * `maven-resolver-api-1.6.2` (which is resolved transitively from `maven-core`)
     * and `aether-api-1.1.0` artifacts. One of the implementations has `isBlocked()`
     * method, and another one lacks it. It leads to `NoSuchMethodError` in runtime.
     */
    exclude("org.eclipse.aether", "aether-api")
    exclude("org.eclipse.aether", "aether-util")
    exclude("org.eclipse.aether", "aether-spi")
}

dependencies {
    implementation(kotlinStdlib())
    api(project(":kotlin-scripting-dependencies"))
    implementation("org.eclipse.aether:aether-connector-basic:1.1.0")
    implementation("org.eclipse.aether:aether-transport-wagon:1.1.0")
    implementation("org.eclipse.aether:aether-transport-file:1.1.0")
    implementation("org.apache.maven:maven-core:3.8.1")
    implementation("org.apache.maven.wagon:wagon-http:3.4.3")
    testImplementation(projectTests(":kotlin-scripting-dependencies"))
    testImplementation(commonDep("junit"))
    testRuntimeOnly("org.slf4j:slf4j-nop:1.7.30")
    testImplementation(kotlin("reflect"))
    testImplementation(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package"
    )
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
