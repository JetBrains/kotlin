/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.deserializeUklibFromDirectory
import java.io.File

@CacheableTransform
internal abstract class UnzippedUklibToPlatformCompilationTransform :
    TransformAction<UnzippedUklibToPlatformCompilationTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val targetFragmentAttribute: Property<String>
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val unzippedUklib = inputArtifact.get().asFile
        val targetFragmentAttribute = parameters.targetFragmentAttribute.get()
        val uklib = deserializeUklibFromDirectory(unzippedUklib)
        val platformFragments = uklib
            .module.fragments
            .filter { it.attributes.singleOrNull() == targetFragmentAttribute }

        if (platformFragments.isEmpty()) {
            /**
             * Platform fragment didn't exist in the Uklib. Per lenient interlibrary resolution rules, just return no artifacts for this variant
             */
            return
        }

        if (platformFragments.size > 1) {
            error("Matched multiple fragments from ${unzippedUklib}, but was expecting to find exactly one. Found fragments: $platformFragments")
        }

        outputs.dir(platformFragments.singleOrNull()!!.file)
    }
}