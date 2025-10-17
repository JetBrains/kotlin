import gradle.GradlePluginVariant
import gradle.commonSourceSetName
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.test.assertEquals

class GradlePluginTests {

    @TempDir
    lateinit var workingDir: File

    /**
     * This test checks that per Gradle variant source sets in "gradle-plugin-common-configuration" and "gradle-plugin-dependency-configuration"
     * properly resolve their variant-specific classpath
     */
    @Test
    fun `gradle variant source sets - resolve per variant classpath`() {
        val root = createFakeKotlinRoot()

        val producerPluginDependency = createKotlinSubproject("producerPluginDependency", root)
        producerPluginDependency.version = "1.0"
        producerPluginDependency.beforeEvaluate {
            plugins.apply(GRADLE_PLUGIN_DEPENDENCY_CONFIGURATION_CONVENTION_PLUGIN)
        }

        val producerPlugin = createKotlinSubproject("producerPlugin", root)
        producerPlugin.version = "1.0"
        producerPlugin.beforeEvaluate {
            plugins.apply(GRADLE_PLUGIN_COMMON_CONFIGURATION_CONVENTION_PLUGIN)
        }

        val consumerPlugin = createKotlinSubproject("consumerPlugin", root)
        consumerPlugin.beforeEvaluate {
            plugins.apply(GRADLE_PLUGIN_COMMON_CONFIGURATION_CONVENTION_PLUGIN)
            dependencies.add("commonImplementation", dependencies.project(":producerPlugin"))
            dependencies.add("commonImplementation", dependencies.project(":producerPluginDependency"))
        }
        consumerPlugin.repositories.mavenCentral()

        producerPluginDependency.evaluate()
        producerPlugin.evaluate()
        consumerPlugin.evaluate()

        val dependencyPathsToCheck: List<Path> = listOf(
            producerPluginDependency.projectDir.toPath(),
            producerPlugin.projectDir.toPath(),
        )

        assertEquals(
            listOf(
                listOf("producerPlugin", "build", "libs", "producerPlugin-1.0.jar"),
                listOf("producerPluginDependency", "build", "libs", "producerPluginDependency-1.0.jar"),
            ),
            consumerPlugin.actualCompilationClasspath(
                variantSourceSetName = GradlePluginVariant.GRADLE_MIN.sourceSetName,
                dependencyPathsToCheck = dependencyPathsToCheck,
            ),
        )

        assertEquals(
            listOf(
                listOf("producerPlugin", "build", "classes", "java", "common"),
                listOf("producerPlugin", "build", "classes", "kotlin", "common"),
                listOf("producerPlugin", "build", "classes", "java", GradlePluginVariant.MIDDLE_GRADLE_VARIANT_FOR_TESTS.sourceSetName),
                listOf("producerPlugin", "build", "classes", "kotlin", GradlePluginVariant.MIDDLE_GRADLE_VARIANT_FOR_TESTS.sourceSetName),
                listOf("producerPluginDependency", "build", "classes", "java", "common"),
                listOf("producerPluginDependency", "build", "classes", "kotlin", "common"),
                listOf("producerPluginDependency", "build", "classes", "java", GradlePluginVariant.MIDDLE_GRADLE_VARIANT_FOR_TESTS.sourceSetName),
                listOf("producerPluginDependency", "build", "classes", "kotlin", GradlePluginVariant.MIDDLE_GRADLE_VARIANT_FOR_TESTS.sourceSetName),
            ),
            consumerPlugin.actualCompilationClasspath(
                variantSourceSetName = GradlePluginVariant.MIDDLE_GRADLE_VARIANT_FOR_TESTS.sourceSetName,
                dependencyPathsToCheck = dependencyPathsToCheck,
            ),
        )

        val actualCompilationClasspath = consumerPlugin.actualCompilationClasspath(
            variantSourceSetName = commonSourceSetName,
            dependencyPathsToCheck = dependencyPathsToCheck,
        )

        listOf(
            listOf("producerPlugin", "build", "classes", "java", "common"),
            listOf("producerPlugin", "build", "classes", "kotlin", "common"),
            listOf("producerPluginDependency", "build", "classes", "java", "common"),
            listOf("producerPluginDependency", "build", "classes", "kotlin", "common"),
        ).forEachIndexed { index, expected ->
            assertEquals(
                expected,
                actualCompilationClasspath[index],
                "Expected $expected is not equal to actual ${actualCompilationClasspath[index]}.\n" +
                        "All actual are: ${actualCompilationClasspath.joinToString(separator = "\n")}"
            )
        }
    }

