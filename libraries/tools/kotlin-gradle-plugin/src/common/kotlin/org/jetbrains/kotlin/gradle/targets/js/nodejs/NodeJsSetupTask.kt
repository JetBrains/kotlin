package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.deleteIfExists

@DisableCachingByDefault
abstract class NodeJsSetupTask @Inject constructor(
    settings: BaseNodeJsEnvSpec,
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

        val destination = destinationProvider.getFile()
        fs.copy {
            it.from(
                when {
                    archive.name.endsWith("zip") -> archiveOperations.zipTree(archive)
                    else -> archiveOperations.tarTree(archive)
                }
            )
            it.into(destination.parentFile)
        }

        if (!archive.name.endsWith("zip")) {
            fixBrokenSymlinks(destination.toPath(), isWindows.get())
        }

        if (!isWindows.get()) {
            File(executable.get()).setExecutable(true)
        }
    }

    private fun fixBrokenSymlinks(destinationDir: Path, isWindows: Boolean) {
        val nodeBinDir = computeNodeBinDir(destinationDir, isWindows)
        fixBrokenSymlink("npm", nodeBinDir, destinationDir, isWindows)
        fixBrokenSymlink("npx", nodeBinDir, destinationDir, isWindows)
    }

    private fun fixBrokenSymlink(
        name: String,
        nodeBinDirPath: Path,
        nodeDir: Path,
        isWindows: Boolean,
    ) {
        val script = nodeBinDirPath.resolve(name)
        val scriptFile = computeNpmScriptFile(nodeDir, name, isWindows)
        val target = nodeBinDirPath.relativize(scriptFile)

        if (script.deleteIfExists()) {
            script.createSymbolicLinkPointingTo(target)
            logger.info("Created symlink for $name. $script -> $target")
        }
    }

    companion object {
        @InternalKotlinGradlePluginApi
        const val BASE_NAME: String = "nodeJsSetup"
    }
}
