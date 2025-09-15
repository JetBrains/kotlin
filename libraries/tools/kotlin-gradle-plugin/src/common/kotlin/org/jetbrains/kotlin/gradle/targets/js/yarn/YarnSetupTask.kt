/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
abstract class YarnSetupTask @Inject constructor(
    settings: BaseYarnRootEnvSpec
) : AbstractSetupTask<YarnEnv, BaseYarnRootEnvSpec>(settings) {

    @get:Internal
    override val artifactPattern: String
        get() = "v[revision]/[artifact](-v[revision]).[ext]"

    @get:Internal
    override val artifactModule: String
        get() = "com.yarnpkg"

    @get:Internal
    override val artifactName: String
        get() = "yarn"

    override fun extract(archive: File) {
        fs.copy {
            it.from(archiveOperations.tarTree(archive))
            it.into(destinationProvider.getFile().parentFile)
            it.includeEmptyDirs = false
        }
    }

    companion object {
        @Deprecated(
            "Use yarnSetupTaskProvider from corresponding YarnRootExtension or WasmYarnRootExtension instead. " +
                    "Scheduled for removal in Kotlin 2.4.",
            level = DeprecationLevel.ERROR
        )
        const val NAME: String = "kotlinYarnSetup"

        @InternalKotlinGradlePluginApi
        const val BASE_NAME: String = "yarnSetup"
    }
}
