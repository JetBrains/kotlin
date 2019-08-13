/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.completion.enhancer

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.Key

object InvokationCountOrigin {
    private val ORIGIN_KEY = Key.create<Int>("second.completion.run")

    fun setInvocationTime(element: LookupElement, number: Int) {
        element.putUserData(ORIGIN_KEY, number)
    }

    fun invokationTime(element: LookupElement): Int = element.getUserData(ORIGIN_KEY) ?: 0
}
