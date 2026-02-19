/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.annotations.jvm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the Java declaration is an actualization for the existing Kotlin {@code expect} declaration.
 * The pair of Kotlin {@code expect} and Java {@code actual} declarations must have the same FQN (fully qualified name)
 *
 * <p>The annotation can be used only in Java and acts similarly to the {@code actual} keyword in Kotlin.
 * Since the annotation is meaningless when used in Kotlin, the Kotlin compiler reports any usages of this annotation in Kotlin code.
 *
 * <p>Until the feature becomes stable, you need to pass an experimental {@code -Xdirect-java-actualization} flag to the Kotlin compiler.
 * You can track the feature stability in the <a href="https://youtrack.jetbrains.com/issue/KT-67202">YouTrack feature request</a>
 *
 * @see <a href="https://kotlinlang.org/docs/multiplatform-expect-actual.html">Expect and actual declarations documentation</a>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface KotlinActual {
}
