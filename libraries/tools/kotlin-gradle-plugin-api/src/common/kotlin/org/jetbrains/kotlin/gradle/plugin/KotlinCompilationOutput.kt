/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import java.io.File

/**
 * Represents the outputs of a Kotlin source set compilation.
 */
interface KotlinCompilationOutput {

    /**
     * @suppress
     */
    @Deprecated(
        "Changing resource output for Kotlin compilation is deprecated. " +
                "Please either use 'resourcesDir' to get the resource location or 'KotlinSourceSet.resources' to configure additional " +
                "resources location for compilation.",
        replaceWith = ReplaceWith("resourcesDir"),
    )
    var resourcesDirProvider: Any

    /**
     * The directory where the resources are located.
     */
    val resourcesDir: File

    /**
     * The collection of directories where the compiled code is located.
     *
     * For example, in the case of JVM target compilation,
     * this will be directories containing class files for Java and Kotlin sources compilations.
     */
    val classesDirs: ConfigurableFileCollection

    /**
     * The collection of all output directories produced by the compilation.
     *
     * Usually combines all output directories from [classesDirs] and [resourcesDir].
     */
    val allOutputs: FileCollection
}
