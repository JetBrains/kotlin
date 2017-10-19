/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.spring.tests.findUsages.AbstractSpringFindUsagesTest
import org.jetbrains.kotlin.idea.spring.tests.generate.AbstractGenerateSpringDependencyActionTest
import org.jetbrains.kotlin.idea.spring.tests.gutter.AbstractSpringClassAnnotatorTest
import org.jetbrains.kotlin.idea.spring.tests.inspections.AbstractSpringInspectionTest
import org.jetbrains.kotlin.idea.spring.tests.quickfixes.AbstractSpringQuickFixTest
import org.jetbrains.kotlin.idea.spring.tests.references.AbstractSpringReferenceCompletionHandlerTest
import org.jetbrains.kotlin.idea.spring.tests.references.AbstractSpringReferenceCompletionTest
import org.jetbrains.kotlin.idea.spring.tests.references.AbstractSpringReferenceNavigationTest
import org.jetbrains.kotlin.idea.spring.tests.rename.AbstractSpringRenameTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("ultimate/tests", "ultimate/testData") {
        testClass<AbstractSpringInspectionTest> {
            model("inspections/spring", pattern = "^(inspections\\.test)$", singleClass = true)
        }

        testClass<AbstractSpringQuickFixTest> {
            model("quickFixes/spring", pattern = "^([\\w\\-_]+)\\.kt$", filenameStartsLowerCase = true)
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

        testClass<AbstractSpringRenameTest>() {
            model("spring/core/rename", extension = "test", singleClass = true)
        }

        testClass<AbstractSpringFindUsagesTest>() {
            model("spring/core/findUsages", pattern = """^(.+)\.kt$""")
        }
    }
}
