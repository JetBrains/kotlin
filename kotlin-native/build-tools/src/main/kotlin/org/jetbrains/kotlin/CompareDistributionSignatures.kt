package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Compares SignatureIds of the current distribution and the given older one.
 * Can be used to validate that there are no unexpected breaking ABI changes.
 */
open class CompareDistributionSignatures : DefaultTask() {

    @Input
    lateinit var oldDistribution: String

    private val newDistribution: String =
            project.kotlinNativeDist.absolutePath

    enum class OnMismatchMode {
        FAIL,
        NOTIFY
    }

    private val messageBuilder = StringBuilder()

    @Input
    var onMismatchMode: OnMismatchMode = OnMismatchMode.NOTIFY

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
                listOf(RemainingLibrary(newDistribution.stdlib(), oldDistribution.stdlib()))
        )
        is Libraries.Platform -> {
            val oldPlatformLibs = oldDistribution.platformLibs(libraries.target)
            val oldPlatformLibsNames = oldPlatformLibs.list().toSet()
            val newPlatformLibs = newDistribution.platformLibs(libraries.target)
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
        check(looksLikeKotlinNativeDistribution(Paths.get(oldDistribution))) {
            """
            `$oldDistribution` doesn't look like Kotlin/Native distribution. 
            Make sure to provide an absolute path to it.
            """.trimIndent()
        }
        val platformLibsDiff = computeDiff()
        if (platformLibsDiff.missingLibs.isNotEmpty()) {
            messageBuilder.apply {
                appendln("Following platform libraries are missing in the new distro:")
                platformLibsDiff.missingLibs.forEach { appendln(it) }
            }
        }
        if (platformLibsDiff.newLibs.isNotEmpty()) {
            messageBuilder.apply {
                appendln("Following platform libraries were added:")
                platformLibsDiff.newLibs.forEach { appendln(it) }
            }
        }
        for ((new, old) in platformLibsDiff.remainingLibs) {
            val result = compareSignatures(new, old)
            if (result.run { newKlibOnly.isNotEmpty() || oldKlibOnly.isNotEmpty() }) {
                reportMismatch(result, new.name)
            }
        }
        if (messageBuilder.isNotEmpty()) {
            report(messageBuilder.toString())
        }
    }

    private fun reportMismatch(compareDiff: CompareDiff, klibName: String) {
        messageBuilder.apply {
            appendln("Mismatch for $klibName:")
            if (compareDiff.oldKlibOnly.isNotEmpty()) {
                appendln("Following signatures are missing in the new version:")
                compareDiff.oldKlibOnly.forEach { appendln(it) }
            }
            if (compareDiff.newKlibOnly.isNotEmpty()) {
                appendln("Following signatures were added:")
                compareDiff.newKlibOnly.forEach { appendln(it) }
            }
        }
    }

    private fun report(message: String) {
        when (onMismatchMode) {
            OnMismatchMode.FAIL -> error(message)
            OnMismatchMode.NOTIFY -> println(message)
        }
    }

    private data class RemainingLibrary(val new: File, val old: File)

    private class KlibDiff(
        val newLibs: Collection<File>,
        val missingLibs: Collection<File>,
        val remainingLibs: Collection<RemainingLibrary>
    )

    private fun String.stdlib(): File =
            File("$this/klib/common/stdlib").also {
                check(it.exists()) {
                    """
                    `${it.absolutePath}` doesn't exists.
                    If $oldDistribution has a different directory layout then it is time to update this comparator.
                    """.trimIndent()
                }
            }

    private fun String.platformLibs(target: String): File =
            File("$this/klib/platform/$target").also {
                check(it.exists()) {
                    """
                    `${it.absolutePath}` doesn't exists.
                    Make sure that given distribution actually supports $target.
                    """.trimIndent()
                }
            }


    private fun getKlibSignatures(klib: File): List<String> {
        val tool = if (HostManager.hostIsMingw) "klib.bat" else "klib"
        val klibTool = File("$newDistribution/bin/$tool").absolutePath
        val args = listOf("signatures", klib.absolutePath)
        return runProcess(localExecutor(project), klibTool, args).stdOut.lines()
    }

    private class CompareDiff(val newKlibOnly: Set<String>, val oldKlibOnly: Set<String>)

    private fun compareSignatures(new: File, old: File): CompareDiff {
        val newKlibSignatures = getKlibSignatures(new).toSet()
        val oldKlibSignatures = getKlibSignatures(old).toSet()
        return CompareDiff(
                newKlibSignatures - oldKlibSignatures,
                oldKlibSignatures - newKlibSignatures,
        )
    }

    private fun looksLikeKotlinNativeDistribution(directory: Path): Boolean {
        val distributionComponents = directory.run {
            val konanDir = resolve("konan")
            setOf(resolve("bin"), resolve("klib"), konanDir, konanDir.resolve("konan.properties"))
        }
        return distributionComponents.all { Files.exists(it, LinkOption.NOFOLLOW_LINKS) }
    }
}