import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import kotlin.io.path.readLines

plugins {
    application
    kotlin("jvm")
    id("jps-compatible")
}


dependencies {
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:cli"))
    implementation(intellijCore())
    implementation(kotlinStdlib())

    // runtime dependencies for IJ
    runtimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    runtimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    runtimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    runtimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))

    // test dependencies
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform()
}

application {
    mainClass.set("org.jetbrains.kotlin.ide.plugin.dependencies.validator.MainKt")
}

val projectsUsedInIntelliJKotlinPlugin: Array<String> by rootProject.extra
val kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin: String by rootProject.extra

tasks.withType<JavaExec> {
    workingDir = rootProject.projectDir

    doFirst {
        args = projectsUsedInIntelliJKotlinPlugin.flatMap {
            project(it).extensions
                .findByType(JavaPluginExtension::class.java)
                ?.sourceSets?.flatMap { sourceSet ->
                    sourceSet.allSource.srcDirs.map { it.path }
                }.orEmpty()
        }
    }
}

tasks.register("checkIdeDependenciesConfiguration") {
    notCompatibleWithConfigurationCache("Uses project in task action")
    doFirst {
        for (projectName in projectsUsedInIntelliJKotlinPlugin) {
            project(projectName).checkIdeDependencyConfiguration()
        }
    }
}

fun Project.checkIdeDependencyConfiguration() {
    val expectedApiVersion = KotlinVersion.fromVersion(kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin)
    for (compileTask in tasks.withType<KotlinJvmCompile>()) {
        val projectApiVersion = compileTask.compilerOptions.apiVersion.get()
        check(projectApiVersion <= expectedApiVersion) {
            "Expected the API Version to be less or equal to `$kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin`" +
                    " for the project `$name`, " +
                    "but `$projectApiVersion` found. The project is used in the IntelliJ, so it should use the same API version" +
                    "for binary compatibility with Kotlin stdlib . " +
                    "See KT-62510 for details."
        }

        val enabledExperimentalAnnotations =
            ExperimentalAnnotationsCollector().getUsedExperimentalAnnotations(compileTask.kotlinOptions.freeCompilerArgs)

        check(enabledExperimentalAnnotations.isEmpty()) {
            "`$name` allows using experimental kotlin stdlib API marked with ${enabledExperimentalAnnotations.joinToString()}. " +
                    "The project is used in the IntelliJ Kotlin Plugin, so it cannot use experimental Kotlin stdlib API " +
                    "for binary compatibility with Kotlin stdlib . " +
                    "See KT-62510 for details."
        }
    }
}

tasks.register("checkIdeDependencies") {
    dependsOn("checkIdeDependenciesConfiguration")
    dependsOn("run")
}

val validatorProject: Project get() = project

private class ExperimentalAnnotationsCollector() {
    val experimentalAnnotations: Set<String> by lazy {
        validatorProject.projectDir.toPath().resolve(EXPERIMENTAL_ANNOTATIONS_FILE)
            .readLines()
            .map { it.trim() }
            .filterNot { it.startsWith("#") || it.isBlank() }
            .toSet()
    }

    fun getUsedExperimentalAnnotations(arguments: List<String>): List<String> {
        return buildList {
            addAll(getOptInAnnotationsByMultipleArguments(arguments))
            arguments.flatMapTo(this) { getOptInAnnotationsBySingleArgument(it) }
            removeAll { it !in experimentalAnnotations }
        }
    }

    /**
     * Returns a list of experimental annotation used in an argument list of kind `["-opt-in", "kotlin.ExperimentalStdlibApi,kotlin.time.ExperimentalTime"]`
     */
    private fun getOptInAnnotationsByMultipleArguments(arguments: List<String>): List<String> {
        return arguments.windowed(2).flatMap { (argumentName, value) ->
            if (argumentName == "-Xopt-in" || argumentName == "-opt-in") value.split(",").map { it.trim() }
            else emptyList()
        }
    }

    private fun getOptInAnnotationsBySingleArgument(argument: String): List<String> {
        @Suppress("NAME_SHADOWING")
        var argument = argument.trim()
        argument = when {
            argument.startsWith("-opt-in=") -> {
                argument.removePrefix("-opt-in=")
            }
            argument.startsWith("-Xopt-in=") -> {
                argument.removePrefix("-Xopt-in=")
            }
            else -> {
                return emptyList()
            }
        }
        return argument.split(",").map { it.trim() }
    }


    companion object {
        private const val EXPERIMENTAL_ANNOTATIONS_FILE = "ExperimentalAnnotations.txt"
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
