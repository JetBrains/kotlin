/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKImportStatementImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKNameIdentifierImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKSymbol
import org.jetbrains.kotlin.nj2k.tree.impl.fqNameToImport
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitorVoid


class ImportStatementConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKFile) return recurse(element)
        for (import in element.declarationList.collectImports()) {
            if (!element.importList.containsImport(import)) {
                element.importList += JKImportStatementImpl(JKNameIdentifierImpl(import))
            }
        }
        return recurse(element)
    }

    private val importExceptionList =
        listOf(
            CommonClassNames.JAVA_UTIL_ARRAY_LIST,
            CommonClassNames.JAVA_UTIL_LIST,
            CommonClassNames.JAVA_UTIL_HASH_SET,
            CommonClassNames.JAVA_UTIL_HASH_MAP,
            CommonClassNames.JAVA_UTIL_COLLECTION,
            CommonClassNames.JAVA_UTIL_ITERATOR,
            "java.util.LinkedHashMap",
            "java.util.LinkedHashSet"
        )


    private fun List<JKImportStatement>.containsImport(import: String) =
        asSequence()
            .map { it.name.value }
            .any {
                it == import ||
                        it.endsWith("*") && import.substringBeforeLast(".") == it.substringBeforeLast(".*")
            }

    private fun List<JKDeclaration>.collectImports(): List<String> {
        val collectImportsVisitor = CollectImportsVisitor()
        forEach {
            it.accept(collectImportsVisitor)
        }
        return collectImportsVisitor.collectedFqNames
    }


    private class CollectImportsVisitor : JKVisitorVoid {
        private val unfilteredCollectedFqNames = mutableSetOf<String>()

        private val defaultImports =
            listOf(
                "kotlin",
                "kotlin.annotation",
                "kotlin.collections",
                "kotlin.comparisons",
                "kotlin.io",
                "kotlin.ranges",
                "kotlin.sequences",
                "kotlin.text",
                "java.lang",
                "kotlin.jvm"
            )

        val collectedFqNames
            get() = unfilteredCollectedFqNames.filter {
                it.substringBeforeLast(".") !in defaultImports && it.contains(".")
            }

        private fun addSymbol(symbol: JKSymbol) {
            symbol.fqNameToImport()?.also { fqName ->
                unfilteredCollectedFqNames += fqName
            }
        }

        override fun visitTreeElement(treeElement: JKTreeElement) {
            treeElement.acceptChildren(this)
        }

        override fun visitClassAccessExpression(classAccessExpression: JKClassAccessExpression) {
            addSymbol(classAccessExpression.identifier)
        }

        override fun visitJavaNewExpression(javaNewExpression: JKJavaNewExpression) {
            addSymbol(javaNewExpression.classSymbol)
            javaNewExpression.acceptChildren(this)
        }

        override fun visitTypeElement(typeElement: JKTypeElement) {
            val classType = typeElement.type as? JKClassType ?: return
            addSymbol(classType.classReference)
        }

        override fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression) {
            addSymbol(fieldAccessExpression.identifier)
        }

        override fun visitMethodCallExpression(methodCallExpression: JKMethodCallExpression) {
            addSymbol(methodCallExpression.identifier)
            methodCallExpression.acceptChildren(this)
        }

        override fun visitAnnotation(annotation: JKAnnotation) {
            addSymbol(annotation.classSymbol)
            annotation.acceptChildren(this)
        }

        override fun visitClassLiteralExpression(classLiteralExpression: JKClassLiteralExpression) {
            val type = classLiteralExpression.classType.type
            if (type is JKClassType) {
                addSymbol(type.classReference)
            }
            classLiteralExpression.acceptChildren(this)
        }
    }
}