/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.kotlin.sample

import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaSourceSet

/**
 * Declares a capability that a `.kt` file can be created that will be used as a usage sample.
 */
interface KotlinSampleFileCreator {

    /**
     * Creates a `.kt` file outside of the source code directory. It should be used as input
     * for the `@sample` KDoc tag.
     *
     * To be picked by Dokka, this file must be included in [TestDokkaSourceSet.samples].
     *
     * @param pathFromProjectRoot path relative to the root of the test project. Must begin
     *                            with `/` to not confuse it with relative paths. Example: `/samples/collections.kt`
     * @param fqPackageName fully qualified package name to be used in the `package` statement of the file.
     *                      This parameter must be set because the package name cannot be deduced
     *                      from the file path, as samples usually reside outside of the source code directory.
     *                      Example: `org.jetbrains.dokka.sample.collections`
     */
    fun sampleFile(
        pathFromProjectRoot: String,
        fqPackageName: String,
        fillFile: KotlinSampleTestDataFile.() -> Unit
    )
}
