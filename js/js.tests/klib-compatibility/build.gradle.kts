import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("jvm")
    alias(libs.plugins.gradle.node)
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
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testImplementation(testFixtures(project(":js:js.tests")))
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
    val configurationName = "customCompilerArtifacts_$version"

    val configuration = configurations.findByName(configurationName)
        ?: configurations.create(configurationName).also {
            project.dependencies.add(configurationName, "org.jetbrains.kotlin:kotlin-compiler-embeddable:${version.rawVersion}")
            project.dependencies.add(configurationName, "org.jetbrains.kotlin:kotlin-stdlib-js:${version.rawVersion}") {
                attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
            }
            project.dependencies.add(configurationName, "org.jetbrains.kotlin:kotlin-test-js:${version.rawVersion}") {
                attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
            }
        }

    return projectTests.testTask(taskName, jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
        setUpJsBoxTests(tag)
        addClasspathProperty(configuration, "kotlin.internal.js.test.compat.customCompilerArtifactsDir")
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

/* Custom-first-phase test tasks for different compiler versions. */
customFirstPhaseTest("1.9.20")
customFirstPhaseTest("2.0.0")
customFirstPhaseTest("2.1.0")
// TODO: Add a new task for the "custom-first-phase" test here.

tasks.test {
    // The default test task does not resolve the necessary dependencies and does not set up the environment.
    // Making it disabled to avoid running it accidentally.
    enabled = false
}

@Suppress("unused")
val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsKlibCompatibilityTestsKt") {
    dependsOn(":compiler:generateTestData")
}
