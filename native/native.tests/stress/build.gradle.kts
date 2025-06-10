plugins {
    kotlin("jvm")
    id("compiler-tests-convention")
    id("test-inputs-check")
}

val embeddable = configurations.dependencyScope("embeddable")
val embeddableElements = configurations.resolvable("embeddableElements") {
    extendsFrom(embeddable.get())
}
dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(projectTests(":native:native.tests"))
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        embeddable(project(":kotlin-native:prepare:kotlin-native-compiler-embeddable"))
    }
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

compilerTests {
    testData(project.isolated, "testData")
}

nativeTest(
    "test",
    null,
    requirePlatformLibs = true,
    allowParallelExecution = false, // Stress tests are resource-intensive tests and they must be run in isolation.
) {
    extensions.configure<TestInputsCheckExtension>() {
        isNative.set(true)
        useXcode.set(true)
    }
    // nativeTest sets workingDir to rootDir so here we need to override it
    workingDir = projectDir

    inputs
        .files(embeddableElements)
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("kotlinNativeCompilerEmbeddable")
}