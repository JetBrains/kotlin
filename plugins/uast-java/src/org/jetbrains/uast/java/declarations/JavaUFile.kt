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
package org.jetbrains.uast.java

import com.intellij.psi.PsiJavaFile
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUFile(override val psi: PsiJavaFile): JavaAbstractUElement(), UFile, PsiElementBacked {
    override val packageFqName by lz { psi.packageName.let { if (it.isNotBlank()) it else null } }

    override val importStatements: List<UImportStatement> by lz {
        val importList = psi.importList ?: return@lz emptyList<UImportStatement>()
        importList.importStatements.map { JavaConverter.convert(it, this) }.filterNotNull()
    }

    override val declarations by lz { psi.classes.map { JavaUClass(it, this) } }
}