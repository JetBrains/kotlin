package org.jetbrains.eval4j.jdi.test

import com.sun.jdi
import junit.framework.TestSuite
import org.jetbrains.eval4j.test.buildTestSuite
import junit.framework.TestCase
import org.jetbrains.eval4j.interpreterLoop
import org.junit.Assert.*
import org.jetbrains.eval4j.jdi.makeInitialFrame
import org.jetbrains.eval4j.jdi.JDIEval
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.jetbrains.eval4j.ExceptionThrown
import org.jetbrains.eval4j.jdi.asJdiValue
import org.jetbrains.eval4j.MethodDescription

val DEBUGEE_CLASS = javaClass<Debugee>()

fun suite(): TestSuite {
    val connectors = jdi.Bootstrap.virtualMachineManager().launchingConnectors()
    val connector = connectors[0]
    println("Using connector $connector")

    val connectorArgs = connector.defaultArguments()

    val debugeeName = DEBUGEE_CLASS.getName()
    println("Debugee name: $debugeeName")
    connectorArgs["main"]!!.setValue(debugeeName)
    connectorArgs["options"]!!.setValue("-classpath out/production/eval4j:out/test/eval4j")
    val vm = connector.launch(connectorArgs)!!

    val req = vm.eventRequestManager().createClassPrepareRequest()
    req.addClassFilter("*.Debugee")
    req.enable()

    val latch = CountDownLatch(1)
    var classLoader : jdi.ClassLoaderReference? = null
    var thread : jdi.ThreadReference? = null

    Thread {
        val eventQueue = vm.eventQueue()
        @mainLoop while (true) {
            val eventSet = eventQueue.remove()
            for (event in eventSet.eventIterator()) {
                when (event) {
                    is jdi.event.ClassPrepareEvent -> {
                        val _class = event.referenceType()!!
                        if (_class.name() == debugeeName) {
                            for (l in _class.allLineLocations()) {
                                if (l.method().name() == "main") {
                                    classLoader = l.method().declaringType().classLoader()
                                    val breakpointRequest = vm.eventRequestManager().createBreakpointRequest(l)
                                    breakpointRequest.enable()
                                    println("Breakpoint: $breakpointRequest")
                                    vm.resume()
                                    break
                                }
                            }
                        }
                    }
                    is jdi.event.BreakpointEvent -> {
                        println("Suspended at: " + event.location())

                        thread = event.thread()
                        latch.countDown()

                        break @mainLoop
                    }
                    else -> {}
                }
            }
        }
    }.start()

    vm.resume()

    latch.await()

    var remainingTests = AtomicInteger(0)

    val suite = buildTestSuite {
        methodNode, ownerClass, expected ->
        remainingTests.incrementAndGet()
        object : TestCase("test" + methodNode.name.capitalize()) {

            override fun runTest() {
                val eval = JDIEval(
                        vm, classLoader!!, thread!!
                )
                val value = interpreterLoop(
                        methodNode,
                        makeInitialFrame(methodNode, listOf()),
                        eval
                )

                if (remainingTests.decrementAndGet() == 0) vm.resume()

                if (value is ExceptionThrown) {
                    val str = eval.invokeMethod(
                            value.exception,
                            MethodDescription(
                                    "java/lang/Object",
                                    "toString",
                                    "()Ljava/lang/String;",
                                    false
                            ),
                            listOf())
                    System.err.println("Exception: $str")
                }

                assertEquals(expected, value)
            }
        }
    }

    return suite
}