import gradle.GradlePluginVariant
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("gradle-plugin-dependency-configuration")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("gradle-plugin-api-reference")
}

pluginApiReference {
    enableForAllGradlePluginVariants()

    failOnWarning = true

    additionalDokkaConfiguration {
        dokkaSourceSets.configureEach {
            if (name != "common") {
                suppress = true
                return@configureEach
            }

            reportUndocumented = true
            includes.from("api-reference-description.md")
        }
    }

    embeddedProject(project.dependencies.project(":kotlin-gradle-compiler-types"))
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-annotations"))
    commonApi(project(":native:kotlin-native-utils")) { // TODO: consider removing in KT-70247
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-util-klib")
    }
    commonApi(project(":kotlin-tooling-core"))
    commonApi(project(":compiler:build-tools:kotlin-build-tools-api"))

    commonCompileOnly(project(":kotlin-gradle-compiler-types"))

    embedded(project(":kotlin-gradle-compiler-types")) { isTransitive = false }
}

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}

registerKotlinSourceForVersionRange(
    GradlePluginVariant.GRADLE_MIN,
    GradlePluginVariant.GRADLE_88,
)

generatedSourcesTask(
    taskName = "generateKotlinVersionConstant",
    generatorProject = ":gradle:generators:native-cache-kotlin-version",
    generatorRoot = "libraries/tools/gradle/generators/native-cache-kotlin-version/src",
    generatorMainClass = "org.jetbrains.kotlin.gradle.generators.native.cache.version.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            version.toString(),
            layout.projectDirectory.file("native-cache-kotlin-versions.txt").toString(),
        )
    }
)

// 1. Trigger apiDump after generation
tasks.named("generateKotlinVersionConstant").configure {
    finalizedBy("apiDump")
}

// 2. Resolve implicit dependency conflict
// apiDump writes the file, apiCheck reads it. If both run, Dump must run first.
tasks.named("apiCheck").configure {
    mustRunAfter("apiDump")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
            useVersion(libs.versions.commons.lang.get())
            because("CVE-2025-48924")
        }
    }
}

// Everything below is copied from "generated-sources" plugin, as applying it in this particular module creates a dependency cycle and
// breaks the build. This should be deleted when a solution for the cyclical dependency problem is solved.
// See KT-79146

/**
 * This utility function creates [taskName] task, which invokes specified code generator which produces some new
 *   sources for the current module in the directory ./gen
 *
 * @param [taskName] name for the created task
 * @param [generatorProject] module of the code generator
 * @param [generatorRoot] path to the `src` directory of the code generator
 * @param [generatorMainClass] FQN of the generator main class
 * @param [argsProvider] used for specifying the CLI arguments to the generator.
 *   By default, it passes the pass to the generated sources (`./gen`)
 * @param [dependOnTaskOutput] set to false disable the gradle dependency between the generation task and the compilation of the current
 *   module. This is needed for cases when the module with generator depends on the module for which it generates new sources.
 *   Use it with caution
 */
fun Project.generatedSourcesTask(
    taskName: String,
    generatorProject: String,
    generatorRoot: String,
    generatorMainClass: String,
    argsProvider: JavaExec.(generationRoot: Directory) -> List<String> = { listOf(it.toString()) },
    dependOnTaskOutput: Boolean = true,
): TaskProvider<JavaExec> {
    val generatorClasspath: Configuration by configurations.creating

    dependencies {
        generatorClasspath(project(generatorProject))
    }

    return generatedSourcesTask(
        taskName,
        generatorClasspath,
        generatorRoot,
        generatorMainClass,
        argsProvider,
        dependOnTaskOutput = dependOnTaskOutput,
    )
}

/**
 * The utility can be used for sources generation by third-party tools.
 * For instance, it's used for Kotlin and KDoc lexer generations by JFlex.
 */
fun Project.generatedSourcesTask(
    taskName: String,
    generatorClasspath: Configuration,
    generatorRoot: String,
    generatorMainClass: String,
    argsProvider: JavaExec.(generationRoot: Directory) -> List<String> = { listOf(it.toString()) },
    dependOnTaskOutput: Boolean = true,
    commonSourceSet: Boolean = false,
): TaskProvider<JavaExec> {
    val genPath = if (commonSourceSet) {
        "common/src/gen"
    } else {
        "gen"
    }
    val generationRoot = layout.projectDirectory.dir(genPath)
    val task = tasks.register<JavaExec>(taskName) {
        workingDir = rootDir
        classpath = generatorClasspath
        mainClass.set(generatorMainClass)
        systemProperties["line.separator"] = "\n"
        args(argsProvider(generationRoot))

        @Suppress("NAME_SHADOWING")
        val generatorRoot = "$rootDir/$generatorRoot"
        val generatorConfigurationFiles = fileTree(generatorRoot) {
            include("**/*.kt")
        }

        inputs.files(generatorConfigurationFiles)
        outputs.dir(generationRoot)
    }

    sourceSets.named("common") {
        val dependency: Any = when (dependOnTaskOutput) {
            true -> task
            false -> generationRoot
        }
        java.srcDirs(dependency)
    }

    apply(plugin = "idea")
    (this as ExtensionAware).extensions.configure<IdeaModel>("idea") {
        this.module.generatedSourceDirs.add(generationRoot.asFile)
    }
    return task
}

private val Project.sourceSets: SourceSetContainer
    get() = extensions.getByType<JavaPluginExtension>().sourceSets

tasks.withType<Jar>().configureEach {
    if (name.endsWith("Jar") || name == "jar") {
        // FIXME: Entry org/jetbrains/kotlin/gradle/dsl/KotlinDependencies.class is a duplicate
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
