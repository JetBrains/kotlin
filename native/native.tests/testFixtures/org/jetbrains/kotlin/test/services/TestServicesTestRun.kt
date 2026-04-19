/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRunSettings

// TODO: KT-85850,KT-85851: To support more backends in grouped blackbox testing,
//   these accessors deserve to be moved to `compiler/tests-common-new`, into package `org.jetbrains.kotlin.test.services`
val TestServices.testRunSettings: TestRunSettings by TestServices.testServiceAccessor()
val TestServices.testRunProvider: TestRunProvider by TestServices.testServiceAccessor()
