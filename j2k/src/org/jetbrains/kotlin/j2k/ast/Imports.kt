/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.ast

import com.intellij.psi.PsiImportStatementBase
import org.jetbrains.kotlin.j2k.*
import com.intellij.psi.PsiImportList
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import com.intellij.psi.PsiJavaCodeReferenceElement
import org.jetbrains.kotlin.asJava.KotlinLightClassForFacade

class Import(val name: String) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "import " append name
    }
}

class ImportList(public var imports: List<Import>) : Element() {
    override val isEmpty: Boolean
        get() = imports.isEmpty()

    override fun generateCode(builder: CodeBuilder) {
        builder.append(imports, "\n")
    }
}

public fun Converter.convertImportList(importList: PsiImportList): ImportList =
        ImportList(importList.getAllImportStatements().map { convertImport(it, true) }.filterNotNull()).assignPrototype(importList)

public fun Converter.convertImport(anImport: PsiImportStatementBase, filter: Boolean): Import? {
    fun doConvert(): Import? {
        val reference = anImport.getImportReference()
        if (reference == null) return null
        val qualifiedName = quoteKeywords(reference.getQualifiedName()!!)
        if (anImport.isOnDemand()) {
            return Import(qualifiedName + ".*")
        }
        else {
            return if (filter) {
                val filteredName = filterImport(qualifiedName, reference)
                if (filteredName != null) Import(filteredName) else null
            }
            else {
                Import(qualifiedName)
            }
        }
    }

    return doConvert()?.assignPrototype(anImport)
}

private fun Converter.filterImport(name: String, ref: PsiJavaCodeReferenceElement): String? {
    if (annotationConverter.isImportNotRequired(name)) return null

    // If imported class has a kotlin analog, drop the import
    if (!JavaToKotlinClassMap.INSTANCE.mapPlatformClass(FqName(name)).isEmpty()) return null

    val target = ref.resolve()
    if (target is KotlinLightClassForFacade) {
        return quoteKeywords(target.getFqName().parent().toString()) + ".*"
    }

    return name
}
