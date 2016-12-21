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

class TemporaryVariableEliminationTest : BasicOptimizerTest("temporary-variable") {
    @Test fun declaration() = box()

    @Test fun assignment() = box()

    @Test fun skipsGlobalDeclarations() = box()

    @Test fun methodCall() = box()

    @Test fun ifBranch() = box()

    @Test fun tryCatch() = box()

    @Test fun nonSideEffect() = box()

    @Test fun propertyAccess() = box()

    @Test fun removeUnused() = box()

    @Test fun innerExpressionProcessed() = box()

    @Test fun transitiveNotConsideredTrivial() = box()

    @Test fun assignmentToNonLocal() = box()

    @Test fun removeUnusedAndSubstitute() = box()
}