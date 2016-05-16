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

package org.jetbrains.kotlin.js.test.optimizer

import org.junit.Test

class WhileConditionFoldingTest : BasicOptimizerTest("while-condition-folding") {
    @Test fun simpleWhile() = box()

    @Test fun simpleDoWhile() = box()

    @Test fun consequentConditions() = box()

    @Test fun nestedConditions() = box()

    @Test fun doWhileWithContinue() = box()

    @Test fun whileEvaluationOrder() = box()

    @Test fun doWhileEvaluationOrder() = box()

    @Test fun inNestedLoop() = box()

    @Test fun inLabeledBlock() = box()

    @Test fun doWhileWithNestedContinue() = box()

    @Test fun labeledContinueInNestedLoop() = box()
}