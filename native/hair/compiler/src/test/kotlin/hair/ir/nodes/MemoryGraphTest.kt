package hair.ir.nodes

import hair.ir.*
import hair.logic.Trilean
import hair.opt.*
import hair.sym.Class
import hair.sym.Field
import hair.sym.Global
import hair.sym.Type
import kotlin.test.*
import hair.utils.printGraphviz

class MemoryGraphTest : IrTest {
    // TODO test on phi(escape)


    // TODO create proper sym definitions for tests
    companion object {
        class TestClass(val name: String) : Class {
            override fun toString(): String = name
        }
        val cls = TestClass("TestClass")
        val refType = object : Type.Reference {}
        val intType = Type.Primitive.INT
        class TestField(val name: String) : Field {
            override fun toString(): String = name
            override val owner: Class get() = TODO("Not yet implemented")
            override val type: Type get() = intType
        }
        class TestGlobal(val name: String) : Global {
            override fun toString(): String = name
            override val type: Type get() = refType
        }
        val field1 = TestField("field1")
        //val field2 = TestField("field2")
        val global = TestGlobal("global")

        ///
        fun memoryChain(access: MemoryAccess) = generateSequence<Node>(access) {
            (it as? MemoryAccess)?.lastLocationAccess
        }
    }

    @Test
    fun testByFieldFactorization() = withTestSession {

        buildInitialIR {
            val objMemoryChain = mutableListOf<Node>(entryBlock)
            val otherMemoryChain = mutableListOf<Node>(entryBlock)

            val value1 = Param(1)
            val value2 = Param(2)
            val value3 = Param(3)
            val value4 = Param(4)

            val obj = New(cls)()
            objMemoryChain += obj
            val altObj1 = Param(37)
            val altObj2 = Param(42)

            objMemoryChain += WriteField(field1)(obj, value1)
            otherMemoryChain += WriteField(field1)(altObj1, value2)

            Use(ReadFieldPinned(field1)(obj).also {
                objMemoryChain += it
            })

            otherMemoryChain += WriteField(field1)(altObj2, value3)

            val wGlobal = WriteGlobal(global)(obj)

            val readAfterEscape = ReadFieldPinned(field1)(obj)
            Use(readAfterEscape)

            WriteField(field1)(altObj2, value4)

            Use(ReadFieldPinned(field1)(obj))

            Halt()
            ///

            buildEscapeGraph()
            buildMemoryGraph(SimpleAliasAnalysis())

            printGraphviz()

            assertEquals(entryBlock, wGlobal.lastLocationAccess)
            val objChainEnd = objMemoryChain.last() as MemoryAccess
            assertContentEquals(objMemoryChain.reversed(), memoryChain(objChainEnd).toList())
            val otherChainEnd = otherMemoryChain.last() as MemoryAccess
            assertContentEquals(otherMemoryChain.reversed(), memoryChain(otherChainEnd).toList())
            assertEquals(
                setOf(objChainEnd, otherChainEnd),
                (readAfterEscape.lastLocationAccess as IndistinctMemory).inputs.toSet()
            )
        }
    }

    @Test
    fun testABAB() = withTestSession {
        buildInitialIR {
            val a = New(cls)()
            val b = New(cls)()
            // FIXME will be eliminated after memory optimizations
            val opA1 = ReadFieldPinned(field1)(a)
            val opB1 = ReadFieldPinned(field1)(b)
            val opA2 = ReadFieldPinned(field1)(a)
            val opB2 = ReadFieldPinned(field1)(b)

            Halt()

            buildEscapeGraph() // FIXME redundant
            buildMemoryGraph(SimpleAliasAnalysis())

            printGraphviz()

            assertContentEquals(listOf(opA2, opA1, a, entryBlock), memoryChain(opA2).toList())
            assertContentEquals(listOf(opB2, opB1, b, entryBlock), memoryChain(opB2).toList())
        }
    }

    @Test
    fun testConditionalAlias() = withTestSession {
        buildInitialIR {
            val a = Param(0)
            val b = Param(1)
            val c = Param(2)

            lateinit var bNotC: Node
            lateinit var cNotB: Node
            val ra = ReadFieldPinned(field1)(a)
            val rb = ReadFieldPinned(field1)(b)
            val rc = ReadFieldPinned(field1)(c)

            lateinit var rb2: ReadFieldPinned
            lateinit var rc2: ReadFieldPinned
            lateinit var ra2: ReadFieldPinned

            lateinit var ra3: ReadFieldPinned

            val cond = Param(42) // FIXME actually (b != c)
            val if_ = branch(cond, {
                // FIXME insert automatically??
                bNotC = NeqFilter(lastControl!!, b, c)
                cNotB = NeqFilter(lastControl!!, c, b)
                rb2 = ReadFieldPinned(field1)(bNotC)
                rc2 = ReadFieldPinned(field1)(cNotB)
                ra2 = ReadFieldPinned(field1)(a)
            }, {
                ReadFieldPinned(field1)(b)
                ReadFieldPinned(field1)(c)
                ra3 = ReadFieldPinned(field1)(a)
            })

            val rb4 = ReadFieldPinned(field1)(b)
            ReadFieldPinned(field1)(c)

            Halt()
            ///
            val aliases = object : AliasAnalysis {
                val base = SimpleAliasAnalysis()
                override fun aliases(lhs: Node, rhs: Node): Trilean {
                    if (lhs == bNotC && rhs in listOf(c, cNotB)) return Trilean.NO
                    if (lhs == cNotB && rhs in listOf(b, bNotC)) return Trilean.NO
                    if (lhs == c && rhs == bNotC) return Trilean.NO
                    if (lhs == b && rhs == cNotB) return Trilean.NO
                    return base.aliases(lhs, rhs)
                }
            }


            buildEscapeGraph() // FIXME redundant
            buildMemoryGraph(aliases)

            printGraphviz()

            assertEquals(rb, rb2.lastLocationAccess)
            assertEquals(rc, rc2.lastLocationAccess)
            assertEquals(setOf(rb2, rc2), (ra2.lastLocationAccess as IndistinctMemory).inputs.toSet())
            assertEquals(setOf(ra2, ra3), (rb4.lastLocationAccess as Phi).joinedValues.toSet())
        }
    }
}