/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.targets.wasm.runtime

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.utils.getFile
import java.nio.file.Path

abstract class CommonEnvSpec(
    val name: String,
) : EnvSpec<CommonEnv>() {
    final override val env: Provider<CommonEnv> = produceEnv()

    override val executable: Provider<String> = env.map { it.executable }

    val moduleGroup
        get() = "org.jetbrains.kotlin.wasm.runtime"

    abstract val executableCommand: Property<(Path) -> String>

    abstract val extension: Property<String>

    final override fun produceEnv(): Provider<CommonEnv> {
        return download.map { downloadValue ->
            val versionValue = version.get()

            val dirName = downloadBaseUrl.get().hashCode().toByte().toString(16)
            val dir = installationDirectory.getFile().resolve(dirName)

            val downloadUrl = downloadBaseUrl.orNull

            val extensionValue = extension.getOrElse(
                downloadUrl?.let { findExtension(it) }
                    ?: error("Can't detect extension type. Set extension")
            )

            fun getIvyDependency(): String {
                return "$moduleGroup:$name:$versionValue@$extensionValue"
            }

            CommonEnv(
                download = downloadValue,
                dir = dir,
                executable = executableCommand.get()(dir.toPath()),
                ivyDependency = getIvyDependency(),
                downloadBaseUrl = downloadUrl,
                allowInsecureProtocol = allowInsecureProtocol.getOrElse(false),
            )
        }
    }

    private fun findExtension(url: String): String {
        if (url.contains(".tar.")) {
            val potentialExtension = url.substringAfterLast(".tar.")
            if (potentialExtension == url.substringAfterLast(".")) {
                return "tar.$potentialExtension"
            }
        }

        return url.substringAfterLast(".")
    }
}
