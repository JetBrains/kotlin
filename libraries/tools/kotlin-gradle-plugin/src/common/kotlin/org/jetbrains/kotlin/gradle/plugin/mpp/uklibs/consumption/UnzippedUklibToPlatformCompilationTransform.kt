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

@DisableCachingByDefault(because = "Investigate caching uklib transforms")
internal abstract class UnzippedUklibToPlatformCompilationTransform :
    TransformAction<UnzippedUklibToPlatformCompilationTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val targetFragmentAttribute: Property<String>

        @get:Input
        val fakeTransform: Property<Boolean>
    }

    internal class PlatformCompilationTransformException(
        val unzippedUklib: File,
        val targetFragmentAttribute: String,
        val availablePlatformFragments: List<String>,
    ) : IllegalStateException(
        "Couldn't resolve platform compilation artifact from $unzippedUklib failed. Needed fragment with attribute '${targetFragmentAttribute}', but only the following fragments were available $availablePlatformFragments"
    )

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        if (parameters.fakeTransform.get()) {
            outputs.dir(inputArtifact.get().asFile)
            return
        }

        val unzippedUklib = inputArtifact.get().asFile
        val targetFragmentAttribute = parameters.targetFragmentAttribute.get()
        val uklib = deserializeUklibFromDirectory(unzippedUklib)
        val platformFragments = uklib
            .module.fragments
            .filter { it.attributes.singleOrNull() == targetFragmentAttribute }

        if (platformFragments.isEmpty()) {
            /**
             * FIXME: Uklib spec mentions that there may be an intermediate fragment without refiners. Was this a crutch for kotlin-test?
             *
             * Should we check this case and silently ignore this case here by detecting that the file is not present?
             */
            // FIXME: 02.02.2025 - Now we no longer need this
//            throw PlatformCompilationTransformException(
//                unzippedUklib,
//                targetFragmentAttribute,
//                uklib.module.fragments.map { it.identifier }.sorted()
//            )
        }

        if (platformFragments.size > 1) {
            error("Matched multiple fragments from ${unzippedUklib}, but was expecting to find exactly one. Found fragments: $platformFragments")
        }

        platformFragments.singleOrNull()?.let {
            outputs.dir(it.file())
        }
    }
}