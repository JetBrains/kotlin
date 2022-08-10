/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.coreCases

import org.jetbrains.kotlin.project.model.infra.KpmTestCase
import org.jetbrains.kotlin.project.model.testDsl.*

object SimpleTwoLevel : KpmTestCaseDescriptor {
    override fun KpmTestCase.describeCase() {
        project("p") {
            allModules {
                jvm()
                macosX64()
            }
        }
    }
}
