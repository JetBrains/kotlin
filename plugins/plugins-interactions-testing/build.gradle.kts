plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

val beforePluginClasspath: Configuration by configurations.creating
val middlePluginClasspath: Configuration by configurations.creating
val afterPluginClasspath: Configuration by configurations.creating

dependencies {
    testFixturesApi(testFixtures(project(":kotlin-allopen-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-assignment-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlinx-serialization-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-lombok-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-noarg-compiler-plugin")))
    testFixturesApi(testFixtures(project(":plugins:parcelize:parcelize-compiler")))

    testFixturesApi(testFixtures(project(":compiler:tests-integration")))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }

    beforePluginClasspath(project(":plugins:test-plugins:before"))
    middlePluginClasspath(project(":plugins:test-plugins:middle"))
    afterPluginClasspath(project(":plugins:test-plugins:after"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

runtimeJar()
sourcesJar()
testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()

    addClasspathProperty(beforePluginClasspath, "plugin.classpath.before")
    addClasspathProperty(middlePluginClasspath, "plugin.classpath.middle")
    addClasspathProperty(afterPluginClasspath, "plugin.classpath.after")
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
