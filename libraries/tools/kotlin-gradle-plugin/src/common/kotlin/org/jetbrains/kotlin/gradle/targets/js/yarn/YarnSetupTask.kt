/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
abstract class YarnSetupTask @Inject constructor(
    settings: YarnRootEnvSpec
) : AbstractSetupTask<YarnEnv, YarnRootEnvSpec>(settings) {

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
        const val NAME: String = "kotlinYarnSetup"
    }
}
