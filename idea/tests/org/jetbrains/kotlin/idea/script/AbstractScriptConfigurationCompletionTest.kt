/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import org.jetbrains.kotlin.idea.completion.test.testCompletion
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

abstract class AbstractScriptConfigurationCompletionTest : AbstractScriptConfigurationTest() {
    fun doTest(path: String) {
        configureScriptFile(path)
        testCompletion(
            file.text,
            JvmPlatforms.defaultJvmPlatform,
            additionalValidDirectives = switches,
            complete = { completionType, count ->
                setType(completionType)
                complete(count)
                myItems
            })
    }
}