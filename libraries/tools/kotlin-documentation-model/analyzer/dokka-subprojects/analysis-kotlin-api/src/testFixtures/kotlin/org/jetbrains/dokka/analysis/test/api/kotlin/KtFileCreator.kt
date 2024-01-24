/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.kotlin

import org.jetbrains.dokka.analysis.test.api.util.filePathToPackageName

/**
 * Declares a capability that a `.kt` file can be created in the scope of the implementation.
 */
interface KtFileCreator {

    /**
     * Creates a `.kt` file.
     *
     * By default, the package of this file is deduced automatically from the [pathFromSrc] param.
     * For example, for a path `org/jetbrains/dokka/test` the package will be `org.jetbrains.dokka.test`.
     * The package can be overridden by setting [fqPackageName].
     *
     * @param pathFromSrc path relative to the source code directory of the project.
     *                    Must contain packages (if any) and end in `.kt`.
     *                    Example: `org/jetbrains/dokka/test/File.kt`
     * @param fqPackageName package name to be used in the `package` statement of this file.
     *                      This value overrides the automatically deduced package.
     */
    fun ktFile(
        pathFromSrc: String,
        fqPackageName: String = filePathToPackageName(pathFromSrc),
        fillFile: KotlinTestDataFile.() -> Unit
    )
}
