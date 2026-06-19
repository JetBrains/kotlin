package hair.ir.opt

import hair.ir.*
import hair.ir.Add
import hair.ir.nodes.*
import hair.sym.ArithmeticType.*
import hair.test.Fun
import kotlin.test.*

class NormalizationTest : IrTest {

    // TODO Phi normalization

    @Test
    fun testConstAdd() = withTestSession {
        buildInitialIR {
            val a = 23
            val b = 42
            assertEquals(Const(a + b), Add(INT)(Const(a), Const(b)))
            ReturnVoid()
        }
    }

    @Test
    fun testConstTree() = withTestSession {
        buildInitialIR {
            val a = 4
            val b = 8
            val c = 15
            val d = 16
            val e = 23
            val f = 42
            assertEquals(
                Const(a + b + c + d + e + f),
                Add(INT)(
                    Add(INT)(Const(a), Const(b)),
                    Add(INT)(
                        Add(INT)(Const(c), Const(d)),
                        Add(INT)(Const(e), Const(f))
                    )
                )
            )
            ReturnVoid()
        }
    }

    @Test
    fun testAssociative() = withTestSession {
        buildInitialIR {
            val a = 23
            val b = 42
            assertEquals(
                Add(INT)(Add(INT)(Param(1), Const(a)), Const(b)),
                Add(INT)(Param(1), Const(a + b))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeSub() = withTestSession {
        buildInitialIR {
            val a = 100
            val b = 23
            val c = 15
            assertEquals(
                Const(a - b - c),
                Sub(INT)(Sub(INT)(Const(a), Const(b)), Const(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testSubSelf() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(Const(0), Sub(INT)(a, a))
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeMul() = withTestSession {
        buildInitialIR {
            val a = 4
            val b = 8
            val c = 15
            val d = 16
            assertEquals(
                Const(a * b * c * d),
                Mul(INT)(
                    Mul(INT)(Const(a), Const(b)),
                    Mul(INT)(Const(c), Const(d))
                )
            )
            ReturnVoid()
        }
    }

    @Test
    fun testMulAbsorption() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(Const(0), Mul(INT)(a, Const(0)))
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeDiv() = withTestSession {
        buildInitialIR {
            val a = 360
            val b = 4
            val c = 5
            assertEquals(
                Const(a / b / c),
                Div(INT)(Div(INT)(Const(a), Const(b)), Const(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testDivSelf() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(Const(1), Div(INT)(a, a))
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeRem() = withTestSession {
        buildInitialIR {
            val a = 23
            val b = 8
            assertEquals(Const(a % b), Rem(INT)(Const(a), Const(b)))
            ReturnVoid()
        }
    }

    @Test
    fun testRemSelf() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(Const(0), Rem(INT)(a, a))
            ReturnVoid()
        }
    }

    @Test
    fun testConstNeg() = withTestSession {
        buildInitialIR {
            assertEquals(Const(-42), Neg(Const(42)))
            ReturnVoid()
        }
    }

    @Test
    fun testDoubleNeg() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(a, Neg(Neg(a)))
            ReturnVoid()
        }
    }

    @Test
    fun testConstNot() = withTestSession {
        buildInitialIR {
            assertEquals(Const(42.inv()), Inv(Const(42)))
            ReturnVoid()
        }
    }

    @Test
    fun testNotZero() = withTestSession {
        buildInitialIR {
            assertEquals(Const(-1), Inv(Const(0)))
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeAnd() = withTestSession {
        buildInitialIR {
            val a = 0b11110000
            val b = 0b11001100
            val c = 0b10101010
            assertEquals(
                Const(a and b and c),
                And(INT)(And(INT)(Const(a), Const(b)), Const(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testAndAbsorber() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(Const(0), And(INT)(a, Const(0)))
            ReturnVoid()
        }
    }

    @Test
    fun testAndIdentity() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(a, And(INT)(a, Const(-1)))
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeOr() = withTestSession {
        buildInitialIR {
            val a = 0b00000001
            val b = 0b00000010
            val c = 0b00000100
            assertEquals(
                Const(a or b or c),
                Or(INT)(Or(INT)(Const(a), Const(b)), Const(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testOrAbsorber() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(Const(-1), Or(INT)(a, Const(-1)))
            ReturnVoid()
        }
    }

    @Test
    fun testOrIdentity() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(a, Or(INT)(a, Const(0)))
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeXor() = withTestSession {
        buildInitialIR {
            val a = 0b11110000
            val b = 0b10101010
            val c = 0b11001100
            assertEquals(
                Const(a xor b xor c),
                Xor(INT)(Xor(INT)(Const(a), Const(b)), Const(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testXorSelf() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(Const(0), Xor(INT)(a, a))
            ReturnVoid()
        }
    }

    @Test
    fun testXorIdentity() = withTestSession {
        buildInitialIR {
            val a = Const(42)
            assertEquals(a, Xor(INT)(a, Const(0)))
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeShl() = withTestSession {
        buildInitialIR {
            val a = 1
            val b = 4
            val c = 2
            assertEquals(
                Const((a shl b) shl c),
                Shl(INT)(Shl(INT)(Const(a), Const(b)), Const(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeShr() = withTestSession {
        buildInitialIR {
            val a = -256
            val b = 2
            val c = 1
            assertEquals(
                Const((a shr b) shr c),
                Shr(INT)(Shr(INT)(Const(a), Const(b)), Const(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeUshr() = withTestSession {
        buildInitialIR {
            val a = -256
            val b = 2
            val c = 1
            assertEquals(
                Const((a ushr b) ushr c),
                Ushr(INT)(Ushr(INT)(Const(a), Const(b)), Const(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testShrVsUshrSignBit() = withTestSession {
        buildInitialIR {
            val a = -1
            val shift = 1
            assertNotEquals(
                Shr(INT)(Const(a), Const(shift)),
                Ushr(INT)(Const(a), Const(shift))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testPackBytes() = withTestSession {
        buildInitialIR {
            val high = 0x12
            val low  = 0x34
            assertEquals(
                Const(0x1234),
                Or(INT)(
                    Shl(INT)(Const(high), Const(8)),
                    Const(low)
                )
            )
            ReturnVoid()
        }
    }

    @Test
    fun testRoundTripPackUnpack() = withTestSession {
        buildInitialIR {
            val high = 0x12
            val low  = 0x34
            val packed = Or(INT)(Shl(INT)(Const(high), Const(8)), Const(low))

            assertEquals(Const(high), And(INT)(Shr(INT)(packed, Const(8)), Const(0xFF)))
            assertEquals(Const(low),  And(INT)(packed, Const(0xFF)))
            ReturnVoid()
        }
    }

    @Test
    fun testSetBit() = withTestSession {
        buildInitialIR {
            val value = 0b10100000
            val bit   = 3
            assertEquals(
                Const(0b10101000),
                Or(INT)(Const(value), Shl(INT)(Const(1), Const(bit)))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testClearBit() = withTestSession {
        buildInitialIR {
            val value = 0b10101010
            val bit   = 3
            assertEquals(
                Const(0b10100010),
                And(INT)(Const(value), Inv(Shl(INT)(Const(1), Const(bit))))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testToggleBit() = withTestSession {
        buildInitialIR {
            val value = 0b10101010
            val bit   = 3
            assertEquals(
                Const(0b10100010),
                Xor(INT)(Const(value), Shl(INT)(Const(1), Const(bit)))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testExtractBitField() = withTestSession {
        buildInitialIR {
            assertEquals(
                Const(0xB),
                And(INT)(Shr(INT)(Const(0xABCD), Const(8)), Const(0xF))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testSignMask() = withTestSession {
        buildInitialIR {
            assertEquals(Const(-1), Shr(INT)(Const(Int.MIN_VALUE), Const(31)))
            assertEquals(Const(0),  Shr(INT)(Const(42),            Const(31)))
            ReturnVoid()
        }
    }

    @Test
    fun testNegAddIsSubReversed() = withTestSession {
        buildInitialIR {
            assertEquals(Const(3 - 10), Add(INT)(Neg(Const(10)), Const(3)))
            ReturnVoid()
        }
    }

    @Test
    fun testAddNegIsSub() = withTestSession {
        buildInitialIR {
            assertEquals(Const(10 - 3), Add(INT)(Const(10), Neg(Const(3))))
            ReturnVoid()
        }
    }

    @Test
    fun testSubNegIsAdd() = withTestSession {
        buildInitialIR {
            assertEquals(Const(10 + 3), Sub(INT)(Const(10), Neg(Const(3))))
            ReturnVoid()
        }
    }

    @Test
    fun testNegSubIsNegOfSum() = withTestSession {
        buildInitialIR {
            assertEquals(Const(-(10 + 3)), Sub(INT)(Neg(Const(10)), Const(3)))
            ReturnVoid()
        }
    }

    @Test
    fun testDoubleNegThroughAdd() = withTestSession {
        buildInitialIR {
            val a = 10; val b = 3
            assertEquals(
                Const(a - b),
                Neg(Add(INT)(Neg(Const(a)), Const(b)))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testProductOfSums() = withTestSession {
        buildInitialIR {
            val a = 4; val b = 8; val c = 3; val d = 5
            assertEquals(
                Const((a + b) * (c + d)),
                Mul(INT)(
                    Add(INT)(Const(a), Const(b)),
                    Add(INT)(Const(c), Const(d))
                )
            )
            ReturnVoid()
        }
    }

    @Test
    fun testDifferenceOfProducts() = withTestSession {
        buildInitialIR {
            val a = 15; val b = 4; val c = 8; val d = 3
            assertEquals(
                Const(a * b - c * d),
                Sub(INT)(
                    Mul(INT)(Const(a), Const(b)),
                    Mul(INT)(Const(c), Const(d))
                )
            )
            ReturnVoid()
        }
    }

    @Test
    fun testQuadraticExpression() = withTestSession {
        buildInitialIR {
            // 3*x^2 + 2*x + 1 at x=4 => 3*16 + 8 + 1 = 57
            val x = Const(4)
            assertEquals(
                Const(57),
                Add(INT)(
                    Add(INT)(
                        Mul(INT)(Const(3), Mul(INT)(x, x)),
                        Mul(INT)(Const(2), x)
                    ),
                    Const(1)
                )
            )
            ReturnVoid()
        }
    }

    @Test
    fun testNestedDivRem() = withTestSession {
        buildInitialIR {
            // (a / b) * b + (a % b) == a  (Euclidean division identity)
            val a = 23; val b = 5
            assertEquals(
                Const(a),
                Add(INT)(
                    Mul(INT)(Div(INT)(Const(a), Const(b)), Const(b)),
                    Rem(INT)(Const(a), Const(b))
                )
            )
            ReturnVoid()
        }
    }

// === Mixed Arithmetic and Bitwise ===

    @Test
    fun testArithmeticResultMasked() = withTestSession {
        buildInitialIR {
            val a = 200; val b = 137
            // (a + b) & 0xFF — wrap to byte
            assertEquals(
                Const((a + b) and 0xFF),
                And(INT)(Add(INT)(Const(a), Const(b)), Const(0xFF))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testScaledByShiftThenAdded() = withTestSession {
        buildInitialIR {
            val a = 5; val b = 3
            // (a << 4) + (b << 4) = (a + b) * 16
            assertEquals(
                Const((a shl 4) + (b shl 4)),
                Add(INT)(
                    Shl(INT)(Const(a), Const(4)),
                    Shl(INT)(Const(b), Const(4))
                )
            )
            ReturnVoid()
        }
    }

    @Test
    fun testBitwiseCombineAndShift() = withTestSession {
        buildInitialIR {
            val a = 0b1010; val b = 0b0101
            // (a | b) << 2
            assertEquals(
                Const((a or b) shl 2),
                Shl(INT)(Or(INT)(Const(a), Const(b)), Const(2))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testSignedUshrDifference() = withTestSession {
        buildInitialIR {
            // (x >> 1) - (x >>> 1): difference is only nonzero for negatives
            // For x = -2: shr gives -1, ushr gives MAX_VALUE, difference is very negative
            val x = -2
            assertEquals(
                Const((x shr 1) - (x ushr 1)),
                Sub(INT)(
                    Shr(INT)(Const(x), Const(1)),
                    Ushr(INT)(Const(x), Const(1))
                )
            )
            ReturnVoid()
        }
    }

// === Real-World Patterns ===

    @Test
    fun testGrayCodeEncode() = withTestSession {
        buildInitialIR {
            // Gray code: g = n ^ (n >> 1)
            val n = 6
            assertEquals(
                Const(n xor (n shr 1)),
                Xor(INT)(Const(n), Shr(INT)(Const(n), Const(1)))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testIsPowerOfTwoCheck() = withTestSession {
        buildInitialIR {
            // n & (n - 1) == 0 iff n is a power of two (for n > 0)
            val n = 16
            assertEquals(
                Const(0),
                And(INT)(Const(n), Sub(INT)(Const(n), Const(1)))
            )
            val notPow2 = 42
            assertNotEquals(
                Const(0),
                And(INT)(Const(notPow2), Sub(INT)(Const(notPow2), Const(1)))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testAbsoluteValuePositive() = withTestSession {
        buildInitialIR {
            // abs(x) via branchless: mask = x >> 31; (x + mask) ^ mask
            val x = 42
            val mask = Shr(INT)(Const(x), Const(31))
            assertEquals(
                Const(42),
                Xor(INT)(Add(INT)(Const(x), mask), mask)
            )
            ReturnVoid()
        }
    }

    @Test
    fun testAbsoluteValueNegative() = withTestSession {
        buildInitialIR {
            val x = -42
            val mask = Shr(INT)(Const(x), Const(31))
            assertEquals(
                Const(42),
                Xor(INT)(Add(INT)(Const(x), mask), mask)
            )
            ReturnVoid()
        }
    }


    @Test
    fun testAfterChange() = withTestSession {
        val a = 23
        val b = 42

        lateinit var use: Use
        lateinit var expected: Node

        buildInitialIR {
            use = Use(Add(INT)(Param(0), Const(a))) as Use
            expected = Const(a + b)
            Return(expected)
        }
        modifyIR {
            val add = use.value as Add
            assertTrue(add.rhs is Const) // make sure normalization has not messed things up
            add.lhs = Const(b)
        }
        assertEquals(expected, use.value)
    }


    @Ignore // FIXME decide how Catch should be implemented and normalized
    @Test
    fun testCatchOfThrow() = withTestSession {
        val value = 42

        buildInitialIR {
            val thr = Throw(Const(value)) as Throw
            val unwind = Unwind(thr)
            BlockEntry(unwind)
            val catch = Catch(unwind)
            Return(catch)
        }
        val ret = allNodes<Return>().single()
        assertEquals(value, (ret.result as Const).value)
    }

    context(cfb: ControlFlowBuilder)
    val lastControl get() = cfb.lastControl

//    @Test
//    fun testPhiFromUnreachable() = withTestSession {
//        lateinit var value1: Node
//        lateinit var value2: Node
//        buildInitialIR {
//            branch(
//                cond = Param(1010),
//                trueInit = {
//                    value1 = Const(42)
//                },
//                falseInit = {
//                    Throw(Const(0))
//                    BlockEntry()
//                    value2 = InvokeStatic(Fun("f"))()
//                })
//
//            Return(Phi(INT)(lastControl as BlockEntry, value1, value2))
//        }
//        allNodes().forEach { println(it) }
//        assertEquals(value1, allNodes<Return>().single().result)
//    }

}
