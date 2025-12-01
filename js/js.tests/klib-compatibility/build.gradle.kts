import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("jvm")
    alias(libs.plugins.gradle.node)
    id("java-test-fixtures")
    id("d8-configuration")
    id("nodejs-configuration")
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
        systemProperty("kotlin.js.stdlib.klib.path", "libraries/stdlib/build/libs/kotlin-stdlib-js-$version.klib")
    }
}

fun Project.customFirstStageTest(rawVersion: String): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)

    return customCompilerTest(
        version = version,
        taskName = "testCustomFirstStage_$version",
        tag = "custom-first-stage"
    )
}

fun Project.customSecondStageTest(rawVersion: String): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)
    return customCompilerTest(
        version = version,
        taskName = "testCustomSecondStage_$version",
        tag = "custom-second-stage"
    )
}

/* Custom-first-stage test tasks for different compiler versions. */
customFirstStageTest("1.9.20")
customFirstStageTest("2.0.0")
customFirstStageTest("2.1.0")
customFirstStageTest("2.2.0")
// TODO: Add a new task for the "custom-first-stage" test here.

/* Custom-second-stage test task for the two compiler major versions: previous one and the latest one . */
// TODO: Keep updating two following compiler versions to be the previous and latest ones.
customSecondStageTest("2.2.0")
customSecondStageTest("2.3.0-Beta2")

tasks.test {
    // The default test task does not resolve the necessary dependencies and does not set up the environment.
    // Making it disabled to avoid running it accidentally.
    enabled = false
}

projectTests {
    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateJsKlibCompatibilityTestsKt")
    testData(project(":compiler").isolated, "testData/codegen/box")
    testData(project(":compiler").isolated, "testData/codegen/boxInline")
    testData(project(":js:js.translator").isolated, "testData")

    withJsRuntime()
    withStdlibCommon()
}

