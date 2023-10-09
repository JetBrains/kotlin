package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.methodFlags
import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.kotlin.abicmp.tag
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class MethodTask(
    private val checkerConfiguration: CheckerConfiguration,
    private val method1: MethodNode,
    private val method2: MethodNode,
    private val report: MethodReport,
) : Runnable {

    override fun run() {
        addMethodInfo()

        for (checker in checkerConfiguration.enabledMethodCheckers) {
            checker.check(method1, method2, report)
        }
    }

    private fun addMethodInfo() {
        report.info {
            tag("p") {
                tag("b", report.header1)
                println(": ${method1.access.methodFlags()}")
            }
            tag("p") {
                tag("b", report.header2)
                println(": ${method2.access.methodFlags()}")
            }
        }
    }
}
