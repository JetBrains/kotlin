/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.serialization

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE

open class AbstractFirNativeSerializationWithInlinedFunInKlibTest : AbstractFirNativeSerializationTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
        }
    }
}