    @Test
    fun `gradle variant source sets - dokka generation doesn't see main source set in variant source sets`() {
        val root = createFakeKotlinRoot()
        val project = createKotlinSubproject("project", root)
        root.extraProperties.set("kotlin.build.gradle.publish.javadocs", "true")
        project.beforeEvaluate {
            plugins.apply(GRADLE_PLUGIN_DEPENDENCY_CONFIGURATION_CONVENTION_PLUGIN)
            plugins.apply(GRADLE_PLUGIN_API_REFERENCE)
        }
        project.afterEvaluate {
            extensions.getByType<PluginApiReferenceExtension>().enableForAllGradlePluginVariants()
        }
        project.evaluate()

        val mainDokkaTask = project.tasks.getByName("dokkaHtml") as AbstractDokkaLeafTask
        val variantSourceSetDokkaTask = project.tasks.getByName(
            "dokka${GradlePluginVariant.MIDDLE_GRADLE_VARIANT_FOR_TESTS.sourceSetName.replaceFirstChar { it.uppercaseChar() }}Html"
        ) as AbstractDokkaLeafTask

        val targetSourceSets = listOf(
            "main",
            "common",
            GradlePluginVariant.MIDDLE_GRADLE_VARIANT_FOR_TESTS.sourceSetName
        )

        assertEquals(
            listOf(
                "main" to false,
                "common" to false,
                GradlePluginVariant.MIDDLE_GRADLE_VARIANT_FOR_TESTS.sourceSetName to true,
            ),
            targetSourceSets.map { mainDokkaTask.dokkaSourceSets.getByName(it).state() }
        )
        assertEquals(
            listOf(
                "main" to true,
                "common" to false,
                GradlePluginVariant.MIDDLE_GRADLE_VARIANT_FOR_TESTS.sourceSetName to false,
            ),
            targetSourceSets.map { variantSourceSetDokkaTask.dokkaSourceSets.getByName(it).state() },
        )
    }

    private fun GradleDokkaSourceSetBuilder.state() = name to suppress.get()

    private fun Project.actualCompilationClasspath(
        variantSourceSetName: String,
        dependencyPathsToCheck: List<Path>,
    ) = sourceSets.getByName(variantSourceSetName).compileClasspath.files
        .map { it.toPath() }
        .mapNotNull { compilationPath ->
            val subprojectPath = dependencyPathsToCheck.firstOrNull { compilationPath.startsWith(it) }
            if (subprojectPath == null) {
                return@mapNotNull null
            }
            compilationPath.drop(subprojectPath.count() - 1).map { it.pathString }
        }

    private fun createKotlinSubproject(named: String, root: Project): ProjectInternal = ProjectBuilder.builder()
        .also {
            it.withName(named)
            it.withParent(root)
            it.withProjectDir(workingDir.resolve(named).also { it.mkdir() })
        }.build().also {
            it.tasks.register("mvnInstall")
            it.extraProperties.set("avoidSettingCompilerVersionForBTA", true)
        } as ProjectInternal

    private fun createFakeKotlinRoot(): Project {
        val root = ProjectBuilder.builder().also {
            it.withProjectDir(workingDir)
        }.build()
        root.extraProperties.set("buildNumber", "1.0")
        root.tasks.register("mvnInstall")

        createKotlinSubproject("kotlin-gradle-plugin-api", root).also {
            it.configurations.consumable("fakeRuntime") {
                attributes.attribute(Usage.USAGE_ATTRIBUTE, it.objects.named(Usage.JAVA_RUNTIME))
            }
        }
        createKotlinSubproject("kotlin-compiler-embeddable", root)

        return root
    }

    companion object {
        private const val GRADLE_PLUGIN_DEPENDENCY_CONFIGURATION_CONVENTION_PLUGIN = "gradle-plugin-dependency-configuration"
        private const val GRADLE_PLUGIN_COMMON_CONFIGURATION_CONVENTION_PLUGIN = "gradle-plugin-common-configuration"
        private const val GRADLE_PLUGIN_API_REFERENCE = "gradle-plugin-api-reference"
    }
}
