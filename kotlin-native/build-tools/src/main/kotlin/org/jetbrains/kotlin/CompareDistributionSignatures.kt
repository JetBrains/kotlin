package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

/**
 * Compares SignatureIds of the current distribution and the given older one.
 * Can be used to validate that there are no unexpected breaking ABI changes.
 */
open class CompareDistributionSignatures : DefaultTask() {

    @Input
    lateinit var target: String

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

    @TaskAction
    fun run() {
        val klibs = getKlibs(oldDistribution, newDistribution)
        if (klibs.missingLibs.isNotEmpty()) {
            messageBuilder.apply {
                appendln("Following libraries are missing in the new distro:")
                klibs.missingLibs.forEach { appendln(it) }
            }
        }
        if (klibs.newLibs.isNotEmpty()) {
            messageBuilder.apply {
                appendln("Following libraries were added:")
                klibs.newLibs.forEach { appendln(it) }
            }
        }
        for ((new, old) in klibs.remainingLibs) {
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
            File("$this/klib/common/stdlib")

    private fun String.platformLibs(target: String): File =
            File("$this/klib/platform/$target")

    private fun getKlibs(oldDistribution: String, newDistribution: String): KlibDiff {
        val oldPlatformLibs = oldDistribution.platformLibs(target)
        val oldPlatformLibsNames = oldPlatformLibs.list().toSet()
        val newPlatformLibs = newDistribution.platformLibs(target)
        val newPlatformLibsNames = newPlatformLibs.list().toSet()
        return KlibDiff(
                (newPlatformLibsNames - oldPlatformLibsNames).map(newPlatformLibs::resolve),
                (oldPlatformLibsNames - newPlatformLibsNames).map(oldPlatformLibs::resolve),
                oldPlatformLibsNames.intersect(newPlatformLibsNames).map {
                    RemainingLibrary(newPlatformLibs.resolve(it), oldPlatformLibs.resolve(it))
                } + RemainingLibrary(newDistribution.stdlib(), oldDistribution.stdlib())
        )
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
}