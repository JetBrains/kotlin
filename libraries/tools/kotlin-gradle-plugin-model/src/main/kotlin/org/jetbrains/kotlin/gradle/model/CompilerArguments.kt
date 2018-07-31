/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import java.io.File

/**
 * Represents the compiler arguments for a given Kotlin source set.
 * @see SourceSet
 */
interface CompilerArguments {

    /**
     * Return current arguments for the given source set.
     *
     * @return current arguments for the given source set.
     */
    val currentArguments: List<String>

    /**
     * Return default arguments for the given source set.
     *
     * @return default arguments for the given source set.
     */
    val defaultArguments: List<String>

    /**
     * Return the classpath the given source set is compiled against.
     *
     * @return the classpath the given source set is compiled against.
     */
    val compileClasspath: List<File>
}