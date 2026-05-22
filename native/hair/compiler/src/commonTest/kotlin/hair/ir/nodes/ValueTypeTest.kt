package hair.ir.nodes

import hair.ir.*
import hair.ir.IrTest
import hair.ir.nodes.Node
import hair.sym.ArithmeticType
import hair.sym.CmpOp
import hair.sym.Field
import hair.sym.Global
import hair.sym.HairClass
import hair.sym.HairType
import hair.sym.HairType.*
import hair.test.Cls
import hair.test.Fun
import kotlin.test.Test
import kotlin.test.assertEquals

private data class TestField(override val owner: HairClass, override val type: HairType) : Field
private data class TestGlobal(override val type: HairType) : Global

class ValueTypeTest : IrTest {

    private inline fun <reified T : ValueNode> Session.expect(expected: HairType, crossinline body: context(NodeBuilder, ArgsUpdater, ControlFlowBuilder) () -> Any) {
        var captured: ValueNode? = null
        buildInitialIR {
            val v = body()
            captured = (v as? ValueNode) ?: error("Builder did not return a ValueNode: $v")
            ReturnVoid()
        }
        assertEquals(expected, captured!!.valueType, "valueType mismatch for ${T::class.simpleName}")
    }

    @Test
    fun constants() = withTestSession {
        expect<ConstI>(INT) { ConstI(0) }
        expect<ConstL>(LONG) { ConstL(0L) }
        expect<ConstF>(FLOAT) { ConstF(0f) }
        expect<ConstD>(DOUBLE) { ConstD(0.0) }
        expect<Null>(REFERENCE) { Null() }
    }

    @Test
    fun arithBinaryOps() = withTestSession {
        // All arithmetic widths
        expect<Add>(INT) { Add(ArithmeticType.INT)(Param(0), ConstI(1)) }
        expect<Add>(LONG) { Add(ArithmeticType.LONG)(Param(0), ConstL(1L)) }
        expect<Add>(FLOAT) { Add(ArithmeticType.FLOAT)(Param(0), ConstF(1f)) }
        expect<Add>(DOUBLE) { Add(ArithmeticType.DOUBLE)(Param(0), ConstD(1.0)) }
        // Sample of the rest of the ArithBinaryOp family.
        expect<Sub>(INT) { Sub(ArithmeticType.INT)(Param(0), ConstI(1)) }
        expect<Mul>(LONG) { Mul(ArithmeticType.LONG)(Param(0), ConstL(1L)) }
        expect<Div>(FLOAT) { Div(ArithmeticType.FLOAT)(Param(0), ConstF(1f)) }
        expect<And>(INT) { And(ArithmeticType.INT)(Param(0), ConstI(1)) }
        expect<Shl>(INT) { Shl(ArithmeticType.INT)(Param(0), ConstI(1)) }
    }

    @Test
    fun cmp() = withTestSession {
        // Result is always INT (boolean-as-int), regardless of operand type.
        expect<Cmp>(INT) { Cmp(INT, CmpOp.EQ)(Param(0), ConstI(0)) }
        expect<Cmp>(INT) { Cmp(REFERENCE, CmpOp.EQ)(Param(0), Null()) }
    }

    @Test
    fun casts() = withTestSession {
        expect<SignExtend>(LONG) { SignExtend(LONG)(Param(0)) }
        expect<ZeroExtend>(LONG) { ZeroExtend(LONG)(Param(0)) }
        expect<Truncate>(INT) { Truncate(INT)(Param(0)) }
        expect<Reinterpret>(FLOAT) { Reinterpret(FLOAT)(Param(0)) }
    }

    @Test
    fun loads() = withTestSession {
        expect<Load>(INT) { Load(INT)(Param(0)) }
        expect<Load>(REFERENCE) { Load(REFERENCE)(Param(0)) }

        val f = TestField(Cls("C"), LONG)
        expect<LoadField>(LONG) { LoadField(f)(Param(0)) }

        val g = TestGlobal(FLOAT)
        expect<LoadGlobal>(FLOAT) { LoadGlobal(g) }
    }

    @Test
    fun newOps() = withTestSession {
        expect<New>(REFERENCE) { New(Cls("C")) }
        expect<NewArray>(REFERENCE) { NewArray(Cls("C"))(ConstI(4)) }
    }

    @Test
    fun typeChecks() = withTestSession {
        expect<IsInstanceOf>(INT) { IsInstanceOf(Cls("C"))(Param(0)) }
        expect<CheckCast>(REFERENCE) { CheckCast(Cls("C"))(Param(0)) }
    }

    @Test
    fun typeInfo() = withTestSession {
        expect<TypeInfo>(REFERENCE) { TypeInfo(Param(0)) }
        expect<ConstTypeInfo>(REFERENCE) { ConstTypeInfo(Cls("C")) }
    }

    @Test
    fun calls() = withTestSession {
        // Fun.resultHairType is INT in the test helper.
        expect<InvokeStatic>(INT) { InvokeStatic(Fun("f"))(callArgs = arrayOf()) }
    }

    @Test
    fun unitValue() = withTestSession {
        expect<UnitValue>(REFERENCE) { UnitValue() }
    }

    @Test
    fun phi() = withTestSession {
        var captured: Phi? = null
        buildInitialIR {
            branch(
                Param(0),
                { /* takes value 1 */ },
                { /* takes value 2 */ }
            )
            val mergeBlock = contextOf<ControlFlowBuilder>().lastControl as BlockEntry
            // Use distinct inputs so the Phi isn't folded away by normalization.
            val inputs: Array<Node?> = arrayOf(ConstL(1L), ConstL(2L))
            captured = Phi(LONG)(mergeBlock, *inputs) as? Phi
                ?: error("Expected Phi from builder")
            ReturnVoid()
        }
        assertEquals(LONG, captured!!.valueType)
    }
}
