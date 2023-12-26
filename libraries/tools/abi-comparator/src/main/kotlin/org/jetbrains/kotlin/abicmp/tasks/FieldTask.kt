/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.fieldFlags
import org.jetbrains.kotlin.abicmp.reports.FieldReport
import org.jetbrains.kotlin.abicmp.tag
import org.jetbrains.org.objectweb.asm.tree.FieldNode

class FieldTask(
    private val checkerConfiguration: CheckerConfiguration,
    private val field1: FieldNode,
    private val field2: FieldNode,
    private val report: FieldReport,
) : Runnable {

    override fun run() {
        addFieldInfo()

        for (checker in checkerConfiguration.enabledFieldCheckers) {
            checker.check(field1, field2, report)
        }
    }

    private fun addFieldInfo() {
        report.info {
            tag("p") {
                tag("b", report.header1)
                println(": ${field1.access.fieldFlags()}")
            }
            tag("p") {
                tag("b", report.header2)
                println(": ${field2.access.fieldFlags()}")
            }
        }
    }
}
