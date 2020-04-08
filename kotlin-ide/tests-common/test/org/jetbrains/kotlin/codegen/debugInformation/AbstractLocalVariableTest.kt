package org.jetbrains.kotlin.codegen.debugInformation

import com.sun.jdi.LocalVariable
import com.sun.jdi.StackFrame
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.Event
import com.sun.jdi.event.LocatableEvent
import junit.framework.TestCase
import org.junit.AfterClass
import org.junit.BeforeClass
import java.io.File

abstract class AbstractLocalVariableTest : AbstractDebugTest() {

    override val virtualMachine: VirtualMachine = Companion.virtualMachine
    override val proxyPort: Int = Companion.proxyPort

    companion object {
        const val LOCAL_VARIABLES = "// LOCAL VARIABLES"
        var proxyPort = 0
        lateinit var process: Process
        lateinit var virtualMachine: VirtualMachine

        @BeforeClass
        @JvmStatic
        fun setUpTest() {
            val (process, port) = startDebuggeeProcess()
            this.process = process
            virtualMachine = attachDebugger(port)
            setUpVM(virtualMachine)

            proxyPort = getProxyPort(process)
        }

        @AfterClass
        @JvmStatic
        fun tearDownTest() {
            process.destroy()
        }
    }

    override fun storeStep(loggedItems: ArrayList<Any>, event: Event) {
        waitUntil { (event as LocatableEvent).thread().isSuspended }
        val visibleVars = (event as LocatableEvent)
            .thread()
            .frame(0)
            .visibleVariables()
            .map { variable -> toRecord(event.thread().frame(0), variable) }
            .joinToString(", ")
        loggedItems.add("${event.location()}: $visibleVars".trim())
    }

    override fun checkResult(wholeFile: File, loggedItems: List<Any>) {
        val expectedLocalVariables = wholeFile
            .readLines()
            .dropWhile { !it.startsWith(LOCAL_VARIABLES) }
            .drop(1)
            .map { it.drop(3) }
            .joinToString("\n")
        val actualLocalVariables = loggedItems.joinToString("\n")

        TestCase.assertEquals(expectedLocalVariables, actualLocalVariables)
    }

    private fun toRecord(frame: StackFrame, variable: LocalVariable): String {
        return "${variable.name()}:${frame.getValue(variable)?.type()?.name() ?: "null"}"
    }

    private fun waitUntil(condition: () -> Boolean) {
        while (!condition()) {
            Thread.sleep(10)
        }
    }
}