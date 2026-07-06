package hair.ir.opt

import hair.ir.*
import hair.ir.Add
import hair.ir.nodes.*
import hair.sym.HairType
import hair.sym.HairType.*
import hair.test.Fun
import kotlin.test.*

class NormalizationTest : IrTest {

    // TODO Phi normalization

    @Test
    fun testConstAdd() = withTestSession {
        buildInitialIR {
            val a = 23
            val b = 42
            assertEquals(ConstI(a + b), Add(INT)(ConstI(a), ConstI(b)))
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
                ConstI(a + b + c + d + e + f),
                Add(INT)(
                    Add(INT)(ConstI(a), ConstI(b)),
                    Add(INT)(
                        Add(INT)(ConstI(c), ConstI(d)),
                        Add(INT)(ConstI(e), ConstI(f))
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
                Add(INT)(Add(INT)(Param(1), ConstI(a)), ConstI(b)),
                Add(INT)(Param(1), ConstI(a + b))
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
                ConstI(a - b - c),
                Sub(INT)(Sub(INT)(ConstI(a), ConstI(b)), ConstI(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testSubSelf() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(ConstI(0), Sub(INT)(a, a))
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
                ConstI(a * b * c * d),
                Mul(INT)(
                    Mul(INT)(ConstI(a), ConstI(b)),
                    Mul(INT)(ConstI(c), ConstI(d))
                )
            )
            ReturnVoid()
        }
    }

    @Test
    fun testMulAbsorption() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(ConstI(0), Mul(INT)(a, ConstI(0)))
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
                ConstI(a / b / c),
                Div(INT)(Div(INT)(ConstI(a), ConstI(b)), ConstI(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testDivSelf() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(ConstI(1), Div(INT)(a, a))
            ReturnVoid()
        }
    }

    @Test
    fun testConstTreeRem() = withTestSession {
        buildInitialIR {
            val a = 23
            val b = 8
            assertEquals(ConstI(a % b), Rem(INT)(ConstI(a), ConstI(b)))
            ReturnVoid()
        }
    }

    @Test
    fun testRemSelf() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(ConstI(0), Rem(INT)(a, a))
            ReturnVoid()
        }
    }

    @Test
    fun testConstNeg() = withTestSession {
        buildInitialIR {
            assertEquals(ConstI(-42), Neg(ConstI(42)))
            ReturnVoid()
        }
    }

    @Test
    fun testDoubleNeg() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(a, Neg(Neg(a)))
            ReturnVoid()
        }
    }

    @Test
    fun testConstNot() = withTestSession {
        buildInitialIR {
            assertEquals(ConstI(42.inv()), Inv(ConstI(42)))
            ReturnVoid()
        }
    }

    @Test
    fun testNotZero() = withTestSession {
        buildInitialIR {
            assertEquals(ConstI(-1), Inv(ConstI(0)))
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
                ConstI(a and b and c),
                And(INT)(And(INT)(ConstI(a), ConstI(b)), ConstI(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testAndAbsorber() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(ConstI(0), And(INT)(a, ConstI(0)))
            ReturnVoid()
        }
    }

    @Test
    fun testAndIdentity() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(a, And(INT)(a, ConstI(-1)))
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
                ConstI(a or b or c),
                Or(INT)(Or(INT)(ConstI(a), ConstI(b)), ConstI(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testOrAbsorber() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(ConstI(-1), Or(INT)(a, ConstI(-1)))
            ReturnVoid()
        }
    }

    @Test
    fun testOrIdentity() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(a, Or(INT)(a, ConstI(0)))
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
                ConstI(a xor b xor c),
                Xor(INT)(Xor(INT)(ConstI(a), ConstI(b)), ConstI(c))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testXorSelf() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(ConstI(0), Xor(INT)(a, a))
            ReturnVoid()
        }
    }

    @Test
    fun testXorIdentity() = withTestSession {
        buildInitialIR {
            val a = ConstI(42)
            assertEquals(a, Xor(INT)(a, ConstI(0)))
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
                ConstI((a shl b) shl c),
                Shl(INT)(Shl(INT)(ConstI(a), ConstI(b)), ConstI(c))
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
                ConstI((a shr b) shr c),
                Shr(INT)(Shr(INT)(ConstI(a), ConstI(b)), ConstI(c))
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
                ConstI((a ushr b) ushr c),
                Ushr(INT)(Ushr(INT)(ConstI(a), ConstI(b)), ConstI(c))
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
                Shr(INT)(ConstI(a), ConstI(shift)),
                Ushr(INT)(ConstI(a), ConstI(shift))
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
                ConstI(0x1234),
                Or(INT)(
                    Shl(INT)(ConstI(high), ConstI(8)),
                    ConstI(low)
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
            val packed = Or(INT)(Shl(INT)(ConstI(high), ConstI(8)), ConstI(low))

            assertEquals(ConstI(high), And(INT)(Shr(INT)(packed, ConstI(8)), ConstI(0xFF)))
            assertEquals(ConstI(low),  And(INT)(packed, ConstI(0xFF)))
            ReturnVoid()
        }
    }

    @Test
    fun testSetBit() = withTestSession {
        buildInitialIR {
            val value = 0b10100000
            val bit   = 3
            assertEquals(
                ConstI(0b10101000),
                Or(INT)(ConstI(value), Shl(INT)(ConstI(1), ConstI(bit)))
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
                ConstI(0b10100010),
                And(INT)(ConstI(value), Inv(Shl(INT)(ConstI(1), ConstI(bit))))
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
                ConstI(0b10100010),
                Xor(INT)(ConstI(value), Shl(INT)(ConstI(1), ConstI(bit)))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testExtractBitField() = withTestSession {
        buildInitialIR {
            assertEquals(
                ConstI(0xB),
                And(INT)(Shr(INT)(ConstI(0xABCD), ConstI(8)), ConstI(0xF))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testSignMask() = withTestSession {
        buildInitialIR {
            assertEquals(ConstI(-1), Shr(INT)(ConstI(Int.MIN_VALUE), ConstI(31)))
            assertEquals(ConstI(0),  Shr(INT)(ConstI(42),            ConstI(31)))
            ReturnVoid()
        }
    }

    @Test
    fun testNegAddIsSubReversed() = withTestSession {
        buildInitialIR {
            assertEquals(ConstI(3 - 10), Add(INT)(Neg(ConstI(10)), ConstI(3)))
            ReturnVoid()
        }
    }

    @Test
    fun testAddNegIsSub() = withTestSession {
        buildInitialIR {
            assertEquals(ConstI(10 - 3), Add(INT)(ConstI(10), Neg(ConstI(3))))
            ReturnVoid()
        }
    }

    @Test
    fun testSubNegIsAdd() = withTestSession {
        buildInitialIR {
            assertEquals(ConstI(10 + 3), Sub(INT)(ConstI(10), Neg(ConstI(3))))
            ReturnVoid()
        }
    }

    @Test
    fun testNegSubIsNegOfSum() = withTestSession {
        buildInitialIR {
            assertEquals(ConstI(-(10 + 3)), Sub(INT)(Neg(ConstI(10)), ConstI(3)))
            ReturnVoid()
        }
    }

    @Test
    fun testDoubleNegThroughAdd() = withTestSession {
        buildInitialIR {
            val a = 10; val b = 3
            assertEquals(
                ConstI(a - b),
                Neg(Add(INT)(Neg(ConstI(a)), ConstI(b)))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testProductOfSums() = withTestSession {
        buildInitialIR {
            val a = 4; val b = 8; val c = 3; val d = 5
            assertEquals(
                ConstI((a + b) * (c + d)),
                Mul(INT)(
                    Add(INT)(ConstI(a), ConstI(b)),
                    Add(INT)(ConstI(c), ConstI(d))
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
                ConstI(a * b - c * d),
                Sub(INT)(
                    Mul(INT)(ConstI(a), ConstI(b)),
                    Mul(INT)(ConstI(c), ConstI(d))
                )
            )
            ReturnVoid()
        }
    }

    @Test
    fun testQuadraticExpression() = withTestSession {
        buildInitialIR {
            // 3*x^2 + 2*x + 1 at x=4 => 3*16 + 8 + 1 = 57
            val x = ConstI(4)
            assertEquals(
                ConstI(57),
                Add(INT)(
                    Add(INT)(
                        Mul(INT)(ConstI(3), Mul(INT)(x, x)),
                        Mul(INT)(ConstI(2), x)
                    ),
                    ConstI(1)
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
                ConstI(a),
                Add(INT)(
                    Mul(INT)(Div(INT)(ConstI(a), ConstI(b)), ConstI(b)),
                    Rem(INT)(ConstI(a), ConstI(b))
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
                ConstI((a + b) and 0xFF),
                And(INT)(Add(INT)(ConstI(a), ConstI(b)), ConstI(0xFF))
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
                ConstI((a shl 4) + (b shl 4)),
                Add(INT)(
                    Shl(INT)(ConstI(a), ConstI(4)),
                    Shl(INT)(ConstI(b), ConstI(4))
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
                ConstI((a or b) shl 2),
                Shl(INT)(Or(INT)(ConstI(a), ConstI(b)), ConstI(2))
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
                ConstI((x shr 1) - (x ushr 1)),
                Sub(INT)(
                    Shr(INT)(ConstI(x), ConstI(1)),
                    Ushr(INT)(ConstI(x), ConstI(1))
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
                ConstI(n xor (n shr 1)),
                Xor(INT)(ConstI(n), Shr(INT)(ConstI(n), ConstI(1)))
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
                ConstI(0),
                And(INT)(ConstI(n), Sub(INT)(ConstI(n), ConstI(1)))
            )
            val notPow2 = 42
            assertNotEquals(
                ConstI(0),
                And(INT)(ConstI(notPow2), Sub(INT)(ConstI(notPow2), ConstI(1)))
            )
            ReturnVoid()
        }
    }

    @Test
    fun testAbsoluteValuePositive() = withTestSession {
        buildInitialIR {
            // abs(x) via branchless: mask = x >> 31; (x + mask) ^ mask
            val x = 42
            val mask = Shr(INT)(ConstI(x), ConstI(31))
            assertEquals(
                ConstI(42),
                Xor(INT)(Add(INT)(ConstI(x), mask), mask)
            )
            ReturnVoid()
        }
    }

    @Test
    fun testAbsoluteValueNegative() = withTestSession {
        buildInitialIR {
            val x = -42
            val mask = Shr(INT)(ConstI(x), ConstI(31))
            assertEquals(
                ConstI(42),
                Xor(INT)(Add(INT)(ConstI(x), mask), mask)
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
            use = Use(Add(INT)(Param(0), ConstI(a))) as Use
            expected = ConstI(a + b)
            Return(expected)
        }
        modifyIR {
            val add = use.value as Add
            assertTrue(add.rhs is ConstI) // make sure normalization has not messed things up
            add.lhs = ConstI(b)
        }
        assertEquals(expected, use.value)
    }


    @Ignore // FIXME decide how Catch should be implemented and normalized
    @Test
    fun testCatchOfThrow() = withTestSession {
        val value = 42

        buildInitialIR {
            val thr = Throw(ConstI(value)) as Throw
            val unwind = Unwind(thr)
            BlockEntry(unwind)
            val catch = Catch(unwind)
            Return(catch)
        }
        val ret = allNodes<Return>().single()
        assertEquals(value, (ret.result as ConstI).value)
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
//                    value1 = ConstI(42)
//                },
//                falseInit = {
//                    Throw(ConstI(0))
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
