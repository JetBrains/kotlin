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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import java.util.HashSet

public open class ClassVisitor() : JavaRecursiveElementVisitor() {
    private val myClassIdentifiers = HashSet<String>()

    public open fun getClassIdentifiers(): Set<String> {
        return HashSet<String>(myClassIdentifiers)
    }

    public override fun visitClass(aClass: PsiClass?) {
        val qName = aClass?.getQualifiedName()
        if (qName != null) {
            myClassIdentifiers.add(qName)
        }
        super.visitClass(aClass)
    }
}
