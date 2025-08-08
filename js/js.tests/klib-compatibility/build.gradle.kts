import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("jvm")
    alias(libs.plugins.gradle.node)
    id("d8-configuration")
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

node {
    download.set(true)
    version.set(nodejsVersion)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
    if (cacheRedirectorEnabled) {
        distBaseUrl.set("https://cache-redirector.jetbrains.com/nodejs.org/dist")
    }
}

dependencies {
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testImplementation(testFixtures(project(":js:js.tests")))
}

val customCompilerVersion = findProperty("kotlin.internal.js.test.compat.customCompilerVersion") as String
val customCompilerArtifacts: Configuration by configurations.creating

dependencies {
    customCompilerArtifacts("org.jetbrains.kotlin:kotlin-compiler-embeddable:$customCompilerVersion")
    customCompilerArtifacts("org.jetbrains.kotlin:kotlin-stdlib-js:$customCompilerVersion") {
        attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
    }
    customCompilerArtifacts("org.jetbrains.kotlin:kotlin-test-js:$customCompilerVersion") {
        attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
    }
}

val customCompilerArtifactsDir: Provider<Directory> = layout.buildDirectory.dir("customCompiler$customCompilerVersion")

val downloadCustomCompilerArtifacts: TaskProvider<Sync> by tasks.registering(Sync::class) {
    from(customCompilerArtifacts)
    into(customCompilerArtifactsDir)
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

fun Test.setUpJsBoxTests() {
    with(d8KotlinBuild) {
        setupV8()
    }
    dependsOn(":dist")
    systemProperty("kotlin.js.test.root.out.dir", "${node.nodeProjectDir.get().asFile}/")
    workingDir = rootDir
}

fun Test.setUpCustomCompiler() {
    dependsOn(downloadCustomCompilerArtifacts)
    systemProperty("kotlin.internal.js.test.compat.customCompilerArtifactsDir", customCompilerArtifactsDir.get().asFile.absolutePath)
    systemProperty("kotlin.internal.js.test.compat.customCompilerVersion", customCompilerVersion)
}

projectTest(
    taskName = "testCustomFirstPhase",
    jUnitMode = JUnitMode.JUnit5,
) {
    setUpJsBoxTests()
    setUpCustomCompiler()
    useJUnitPlatform { includeTags("custom-first-phase") }
}

projectTest(
    taskName = "testCustomSecondPhase",
    jUnitMode = JUnitMode.JUnit5,
) {
    setUpJsBoxTests()
    setUpCustomCompiler()
    useJUnitPlatform { includeTags("custom-second-phase") }
}

@Suppress("unused")
val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsKlibCompatibilityTestsKt") {
    dependsOn(":compiler:generateTestData")
}
