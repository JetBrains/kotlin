plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
}

// The tests depend on the published artifact alone, and on its JARs rather than on the project: a project dependency
// gives the IDE the project's empty classes directory, whereas the JARs hold the embedded content once they are built.
val artifact = configurations.dependencyScope("artifact")
val artifactClasspath = configurations.resolvable("artifactClasspath") {
    extendsFrom(artifact.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    artifact(project(":prepare:analysis-api:kotlin-analysis-api-syntax-implementation"))
    testImplementation(files(artifactClasspath))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

projectTests {
    testTask()
}
