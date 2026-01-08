package org.jetbrains.kotlin

import kotlinBuildProperties
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.nativeDistribution.NativeDistribution
import org.jetbrains.kotlin.nativeDistribution.asNativeDistribution
import org.jetbrains.kotlin.nativeDistribution.nativeDistribution
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import javax.inject.Inject

/**
 * Compares SignatureIds of the current distribution and the given older one.
 * Can be used to validate that there are no unexpected breaking ABI changes.
 */
@DisableCachingByDefault(because = "Task with no outputs")
open class CompareDistributionSignatures @Inject constructor(
        objectFactory: ObjectFactory,
        private val execOperations: ExecOperations,
) : DefaultTask() {

    companion object {
        @JvmStatic
        fun registerForPlatform(project: Project, target: String) {
            register(project, "${target}CheckPlatformAbiCompatibility") {
                libraries = Libraries.Platform(target)
                dependsOn(":kotlin-native:${target}PlatformLibs") // The task configures inputs on the insides of the distribution
            }
        }

        @JvmStatic
        fun registerForStdlib(project: Project) {
            register(project, "checkStdlibAbiCompatibility") {
                libraries = Libraries.Standard
                dependsOn(":kotlin-native:distRuntime") // The task configures inputs on the insides of the distribution
            }
        }

        private fun register(project: Project, name: String, configure: CompareDistributionSignatures.() -> Unit) {
            project.tasks.register(name, CompareDistributionSignatures::class.java) {
                val property = project.kotlinBuildProperties.stringProperty("anotherDistro").orNull
                oldDistributionRoot.set(project.layout.dir(project.provider {
                    // `property` can only be checked for existence during task execution: during IDE import all tasks are
                    // created eagerly, so checking it during configuration stage will cause errors.
                    project.file(property ?: error("'anotherDistro' property must be set in order to execute '$name' task"))
                }))
                newDistributionRoot.set(project.nativeDistribution.map { it.root })
                configure(this)
            }
        }
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    protected val oldDistributionRoot: DirectoryProperty = objectFactory.directoryProperty()

    @Internal
    private val oldDistribution: Provider<NativeDistribution> = oldDistributionRoot.asNativeDistribution()

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    protected val newDistributionRoot: DirectoryProperty = objectFactory.directoryProperty()

    @Internal
    private val newDistribution: Provider<NativeDistribution> = newDistributionRoot.asNativeDistribution()

    enum class OnMismatchMode {
        FAIL,
        NOTIFY
    }

    @Input
    var onMismatchMode: OnMismatchMode = OnMismatchMode.FAIL

    sealed class Libraries {
        object Standard : Libraries()

        class Platform(val target: String) : Libraries()
    }

    @Input
    lateinit var libraries: Libraries

    private fun computeDiff(): KlibDiff = when (val libraries = libraries) {
        Libraries.Standard -> KlibDiff(
                emptyList(),
                emptyList(),
                listOf(RemainingLibrary(newDistribution.get().stdlib.asFile, oldDistribution.get().stdlib.asFile))
        )

        is Libraries.Platform -> {
            val oldPlatformLibs = oldDistribution.get().platformLibs(libraries.target).asFile
            val oldPlatformLibsNames = oldPlatformLibs.list().toSet()
            val newPlatformLibs = newDistribution.get().platformLibs(libraries.target).asFile
            val newPlatformLibsNames = newPlatformLibs.list().toSet()
            KlibDiff(
                    (newPlatformLibsNames - oldPlatformLibsNames).map(newPlatformLibs::resolve),
                    (oldPlatformLibsNames - newPlatformLibsNames).map(oldPlatformLibs::resolve),
                    oldPlatformLibsNames.intersect(newPlatformLibsNames).map {
                        RemainingLibrary(newPlatformLibs.resolve(it), oldPlatformLibs.resolve(it))
                    }
            )
        }
    }

    @TaskAction
    fun run() {
        check(looksLikeKotlinNativeDistribution(oldDistribution.get(), libraries)) {
            """
            `${oldDistribution.get().root.asFile}` doesn't look like Kotlin/Native distribution. 
            Make sure to provide an absolute path to it.
            """.trimIndent()
        }
        check(looksLikeKotlinNativeDistribution(newDistribution.get(), libraries)) {
            """
                `${newDistribution.get().root.asFile}` doesn't look like Kotlin/Native distribution.
                Check that $name has all required task dependencies.
            """.trimIndent()
        }
        val platformLibsDiff = computeDiff()
        report("libraries diff")
        val librariesMismatch = platformLibsDiff.missingLibs.isNotEmpty() || platformLibsDiff.newLibs.isNotEmpty()
        platformLibsDiff.missingLibs.forEach { report("-: $it") }
        platformLibsDiff.newLibs.forEach { report("+: $it") }
        val signaturesMismatch = cumulativeSignaturesComparison(platformLibsDiff)
        if ((librariesMismatch || signaturesMismatch) && onMismatchMode == OnMismatchMode.FAIL) {
            error("Mismatch found, see stdout for details.")
        }
    }

    private data class Mark(var presentInOld: Boolean = false, var presentInNew: Boolean = false) {
        val newOnly: Boolean
            get() = presentInNew && !presentInOld

        val oldOnly: Boolean
            get() = presentInOld && !presentInNew
    }

    private fun cumulativeSignaturesComparison(klibDiff: KlibDiff): Boolean {
        report("signatures diff")
        // Boolean value signifies if value is present in new platform libraries.
        val signaturesMap = mutableMapOf<String, Mark>()
        val oldLibs = klibDiff.missingLibs + klibDiff.remainingLibs.map { it.old }
        oldLibs.flatMap { getKlibSignatures(it) }.forEach { sig ->
            signaturesMap.getOrPut(sig, ::Mark).presentInOld = true
        }
        val duplicates = mutableListOf<String>()
        val newLibs = klibDiff.newLibs + klibDiff.remainingLibs.map { it.new }
        newLibs.flatMap { getKlibSignatures(it) }.forEach { sig ->
            val mark = signaturesMap.getOrPut(sig, ::Mark)
            if (mark.presentInNew) {
                duplicates += sig
            } else {
                mark.presentInNew = true
            }
        }
        duplicates.forEach { report("dup: $it") }
        val oldSigs = signaturesMap.filterValues { it.oldOnly }.keys
                .sorted()
                .onEach { report("-: $it") }
        val newSigs = signaturesMap.filterValues { it.newOnly }.keys
                .sorted()
                .onEach { report("+: $it") }
        return oldSigs.isNotEmpty() || newSigs.isNotEmpty()
    }

    private fun report(message: String) {
        println(message)
    }

    private data class RemainingLibrary(val new: File, val old: File)

    private class KlibDiff(
            val newLibs: Collection<File>,
            val missingLibs: Collection<File>,
            val remainingLibs: Collection<RemainingLibrary>
    )

    private fun getKlibSignatures(klib: File): List<String> {
        val args = listOf("dump-metadata-signatures", klib.absolutePath, "-signature-version", "1")
        ByteArrayOutputStream().use { stdout ->
            execOperations.exec {
                commandLine(newDistribution.get().klib.asFile, *args.toTypedArray())
                this.standardOutput = stdout
            }.assertNormalExitValue()
            return stdout.toString().lines().filter { it.isNotBlank() }
        }
    }

    private fun looksLikeKotlinNativeDistribution(distribution: NativeDistribution, libraries: Libraries): Boolean {
        val distributionComponents = buildSet {
            add(distribution.bin.asFile)
            add(distribution.konanProperties.asFile)
            when (libraries) {
                Libraries.Standard -> add(distribution.stdlib.asFile)
                is Libraries.Platform -> add(distribution.platformLibs(libraries.target).asFile)
            }
        }
        return distributionComponents.all { Files.exists(it.toPath(), LinkOption.NOFOLLOW_LINKS) }
    }
}