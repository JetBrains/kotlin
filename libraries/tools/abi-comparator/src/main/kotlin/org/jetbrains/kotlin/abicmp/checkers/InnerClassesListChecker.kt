/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.classFlags
import org.jetbrains.kotlin.abicmp.defects.DefectType
import org.jetbrains.kotlin.abicmp.defects.INNER_CLASS_A
import org.jetbrains.kotlin.abicmp.isSynthetic
import org.jetbrains.kotlin.abicmp.listOfNotNull
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.reports.ListEntryDiff
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.InnerClassNode

class InnerClassesListChecker : ClassChecker {
    override val name = "class.innerClasses"

    val missing1Defect = DefectType("${name}.missing1", "Missing inner class in #1", INNER_CLASS_A)
    val missing2Defect = DefectType("${name}.missing2", "Missing inner class in #2", INNER_CLASS_A)

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val innerClasses1 = class1.loadInnerClasses()
        val innerClasses2 = class2.loadInnerClasses()

        val relevantInnerClassNames =
            innerClasses1.keys.union(innerClasses2.keys).filter {
                val ic1 = innerClasses1[it]
                val ic2 = innerClasses2[it]
                ic1 != null && !ic1.access.isSynthetic() ||
                        ic2 != null && ic2.access.isSynthetic()
            }
        val innerClassNames1 = innerClasses1.keys.filter { it in relevantInnerClassNames }.sorted()
        val innerClassNames2 = innerClasses2.keys.filter { it in relevantInnerClassNames }.sorted()

        val listDiff = compareLists(innerClassNames1, innerClassNames2) ?: return

        report.addInnerClassesDiffs(
            this,
            listDiff.map {
                ListEntryDiff(
                    it.value1?.let { v1 -> innerClasses1[v1]?.toInnerClassLine() },
                    it.value2?.let { v2 -> innerClasses2[v2]?.toInnerClassLine() }
                )
            }
        )
    }

    private fun ClassNode.loadInnerClasses(): Map<String, InnerClassNode> =
        innerClasses.listOfNotNull<InnerClassNode>()
            .filterNot {
                it.innerName == null || it.innerName == "WhenMappings" || isSamAdapterName(it.name)
            }
            .associateBy { it.name }


    private fun InnerClassNode.toInnerClassLine(): String =
        "INNER_CLASS $name $outerName $innerName ${access.toString(2)} ${access.classFlags()}"
}

fun isSamAdapterName(name: String): Boolean =
    "\$sam$" in name && name.endsWith("$0")
