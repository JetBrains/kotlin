/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.tests

import org.jetbrains.kotlin.generators.tests.testGroup
import org.jetbrains.kotlin.idea.codeInsight.AbstractInspectionTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixTest
import org.jetbrains.kotlin.idea.spring.tests.generate.AbstractGenerateSpringDependencyActionTest
import org.jetbrains.kotlin.idea.spring.tests.gutter.AbstractSpringClassAnnotatorTest
import org.jetbrains.kotlin.idea.spring.tests.references.AbstractSpringReferenceCompletionHandlerTest
import org.jetbrains.kotlin.idea.spring.tests.references.AbstractSpringReferenceCompletionTest
import org.jetbrains.kotlin.idea.spring.tests.references.AbstractSpringReferenceNavigationTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("ultimate/tests", "ultimate/testData") {
        testClass<AbstractInspectionTest>("UltimateInspectionTestGenerated") {
            model("inspections", pattern = "^(inspections\\.test)$", singleClass = true)
        }

        testClass<AbstractQuickFixTest>("UltimateQuickFixTestGenerated") {
            model("quickFixes", pattern = "^([\\w\\-_]+)\\.kt$", filenameStartsLowerCase = true)
        }

        testClass<AbstractSpringClassAnnotatorTest>() {
            model("spring/core/gutter", extension = "test", singleClass = true)
        }

        testClass<AbstractSpringReferenceCompletionHandlerTest>() {
            model("spring/core/references/completion/handler")
        }

        testClass<AbstractSpringReferenceCompletionTest>() {
            model("spring/core/references/completion/variants")
        }

        testClass<AbstractSpringReferenceNavigationTest>() {
            model("spring/core/references/navigation")
        }

        testClass<AbstractGenerateSpringDependencyActionTest>() {
            model("spring/core/generate", pattern = "^([\\w]+)\\.kt$", singleClass = true)
        }
    }
}
