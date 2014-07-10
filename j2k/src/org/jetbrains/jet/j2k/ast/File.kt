/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.CodeBuilder
import org.jetbrains.jet.j2k.append

class PackageStatement(val packageName: String) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "package " append packageName
    }
}

class File(val elements: List<Element>, val mainFunction: String?) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(elements, "\n")
        if (mainFunction != null) {
            builder append "\n\n" append mainFunction
        }
    }
}
