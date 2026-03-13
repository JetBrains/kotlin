import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.get
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import java.io.File
import java.net.URI
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.io.normalize


fun Project.getGithubRevision(): String {
    val isTeamcityBuild = project.hasProperty("teamcity.version") || System.getenv("TEAMCITY_VERSION") != null
    return if (isTeamcityBuild) (project.property("githubRevision") as? String) ?: "" else "master"
}

fun Project.getKotlinRoot(): String {
    val kotlin_root = rootProject.file("../../../").absoluteFile.invariantSeparatorsPath
    return kotlin_root
}


/**
 * The Dokka K2 does not support intersecting source or sample roots
 * https://github.com/Kotlin/dokka/issues/3701
 *
 * As a workaround, the intersecting roots may be copied and source links should be fixed
 *
 * This function detects such source and sample roots, copy them and fix source links.
 * It should be called after all other configurations
 */
fun Project.fixIntersectedSourceRootsAndSamples(
    dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec>,
    libraryName: String
) {
    val kotlin_root = getKotlinRoot()
    val githubRevision = getGithubRevision()

    val kotlin_library_dir = file("$kotlin_root/libraries/$libraryName")

    fun intersectOfNormalizedPaths(normalizedPaths: Set<File>, normalizedPaths2: Set<File>): Set<File> {
        val result = mutableSetOf<File>()
        for (p1 in normalizedPaths) {
            for (p2 in normalizedPaths2) {
                if (p1.startsWith(p2) || p2.startsWith(p1)) {
                    result.add(p1)
                    result.add(p2)
                }
            }
        }
        return result
    }

    fun Set<File>.normalize() = mapTo(mutableSetOf()) { it.normalize() }
    fun intersect(paths: Set<File>, paths2: Set<File>): Set<File> =
        intersectOfNormalizedPaths(paths.normalize(), paths2.normalize())

    val sourceSets = dokkaSourceSets.toList()
    val temporaryDirectory = buildDir.resolve("temporary_dokka_source_sets/$libraryName/").toPath()

    val replacementsSources = mutableMapOf<String, MutableMap<File, File>>()
    val replacementsSamples = mutableMapOf<String, MutableMap<File, File>>()

    for (i in sourceSets.indices) {
        for (j in i + 1 until sourceSets.size) {
            intersect(
                sourceSets[i].sourceRoots.toSet(),
                sourceSets[j].sourceRoots.toSet()
            ).forEach {
                val relativePath = kotlin_library_dir.toPath().relativize(it.toPath())
                replacementsSources.getOrPut(sourceSets[i].name, ::mutableMapOf)[it] =
                    temporaryDirectory.resolve(sourceSets[i].name).resolve(relativePath).toFile()
                replacementsSources.getOrPut(sourceSets[j].name, ::mutableMapOf)[it] =
                    temporaryDirectory.resolve(sourceSets[j].name).resolve(relativePath).toFile()
            }

            intersect(
                sourceSets[i].samples.toSet(),
                sourceSets[j].samples.toSet()
            ).forEach {
                val relativePath = kotlin_library_dir.toPath().relativize(it.toPath())
                replacementsSamples.getOrPut(sourceSets[i].name, ::mutableMapOf)[it] =
                    temporaryDirectory.resolve(sourceSets[i].name).resolve(relativePath).toFile()
                replacementsSamples.getOrPut(sourceSets[j].name, ::mutableMapOf)[it] =
                    temporaryDirectory.resolve(sourceSets[j].name).resolve(relativePath).toFile()
            }
        }
    }
    replacementsSamples.forEach { (sourceSetName, replacements) ->
        val sourceSet = dokkaSourceSets[sourceSetName]

        // replace samples here
        sourceSet.samples.setFrom(sourceSet.samples.map { replacements[it] ?: it })
    }

    val kotlin_library_url = "https://github.com/JetBrains/kotlin/tree/$githubRevision/libraries/$libraryName"
    replacementsSources.forEach { (sourceSetName, replacements) ->
        val sourceSet = dokkaSourceSets[sourceSetName]
        // replace sourceRoots here
        sourceSet.sourceRoots.setFrom(sourceSet.sourceRoots.map { replacements[it] ?: it })

        replacements.forEach { (original, replacement) ->
            // setup source-links
            sourceSet.sourceLink {
                remoteUrl.set(URI("$kotlin_library_url/${kotlin_library_dir.toPath().relativize(original.toPath())}"))
                localDirectory.set(replacement)
                remoteLineSuffix.set("#L")
            }
        }

        // The order of source links is important
        // source links to temporary directories should have higher priority
        //sourceSet.sourceLinks.set(sourceSet.sourceLinks.get().reversed())

        // work with files
        (getTasksByName("dokkaGenerateHtml", false) + getTasksByName("dokkaGenerate", false)).forEach {
            it.doFirst {
                temporaryDirectory.toFile().deleteRecursively()
                replacementsSamples.forEach { (_, replacements) ->
                    replacements.forEach { (original, replacement) ->
                        // copy files
                        original.copyRecursively(replacement, overwrite = true)
                    }
                }
                replacementsSources.forEach { (_, replacements) ->
                    replacements.forEach { (original, replacement) ->
                        // copy files
                        original.copyRecursively(replacement, overwrite = true)
                    }
                }
            }
        }
    }
}

@OptIn(InternalDokkaGradlePluginApi::class)
abstract class VersionFilterPluginParameters @Inject constructor(
    name: String
) : DokkaPluginParametersBaseSpec(
    name,
    "org.jetbrains.dokka.kotlinlang.VersionFilterPlugin",
) {

    @get:Input
    abstract val targetVersion: Property<String>

    override fun jsonEncode(): String {
        return """
            {
              "targetVersion": "${targetVersion.get()}"
            }
            """.trimIndent()
    }
}

fun Project.sourceLinksFromRoot(sourceSet: DokkaSourceSetSpec) {
    val githubRevision = getGithubRevision()
    sourceSet.sourceLink {
        localDirectory.set(file(getKotlinRoot()))
        remoteUrl.set(URI("https://github.com/JetBrains/kotlin/tree/$githubRevision"))
        remoteLineSuffix.set("#L")
    }
}