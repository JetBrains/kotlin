/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import java.io.File

@DisableCachingByDefault
abstract class YarnSetupTask : AbstractSetupTask<YarnEnv, YarnRootExtension>() {
    @Transient
    @Internal
    override val settings = project.yarn

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
        val dirInTar = archive.name.removeSuffix(".tar.gz")
        fs.copy {
            it.from(archiveOperations.tarTree(archive))
            it.into(destination.parentFile)
            it.includeEmptyDirs = false
            it.eachFile { fileCopy ->
                fileCopy.path = fileCopy.path.removePrefix(dirInTar)
            }
        }
    }

    companion object {
        const val NAME: String = "kotlinYarnSetup"
    }
}
