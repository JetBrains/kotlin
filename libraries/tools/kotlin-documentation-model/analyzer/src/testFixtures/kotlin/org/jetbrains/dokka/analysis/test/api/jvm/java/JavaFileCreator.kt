/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.java

/**
 * Declares a capability that a `.java` file can be created in the scope of the implementation.
 */
interface JavaFileCreator {

    /**
     * Creates a `.java` file.
     *
     * By default, the package of this file is deduced automatically from the [pathFromSrc] param.
     * For example, for a path `org/jetbrains/dokka/test` the package will be `org.jetbrains.dokka.test`.
     * It is normally prohibited for Java files to have a mismatch in package and file path, so it
     * cannot be overridden.
     *
     * @param pathFromSrc path relative to the source code directory of the project.
     *                    Must contain packages (if any) and end in `.java`.
     *                    Example: `org/jetbrains/dokka/test/MyClass.java`
     */
    fun javaFile(pathFromSrc: String, fillFile: JavaTestDataFile.() -> Unit)
}
