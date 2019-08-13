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
package com.intellij.codeInsight.completion

import com.intellij.openapi.util.text.StringUtil

/**
 * @author peter
 */
internal data class PresentationInvariant(val itemText: String?, val tail: String?, val type: String?): Comparable<PresentationInvariant> {
  override fun compareTo(other: PresentationInvariant): Int {
    var result = StringUtil.naturalCompare(itemText, other.itemText)
    if (result != 0) return result

    result = Integer.compare(tail?.length ?: 0, other.tail?.length ?: 0)
    if (result != 0) return result

    result = StringUtil.naturalCompare(tail ?: "", other.tail ?: "")
    if (result != 0) return result

    return StringUtil.naturalCompare(type ?: "", other.type ?: "")
  }

}