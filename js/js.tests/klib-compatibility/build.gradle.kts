import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("jvm")
    id("d8-configuration")
    id("compiler-tests-convention")
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
        attributes {
            attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        }
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

    workingDir = rootDir
}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(downloadCustomCompilerArtifacts)
        systemProperty("kotlin.internal.js.test.compat.customCompilerArtifactsDir", customCompilerArtifactsDir.get().asFile.absolutePath)
        systemProperty("kotlin.internal.js.test.compat.customCompilerVersion", customCompilerVersion)

        setUpJsBoxTests()
    }
}

