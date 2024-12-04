/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.settings;

import org.jetbrains.kotlin.konan.test.blackbox.support.group.TestCaseGroupProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Has to use Java annotation here instead of Kotlin annotation because of KT-49920.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestConfiguration {
    Class<? extends TestCaseGroupProvider> providerClass();
    Class<?>[] requiredSettings() default {};
}
