/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.defects.DefectType
import org.jetbrains.kotlin.abicmp.defects.FIELD_A
import org.jetbrains.kotlin.abicmp.fieldFlags
import org.jetbrains.kotlin.abicmp.isSynthetic
import org.jetbrains.kotlin.abicmp.listOfNotNull
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.reports.ListEntryDiff
import org.jetbrains.kotlin.abicmp.tasks.fieldId
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode

class FieldsListChecker : ClassChecker {
    override val name = "class.fields"

    val missing1Defect = DefectType("${name}.missing1", "Missing field in #1", FIELD_A)
    val missing2Defect = DefectType("${name}.missing2", "Missing field in #2", FIELD_A)

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val fields1 = class1.loadFields()
        val fields2 = class2.loadFields()

        val relevantFieldIds = fields1.keys.union(fields2.keys)
            .filter {
                val field1 = fields1[it]
                val field2 = fields2[it]
                !(field1 != null && !field1.access.isSynthetic() ||
                        field2 != null && !field2.access.isSynthetic())
            }.toSet()

        val fieldIds1 = fields1.keys.intersect(relevantFieldIds).sorted()

        val fieldIds2 = fields2.keys.intersect(relevantFieldIds).sorted()

        val listDiff = compareLists(fieldIds1, fieldIds2) ?: return
        report.addFieldListDiffs(
            this,
            listDiff.map {
                ListEntryDiff(
                    it.value1?.toFieldWithFlags(fields1),
                    it.value2?.toFieldWithFlags(fields2)
                )
            }
        )
    }

    private fun String.toFieldWithFlags(fields: Map<String, FieldNode>): String {
        val field = fields[this] ?: return this
        return "$this ${field.access.fieldFlags()}"
    }
}

fun ClassNode.loadFields(): Map<String, FieldNode> =
    fields.listOfNotNull<FieldNode>().filter {
        (it.access and Opcodes.ACC_PUBLIC) != 0 ||
                (it.access and Opcodes.ACC_PROTECTED) != 0
    }.associateBy { it.fieldId() }
