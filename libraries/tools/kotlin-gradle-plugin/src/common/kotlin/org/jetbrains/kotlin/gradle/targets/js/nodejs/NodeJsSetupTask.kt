package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@DisableCachingByDefault
abstract class NodeJsSetupTask : AbstractSetupTask<NodeJsEnv, NodeJsExtension>() {
    @Transient
    @get:Internal
    override val settings = project.kotlinNodeJsExtension

    @get:Internal
    override val artifactPattern: String
        get() = "v[revision]/[artifact](-v[revision]-[classifier]).[ext]"

    @get:Internal
    override val artifactModule: String
        get() = "org.nodejs"

    @get:Internal
    override val artifactName: String
        get() = "node"

    override fun extract(archive: File) {
        var fixBrokenSymLinks = false

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

        fixBrokenSymlinks(this.destination, env.isWindows, fixBrokenSymLinks)

        if (!env.isWindows) {
            File(env.executable).setExecutable(true)
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
        const val NAME: String = "kotlinNodeJsSetup"
    }
}
