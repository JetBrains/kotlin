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

fun Project.customSecondPhaseTest(rawVersion: String): TaskProvider<out Task> = customCompilerTest(
    version = CustomCompilerVersion(rawVersion),
    taskName = "testCustomSecondPhase",
    tag = "custom-second-phase"
)

/* Custom-first-phase test tasks for different compiler versions. */
customFirstPhaseTest("1.9.20")
customFirstPhaseTest("2.0.0")
customFirstPhaseTest("2.1.0")
customFirstPhaseTest("2.2.0")
// TODO: Add a new task for the "custom-first-phase" test here.

/* Custom-second-phase test task for the latest compiler version. */
// TODO: Update the compiler version to the latest one.
customSecondPhaseTest("2.2.0")

tasks.test {
    // The default test task does not resolve the necessary dependencies and does not set up the environment.
    // Making it disabled to avoid running it accidentally.
    enabled = false
}

projectTests {
    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateJsKlibCompatibilityTestsKt")
}

