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

package org.jetbrains.kotlin.jps.build

import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.JpsSimpleElement
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase
import org.jetbrains.kotlin.incremental.components.LookupTracker

private val TESTING_CONTEXT = JpsElementChildRoleBase.create<JpsSimpleElement<out TestingContext>>("Testing context")

@TestOnly
fun JpsProject.setTestingContext(context: TestingContext) {
    val dataContainer = JpsElementFactory.getInstance().createSimpleElement(context)
    container.setChild(TESTING_CONTEXT, dataContainer)
}

val JpsProject.testingContext: TestingContext?
    get() = container.getChild(TESTING_CONTEXT)?.data

val CompileContext.testingContext: TestingContext?
    get() = projectDescriptor?.project?.testingContext

class TestingContext(
    val lookupTracker: LookupTracker,
    val buildLogger: BuildLogger
)
