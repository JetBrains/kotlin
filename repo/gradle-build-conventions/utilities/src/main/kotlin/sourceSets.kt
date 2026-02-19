import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.ideaExt.idea

inline fun Project.sourceSets(crossinline body: SourceSetsBuilder.() -> Unit) = SourceSetsBuilder(this).body()

class SourceSetsBuilder(val project: Project) {

    inline operator fun String.invoke(crossinline body: SourceSet.() -> Unit): SourceSet {
        val sourceSetName = this
        return project.sourceSets.maybeCreate(sourceSetName).apply {
            body()
        }
    }
}

fun SourceSet.none() {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

val SourceSet.projectDefault: Project.() -> Unit
    get() = {
        when (val name = this@projectDefault.name) {
            "main" -> {
                java.srcDirs("src")
                this@projectDefault.resources.srcDir("resources")
            }
            "test" -> {
                java.srcDirs("test", "tests")
                this@projectDefault.resources.srcDir("testResources")
            }
            "testFixtures" -> {
                java.srcDirs("testFixtures")
                this@projectDefault.resources.srcDir("testFixturesResources")
            }
            else -> error("Unknown source set $name")
        }
    }

val Project.sourceSets: SourceSetContainer
    get() = javaPluginExtension().sourceSets

val Project.mainSourceSet: SourceSet
    get() = javaPluginExtension().mainSourceSet

val Project.testFixturesSourceSet: SourceSet
    get() = javaPluginExtension().testFixturesSourceSet

val Project.testSourceSet: SourceSet
    get() = javaPluginExtension().testSourceSet

val JavaPluginExtension.mainSourceSet: SourceSet
    get() = sourceSets.getByName("main")

val JavaPluginExtension.testFixturesSourceSet: SourceSet
    get() = sourceSets.getByName("testFixtures")

val JavaPluginExtension.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")

fun Project.mainJavaPluginSourceSet() = findJavaPluginExtension()?.sourceSets?.findByName("main")
fun Project.mainKotlinSourceSet() =
    (extensions.findByName("kotlin") as? KotlinSourceSetContainer)?.sourceSets?.findByName("main")
fun Project.sources() = mainJavaPluginSourceSet()?.allSource ?: mainKotlinSourceSet()?.kotlin

fun SourceSet.generatedDir(project: Project, generationRoot: Provider<Directory>) {
    java.srcDir(generationRoot)

    // This is needed until IDEA fixes IDEA-339729
    project.apply(plugin = "idea")
    project.idea {
        this.module.generatedSourceDirs.add(generationRoot.get().asFile)
    }
}
