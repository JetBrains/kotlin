package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

@DisableCachingByDefault
abstract class NodeJsSetupTask @Inject constructor(
    settings: BaseNodeJsEnvSpec
) : AbstractSetupTask<NodeJsEnv, BaseNodeJsEnvSpec>(settings) {

    @get:Internal
    override val artifactPattern: String
        get() = "v[revision]/[artifact](-v[revision]-[classifier]).[ext]"

    @get:Internal
    override val artifactModule: String
        get() = "org.nodejs"

    @get:Internal
    override val artifactName: String
        get() = "node"

    private val isWindows = env.map { it.isWindows }

    private val executable = env.map { it.executable }

    override fun extract(archive: File) {
        var fixBrokenSymLinks = false

        val destination = destinationProvider.getFile()
        fs.copy {
            it.from(
                when {
                    archive.name.endsWith("zip") -> archiveOperations.zipTree(archive)
                    else -> {
                        fixBrokenSymLinks = true
                        archiveOperations.tarTree(archive)
                    }
                }
            )
            it.into(destination.parentFile)
        }

        fixBrokenSymlinks(destination, isWindows.get(), fixBrokenSymLinks)

        if (!isWindows.get()) {
            File(executable.get()).setExecutable(true)
        }
    }

    private fun fixBrokenSymlinks(destinationDir: File, isWindows: Boolean, necessaryToFix: Boolean) {
        if (necessaryToFix) {
            val nodeBinDir = computeNodeBinDir(destinationDir, isWindows).toPath()
            fixBrokenSymlink("npm", nodeBinDir, destinationDir, isWindows)
            fixBrokenSymlink("npx", nodeBinDir, destinationDir, isWindows)
        }
    }

    private fun fixBrokenSymlink(
        name: String,
        nodeBinDirPath: Path,
        nodeDirProvider: File,
        isWindows: Boolean,
    ) {
        val script = nodeBinDirPath.resolve(name)
        val scriptFile = computeNpmScriptFile(nodeDirProvider, name, isWindows)
        if (Files.deleteIfExists(script)) {
            Files.createSymbolicLink(script, nodeBinDirPath.relativize(Paths.get(scriptFile)))
        }
    }

    companion object {
        @Deprecated(
            "Use nodeJsSetupTaskProvider from corresponding NodeJsEnvSpec or WasmNodeJsEnvSpec instead. " +
                    "Scheduled for removal in Kotlin 2.4."
        )
        const val NAME: String = "kotlinNodeJsSetup"

        @InternalKotlinGradlePluginApi
        const val BASE_NAME: String = "nodeJsSetup"
    }
}
