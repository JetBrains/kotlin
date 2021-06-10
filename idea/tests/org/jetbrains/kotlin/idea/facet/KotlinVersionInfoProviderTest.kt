/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.facet

import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.Test
import kotlin.test.assertSame

class KotlinVersionInfoProviderTest {
    @Test
    fun supportedLanguageVersionConsistency() {
        LanguageVersion.values().forEach { languageVersion ->
            assertSame(languageVersion, languageVersion.toString().toLanguageVersion())
        }
    }
}