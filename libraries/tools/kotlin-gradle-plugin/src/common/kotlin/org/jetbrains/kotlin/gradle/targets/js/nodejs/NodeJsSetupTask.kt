package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.extractNodeJs
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.setUpNodeJs
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

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
        archiveOperations.extractNodeJs(fs, archive, destinationProvider.getFile().parentFile)
        setUpNodeJs(logger, archive, destinationProvider.getFile(), isWindows.get(), executable.get())
    }

    companion object {
        @InternalKotlinGradlePluginApi
        const val BASE_NAME: String = "nodeJsSetup"
    }
}
