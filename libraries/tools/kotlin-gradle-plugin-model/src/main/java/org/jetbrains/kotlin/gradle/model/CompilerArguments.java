/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * Represents the compiler arguments for a given Kotlin source set.
 * @see SourceSet
 */
public interface CompilerArguments {

    /**
     * Return current arguments for the given source set.
     *
     * @return current arguments for the given source set.
     */
    @NotNull
    List<String> getCurrentArguments();

    /**
     * Return default arguments for the given source set.
     *
     * @return default arguments for the given source set.
     */
    @NotNull
    List<String> getDefaultArguments();

    /**
     * Return the classpath the given source set is compiled against.
     *
     * @return the classpath the given source set is compiled against.
     */
    @NotNull
    List<File> getCompileClasspath();
}
