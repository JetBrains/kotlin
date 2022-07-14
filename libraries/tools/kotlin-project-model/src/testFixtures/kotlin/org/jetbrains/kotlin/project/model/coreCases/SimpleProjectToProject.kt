/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.coreCases

import org.jetbrains.kotlin.project.model.infra.KpmTestCase
import org.jetbrains.kotlin.project.model.testDsl.*

object SimpleProjectToProject : KpmTestCaseWrapper {
    override val case: KpmTestCase = describeCase(applyDefaults = false) {
        allModules {
            jvm()
            macosX64()
        }

        val a = project("a")
        val b = project("b")

        a.depends(b)
    }
}
