/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.exec

import com.intellij.debugger.streams.resolve.ResolvedStreamCall
import com.intellij.debugger.streams.resolve.ResolvedStreamChain
import com.intellij.debugger.streams.trace.*
import com.intellij.execution.ExecutionTestCase
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import one.util.streamex.StreamEx
import java.util.*

class StreamTraceChecker(private val testCase: ExecutionTestCase) {
    fun checkChain(trace: List<TraceInfo>) {
        for (info in trace) {
            val name = info.call.name
            println(name)

            print("    before: ")
            val before = info.valuesOrderBefore
            println(traceToString(before.values))

            print("    after: ")
            val after = info.valuesOrderAfter
            println(traceToString(after.values))
        }
    }

    fun checkResolvedChain(result: ResolvedTracingResult) {
        val resolvedChain = result.resolvedChain

        checkChain(resolvedChain)
        checkTracesIsCorrectInBothDirections(resolvedChain)

        val terminator = resolvedChain.terminator
        resolvedChain.intermediateCalls.forEach { x -> printBeforeAndAfterValues(x.stateBefore, x.stateAfter) }
        printBeforeAndAfterValues(terminator.stateBefore, terminator.stateAfter)
    }

    private fun printBeforeAndAfterValues(before: NextAwareState?, after: PrevAwareState?) {
        TestCase.assertFalse(before == null && after == null)
        val call = before?.nextCall ?: after!!.prevCall
        TestCase.assertNotNull(call)
        println("mappings for " + call!!.name)
        println("  direct:")
        if (before != null) {
            printMapping(before.trace, { before.getNextValues(it) }, Direction.FORWARD)
        } else {
            println("    no")
        }

        println("  reverse:")
        if (after != null) {
            printMapping(after.trace, { after.getPrevValues(it) }, Direction.BACKWARD)
        } else {
            println("    not found")
        }
    }

    private fun printMapping(
        values: List<TraceElement>,
        mapper: (TraceElement) -> List<TraceElement>,
        direction: Direction
    ) {
        if (values.isEmpty()) {
            println("    empty")
        }
        for (element in values) {
            val mappedValues = mapper(element)
            val mapped = traceToString(mappedValues)
            val line = if (Direction.FORWARD == direction) element.time.toString() + " -> " + mapped else mapped + " <- " + element.time
            println("    $line")
        }
    }

    private enum class Direction {
        FORWARD, BACKWARD
    }

    private fun checkChain(chain: ResolvedStreamChain) {
        val intermediates = chain.intermediateCalls
        val terminator = chain.terminator
        if (intermediates.isEmpty()) {
            TestCase.assertFalse(terminator.stateBefore is PrevAwareState)
        }

        checkIntermediates(chain.intermediateCalls)

        TestCase.assertEquals(terminator.call.name, terminator.stateBefore.nextCall.name)
        val after = terminator.stateAfter
        if (after != null) {
            val terminatorCall = after.prevCall
            TestCase.assertNotNull(terminatorCall)
            TestCase.assertEquals(terminator.call.name, terminatorCall!!.name)
        }

        if (!intermediates.isEmpty()) {
            val lastIntermediate = intermediates[intermediates.size - 1]
            val stateAfterIntermediates = lastIntermediate.stateAfter
            UsefulTestCase.assertInstanceOf(stateAfterIntermediates, NextAwareState::class.java)
            TestCase.assertEquals(terminator.call.name, (stateAfterIntermediates as NextAwareState).nextCall.name)
        }
    }

    private fun checkIntermediates(intermediates: List<ResolvedStreamCall.Intermediate>) {
        for (i in 0 until intermediates.size - 1) {
            val prev = intermediates[i]
            val next = intermediates[i + 1]
            TestCase.assertSame(prev.stateAfter, next.stateBefore)
            val prevCall = prev.stateAfter.prevCall
            TestCase.assertNotNull(prevCall)
            TestCase.assertEquals(prev.call.name, prevCall!!.name)
            TestCase.assertEquals(next.call.name, next.stateBefore.nextCall.name)
        }
    }

    private fun checkTracesIsCorrectInBothDirections(resolvedChain: ResolvedStreamChain) {
        for (intermediate in resolvedChain.intermediateCalls) {
            checkNeighborTraces(intermediate.stateBefore, intermediate.stateAfter)
        }

        val terminator = resolvedChain.terminator
        val after = terminator.stateAfter
        if (after != null) {
            checkNeighborTraces(terminator.stateBefore, after)
        }
    }

    private fun checkNeighborTraces(left: NextAwareState, right: PrevAwareState) {
        val leftValues = HashSet(left.trace)
        val rightValues = HashSet(right.trace)

        checkThatMappingsIsCorrect(
            leftValues,
            rightValues,
            { left.getNextValues(it) },
            { right.getPrevValues(it) })
        checkThatMappingsIsCorrect(
            rightValues,
            leftValues,
            { right.getPrevValues(it) },
            { left.getNextValues(it) })
    }

    private fun checkThatMappingsIsCorrect(
        prev: Set<TraceElement>,
        next: Set<TraceElement>,
        toNext: (TraceElement) -> List<TraceElement>,
        toPrev: (TraceElement) -> List<TraceElement>
    ) {
        for (leftElement in prev) {
            val mapToRight = toNext.invoke(leftElement)
            for (rightElement in mapToRight) {
                TestCase.assertTrue(next.contains(rightElement))
                TestCase.assertTrue(toPrev.invoke(rightElement).contains(leftElement))
            }
        }
    }

    private fun traceToString(trace: Collection<TraceElement>): String {
        return replaceIfEmpty(StreamEx.of(trace).map<Int>({ it.time }).sorted().joining(","))
    }

    private fun replaceIfEmpty(str: String): String {
        return if (str.isEmpty()) "nothing" else str
    }

    private fun println(msg: String) = testCase.println(msg, ProcessOutputTypes.SYSTEM)

    private fun print(msg: String) = testCase.print(msg, ProcessOutputTypes.SYSTEM)
}