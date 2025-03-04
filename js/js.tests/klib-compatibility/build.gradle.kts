import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("jvm")
    id("d8-configuration")
}

dependencies {
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testImplementation(projectTests(":js:js.tests"))
}

val latestReleasedCompiler = findProperty("kotlin.internal.js.test.releasedCompilerVersion") as String
val releasedCompiler: Configuration by configurations.creating

dependencies {
    releasedCompiler("org.jetbrains.kotlin:kotlin-compiler-embeddable:$latestReleasedCompiler")
    releasedCompiler("org.jetbrains.kotlin:kotlin-stdlib-js:$latestReleasedCompiler") {
        attributes {
            attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        }
    }
}

val releasedCompilerArtifactsTarget: Provider<Directory> = layout.buildDirectory.dir("releaseCompiler$latestReleasedCompiler")

val releasedCompilerDist: TaskProvider<Sync> by tasks.registering(Sync::class) {
    from(releasedCompiler)
    into(releasedCompilerArtifactsTarget)
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

    workingDir = rootDir
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(releasedCompilerDist)
    systemProperty("kotlin.internal.js.test.latestReleasedCompilerLocation", releasedCompilerArtifactsTarget.get().asFile.absolutePath)
    systemProperty("kotlin.internal.js.test.releasedCompilerVersion", latestReleasedCompiler)

    setUpJsBoxTests()
    useJUnitPlatform()
}

