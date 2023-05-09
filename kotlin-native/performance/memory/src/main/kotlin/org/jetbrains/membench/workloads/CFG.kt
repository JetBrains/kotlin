/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.membench

import org.jetbrains.benchmarksLauncher.Random

interface Node {
    fun next(): Iterable<Node>
    fun graphviz() {
        println("digraph G {")
        val visited = mutableSetOf<Node>()
        fun visit(n: Node) {
            if (n in visited) return
            println("    \"$n\"")
            visited += n
            n.next().forEach { println("    \"$n\" -> \"$it\"") }
            n.next().forEach { visit(it) }
        }
        visit(this)
        println("}")
    }
}
class Basic(var next: Node?) : Node {
    override fun next() = listOfNotNull(next)
}
class Branch(var left: Node, var right: Node) : Node {
    override fun next() = listOf(left, right)
}
class Switch() : Node {
    val next = mutableListOf<Node>()
    override fun next() = next
}

data class Hammock(val source: Node, val sink: Basic)

class HammockBuilder {
    private val source = Basic(null)
    private var sink = source

    private fun add(hammock: Hammock) = hammock.also {
        sink.next = it.source
        sink = it.sink
    }

    fun get() = Hammock(source, sink)

    fun basic(): Hammock {
        val n = Basic(null)
        return add(Hammock(n, n))
    }

    fun branch(left: HammockBuilder.() -> Unit, right: HammockBuilder.() -> Unit): Hammock {
        val merge = Basic(null)
        val l = HammockBuilder().apply {
            left()
            sink.next = merge
        }
        val r = HammockBuilder().apply {
            right()
            sink.next = merge
        }
        val b = Branch(l.source, r.source)
        return add(Hammock(b, merge))
    }

    fun switch(branches: Array<HammockBuilder.() -> Unit>): Hammock {
        val merge = Basic(null)
        val bs = branches.map {
            HammockBuilder().apply {
                it()
                sink.next = merge
            }
        }
        val s = Switch()
        s.next.addAll(bs.map { it.source })

        return add(Hammock(s, merge))
    }

    fun loop(body: HammockBuilder.() -> Unit): Hammock {
        val bb = HammockBuilder().apply {
            var bodySink: Basic? = null
            branch({
                body()
                bodySink = sink
            }, {})
            // back edge
            bodySink!!.next = source
        }
        return add(Hammock(bb.source, bb.sink))
    }

    fun random(p0: Int, pb: Int, ps: Int, pl: Int) {
        // FIXME names
        var p0v = p0
        var pbv = pb
        var psv = ps
        var plv = pl
        while (true) {
            val sum = p0v + pbv + psv + plv
            if (sum == 0) break
            val r = Random.nextInt(sum)
            when {
                r < p0v -> {
                    basic()
                    p0v -= 1
                }
                r < p0v + pbv -> {
                    branch({
                        random(p0v / 2, pbv / 2, psv / 2, plv / 2)
                    }, {
                        random(p0v / 2, pbv / 2, psv / 2, plv / 2)
                    })
                    pbv -= 1
                }
                r < p0v + pbv + psv -> {
                    val ar = Random.nextInt(16)
                    switch(Array(ar) {
                        {
                            random(p0v / 2, pbv / 2, 0, plv / 2)
                        }
                    })
                    psv -= 1
                }

                else -> {
                    loop {
                        random(p0v / 2, pbv / 2, psv / 4, plv / 2)
                    }
                    plv -= 1
                }
            }
        }
    }
}

class CFG(scale: Int) : Workload {
    // TODO use scale?
    private val pBasic = 100
    private val pBranch = 60
    private val pSwitch = 24
    private val pLoop = 60

    private val data = HammockBuilder().apply { random(pBasic, pBranch, pSwitch, pLoop) }.get()
//
//    init {
//        data.source.graphviz()
//    }

    companion object : WorkloadProvider<CFG> {
        override fun name(): String = "CFG"
        override fun allocate(scale: Int) = CFG(scale)
    }
}
