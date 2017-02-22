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

package org.jetbrains.kotlin.java.model

import javax.lang.model.element.Name

fun JeName(name: String?) = name?.let(::JeName) ?: JeName.EMPTY

class JeName(val name: String) : Name, CharSequence by name {
    override fun contentEquals(cs: CharSequence?) = cs?.toString() == name
    
    override fun toString() = name
    
    companion object {
        val EMPTY = JeName("")
        val INIT = JeName("<init>")
        val CLINIT = JeName("<clinit>")
    }
}