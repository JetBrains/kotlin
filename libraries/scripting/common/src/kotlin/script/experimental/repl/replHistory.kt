/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.repl

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

const val REPL_CODE_LINE_FIRST_NO = 1
const val REPL_CODE_LINE_FIRST_GEN = 1

interface ReplSnippetId : Comparable<ReplSnippetId> {
    val no: Int
    val generation: Int
}

data class ReplHistoryRecord<out T>(val id: ReplSnippetId, val item: T)

interface IReplStageHistory<T> : List<ReplHistoryRecord<T>> {

    fun peek(): ReplHistoryRecord<T>? = lock.read { lastOrNull() }

    fun push(id: ReplSnippetId, item: T)

    fun pop(): ReplHistoryRecord<T>?

    fun verifiedPop(id: ReplSnippetId): ReplHistoryRecord<T>? = lock.write {
        if (lastOrNull()?.id == id) pop()
        else null
    }

    fun reset(): Iterable<ReplSnippetId>

    fun resetTo(id: ReplSnippetId): Iterable<ReplSnippetId>

    val lock: ReentrantReadWriteLock
}

interface ReplStageState<T> {
    val history: IReplStageHistory<T>

    val lock: ReentrantReadWriteLock

    val currentGeneration: Int

    fun getNextLineNo(): Int =
        history.peek()?.id?.no?.let { it + 1 } ?: REPL_CODE_LINE_FIRST_NO // TODO: it should be more robust downstream (e.g. use atomic)

    fun <StateT : ReplStageState<*>> asState(target: Class<out StateT>): StateT =
        if (target.isAssignableFrom(this::class.java)) this as StateT
        else throw IllegalArgumentException("$this is not an expected instance of IReplStageState")
}
