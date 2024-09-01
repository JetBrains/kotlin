/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import kotlinBuildProperties
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.properties.resolvablePropertyString
import org.jetbrains.kotlin.konan.target.AbstractToolConfig
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import org.jetbrains.kotlin.kotlinNativeDist
import java.util.*


private val load0 = Runtime::class.java.getDeclaredMethod("load0", Class::class.java, String::class.java).also {
    it.isAccessible = true
}

private class CliToolConfig(konanHome: String, target: String) : AbstractToolConfig(konanHome, target, emptyMap()) {
    override fun loadLibclang() {
        // Load libclang into the system class loader. This is needed to allow developers to make changes
        // in the tooling infrastructure without having to stop the daemon (otherwise libclang might end up
        // loaded in two different class loaders which is not allowed by the JVM).
        load0.invoke(Runtime.getRuntime(), String::class.java, libclang)
    }
}

/** Kotlin/Native C-interop tool runner */
internal class KonanCliInteropRunner(
        fileOperations: FileOperations,
        execOperations: ExecOperations,
        logger: Logger,
        private val projectLayout: ProjectLayout,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        konanHome: String,
        target: KonanTarget,
        private val allowRunningCinteropInProcess: Boolean,
) : KonanCliRunner("cinterop", fileOperations, execOperations, logger, isolatedClassLoadersService, konanHome) {
    init {
        CliToolConfig(konanHome, target.visibleName).prepare()
    }

    override val mustRunViaExec: Boolean
        get() = if (allowRunningCinteropInProcess) {
            super.mustRunViaExec
        } else {
            true
        }

    override fun transformArgs(args: List<String>): List<String> {
        return super.transformArgs(args) + listOf("-Xproject-dir", projectLayout.projectDirectory.asFile.absolutePath)
    }

    override val execEnvironment by lazy {
        val result = mutableMapOf<String, String>()
        result.putAll(super.execEnvironment)
        result["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
        llvmExecutablesPath?.let {
            result["PATH"] = "$it;${System.getenv("PATH")}"
        }
        result
    }

    private val llvmExecutablesPath: String? by lazy {
        if (HostManager.host == KonanTarget.MINGW_X64) {
            // TODO: Read it from Platform properties when it is accessible.
            val konanProperties = Properties().apply {
                fileOperations.file("$konanHome/konan/konan.properties").inputStream().use(::load)
            }

            konanProperties.resolvablePropertyString("llvmHome.mingw_x64")?.let { toolchainDir ->
                DependencyDirectories.defaultDependenciesRoot
                        .resolve("$toolchainDir/bin")
                        .absolutePath
            }
        } else
            null
    }
}