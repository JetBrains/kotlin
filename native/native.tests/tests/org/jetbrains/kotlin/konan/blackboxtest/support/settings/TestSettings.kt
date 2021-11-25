/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import org.jetbrains.kotlin.konan.blackboxtest.support.group.StandardTestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.group.TestCaseGroupProvider
import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class TestSettings(val providerClass: KClass<out TestCaseGroupProvider>) {
    companion object {
        val DEFAULT = TestSettings(StandardTestCaseGroupProvider::class)
    }
}
