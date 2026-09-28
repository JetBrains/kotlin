/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.markdown

import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaSourceSet

/**
 * Declares a capability that an `.md` file can be created in the scope of the implementation.
 */
interface MdFileCreator {

    /**
     * Creates an `.md` (Markdown) file.
     *
     * If you want to use this file for module and package documentation, it must be included
     * in [TestDokkaConfiguration.includes] or [TestDokkaSourceSet.includes].
     *
     * @param pathFromProjectRoot path relative to the root of the test project. Must begin
     *                            with `/` to not confuse it with relative paths. Example: `/docs/core-package.md`
     */
    fun mdFile(
        pathFromProjectRoot: String,
        fillFile: MarkdownTestDataFile.() -> Unit
    )
}
