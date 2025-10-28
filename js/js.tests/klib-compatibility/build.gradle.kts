import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("jvm")
    alias(libs.plugins.gradle.node)
    id("java-test-fixtures")
    id("d8-configuration")
    id("project-tests-convention")
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
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testFixturesApi(testFixtures(project(":js:js.tests")))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { }
    "testFixtures" { projectDefault() }
    "test" { generatedTestDir() }
}

testsJar {}

fun Test.setUpJsBoxTests(tag: String) {
    with(d8KotlinBuild) {
        setupV8()
    }
    dependsOn(":dist")
    systemProperty("kotlin.js.test.root.out.dir", "${node.nodeProjectDir.get().asFile}/")

    dependsOn(":kotlin-stdlib:jsJar")
    systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/build/classes/kotlin/js/main")
    inputs.dir(rootDir.resolve("libraries/stdlib/build/classes/kotlin/js/main"))

    systemProperty("kotlin.js.stdlib.klib.path", "libraries/stdlib/build/libs/kotlin-stdlib-js-$version.klib")
    inputs.file(rootDir.resolve("libraries/stdlib/build/libs/kotlin-stdlib-js-$version.klib"))

    dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
    systemProperty("kotlin.js.reduced.stdlib.path", "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main")
    inputs.dir(rootDir.resolve("libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main"))

    dependsOn(":kotlin-test:jsJar")
    systemProperty("kotlin.js.kotlin.test.klib.path", "libraries/kotlin.test/build/libs/kotlin-test-js-$version.klib")
    inputs.file(rootDir.resolve("libraries/kotlin.test/build/libs/kotlin-test-js-$version.klib"))

    systemProperty("kotlin.js.full.test.path", "libraries/kotlin.test/build/classes/kotlin/js/main")
    inputs.dir(rootDir.resolve("libraries/kotlin.test/build/classes/kotlin/js/main"))

    useJUnitPlatform { includeTags(tag) }
    workingDir = rootDir
}

data class CustomCompilerVersion(val rawVersion: String) {
    val sanitizedVersion = rawVersion.replace('.', '_').replace('-', '_')
    override fun toString() = sanitizedVersion
}

fun Test.addClasspathProperty(configuration: Configuration, property: String) {
    val classpathProvider = objects.newInstance<SystemPropertyClasspathProvider>()
    classpathProvider.classpath.from(configuration)
    classpathProvider.property.set(property)
    jvmArgumentProviders.add(classpathProvider)
}

abstract class SystemPropertyClasspathProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    abstract val property: Property<String>

    override fun asArguments(): Iterable<String> {
        return listOf(
            "-D${property.get()}=${classpath.asPath}"
        )
    }
}

fun Project.customCompilerTest(
    version: CustomCompilerVersion,
    taskName: String,
    tag: String,
): TaskProvider<out Task> {
    val customCompiler: Configuration = getOrCreateConfiguration("customCompiler_$version") {
        project.dependencies.add(name, "org.jetbrains.kotlin:kotlin-compiler-embeddable:${version.rawVersion}")
    }

    val runtimeDependencies: Configuration = getOrCreateConfiguration("customCompilerRuntimeDependencies_$version") {
        project.dependencies.add(name,"org.jetbrains.kotlin:kotlin-stdlib-js:${version.rawVersion}") {
            attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
        }
        project.dependencies.add(name, "org.jetbrains.kotlin:kotlin-test-js:${version.rawVersion}") {
            attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
        }
    }

    return projectTests.testTask(taskName, jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
        setUpJsBoxTests(tag)
        addClasspathProperty(customCompiler, "kotlin.internal.js.test.compat.customCompilerClasspath")
        addClasspathProperty(runtimeDependencies, "kotlin.internal.js.test.compat.runtimeDependencies")
        systemProperty("kotlin.internal.js.test.compat.customCompilerVersion", version.rawVersion)
    }
}

fun Project.customFirstPhaseTest(rawVersion: String): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)

    return customCompilerTest(
        version = version,
        taskName = "testCustomFirstPhase_$version",
        tag = "custom-first-phase"
    )
}

fun Project.customSecondPhaseTest(rawVersion: String): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)
    return customCompilerTest(
        version = version,
        taskName = "testCustomSecondPhase_$version",
        tag = "custom-second-phase"
    )
}

/* Custom-first-phase test tasks for different compiler versions. */
customFirstPhaseTest("1.9.20")
customFirstPhaseTest("2.0.0")
customFirstPhaseTest("2.1.0")
customFirstPhaseTest("2.2.0")
// TODO: Add a new task for the "custom-first-phase" test here.

/* Custom-second-phase test task for the previous and latest compiler major versions. */
// TODO: Update two following compiler versions to the previous and latest ones.
customSecondPhaseTest("2.2.0")
customSecondPhaseTest("2.3.0-Beta2")

tasks.test {
    // The default test task does not resolve the necessary dependencies and does not set up the environment.
    // Making it disabled to avoid running it accidentally.
    enabled = false
}

projectTests {
    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateJsKlibCompatibilityTestsKt")
}

