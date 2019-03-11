/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import org.jetbrains.kotlin.idea.completion.test.testCompletion
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms

abstract class AbstractScriptConfigurationCompletionTest : AbstractScriptConfigurationTest() {
    fun doTest(path: String) {
        configureScriptFile(path)
        testCompletion(
            file.text,
            DefaultBuiltInPlatforms.jvmPlatform,
            additionalValidDirectives = switches,
            complete = { completionType, count ->
                setType(completionType)
                complete(count)
                myItems
            })
    }
}