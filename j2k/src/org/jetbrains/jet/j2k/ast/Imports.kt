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

import com.intellij.psi.PsiImportStatementBase
import org.jetbrains.jet.j2k.*
import com.intellij.psi.PsiImportList
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.util.QualifiedNamesUtil
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap


public class Import(val name: String) : Node {
    public override fun toKotlin() = "import " + name
}

public class ImportList(val imports: List<Import>) : Node {
    override fun toKotlin() = imports.filter {
        !it.name.isEmpty() && it.name !in NOT_NULL_ANNOTATIONS
    }.filter {
        // If name is invalid, like with star imports, don't try to filter
        if (!QualifiedNamesUtil.isValidJavaFqName(it.name))
            true
        else {
            // If imported class has a kotlin analog, drop the import
            val kotlinAnalogsForClass = JavaToKotlinClassMap.getInstance().mapPlatformClass(FqName(it.name))
            kotlinAnalogsForClass.isEmpty()
        }
    }.toKotlin("\n")
}

public fun Converter.importsToImportList(importList: PsiImportList): ImportList =
        ImportList(importList.getAllImportStatements() map { importToImport(it) })

public fun Converter.importToImport(i: PsiImportStatementBase): Import {
    val reference = i.getImportReference()
    if (reference != null) {
        return Import(quoteKeywords(reference.getQualifiedName()!!) + if (i.isOnDemand()) ".*" else "")
    }
    return Import("")
}