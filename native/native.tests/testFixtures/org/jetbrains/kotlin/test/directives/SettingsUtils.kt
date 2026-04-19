/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives

/**
 * Gets the [TestKind] defined by possible [RegisteredDirectives].
 * The default is determined by TEST_KIND global property, for ex. testrunner's annotation:
 *      @EnforcedProperty(property = ClassLevelProperty.TEST_KIND, propertyValue = "STANDALONE_NO_TR")
 */
fun Settings.testKindFrom(directives: RegisteredDirectives?): TestKind =
    directives?.testKind ?: get<TestKind>()
