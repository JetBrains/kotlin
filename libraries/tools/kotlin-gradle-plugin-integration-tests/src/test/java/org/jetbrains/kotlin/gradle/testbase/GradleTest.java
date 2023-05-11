/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Has to be java annotation
// Workaround for https://youtrack.jetbrains.com/issue/IDEA-265284
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@GradleTestVersions
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(GradleArgumentsProvider.class)
public @interface GradleTest {
}
