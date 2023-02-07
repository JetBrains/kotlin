/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.incremental.web.TranslationResultValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class JsClassicIncrementalDataProvider(private val testServices: TestServices) : TestService {
    class IncrementalData(
        var header: ByteArray? = null,
        val translatedFiles: MutableMap<File, TranslationResultValue> = hashMapOf(),
        val packageMetadata: MutableMap<String, ByteArray> = hashMapOf()
    ) {
        fun copy(): IncrementalData {
            return IncrementalData(
                header?.clone(),
                translatedFiles.toMutableMap(),
                packageMetadata.toMutableMap()
            )
        }
    }

    private val cache: MutableMap<TestModule, IncrementalData> = mutableMapOf()

    fun recordIncrementalData(module: TestModule, incrementalData: IncrementalData) {
        cache[module] = incrementalData
    }

    fun getIncrementalData(module: TestModule): IncrementalData {
        return cache.getValue(module)
    }

    fun getIncrementalDataIfAny(module: TestModule): IncrementalData? {
        return cache[module]
    }
}

val TestServices.jsClassicIncrementalDataProvider: JsClassicIncrementalDataProvider by TestServices.testServiceAccessor()
