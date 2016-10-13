/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package org.jetbrains.uast.expressions

import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UResolvable

interface UReferenceExpression : UExpression, UResolvable {
    /**
     * Returns the resolved name for this reference, or null if the reference can't be resolved.
     */
    val resolvedName: String?
    
    override fun asLogString() = "UReferenceExpression"
}