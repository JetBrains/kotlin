/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

/**
 * Created by minamoto on 12/26/16.
 */

open class A(val a:Int) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other as? A == null) return false
        return (other as A).a == a // Where is smart casting?
    }

    companion object {
        val zero  = A(0)
        val one   =  A(1)
        val magic =  A(42)
    }
}

//     FUN public fun foo(a: defaults.A = ...): kotlin.Int
//       a: EXPRESSION_BODY
//         CALL '<get-magic>(): A' type=defaults.A origin=GET_PROPERTY
//           $this: GET_OBJECT 'companion object of A' type=defaults.A.Companion
//       BLOCK_BODY
//         RETURN type=kotlin.Nothing from='foo(A = ...): Int'
//           CALL '<get-a>(): Int' type=kotlin.Int origin=GET_PROPERTY
//             $this: GET_VAR 'value-parameter a: A = ...' type=defaults.A origin=null
fun foo(a: A = A.magic, b:Int = 0xdeadbeef.toInt()) = a.a

//     FUN public fun bar(a: defaults.A, inc: kotlin.Int = ...): defaults.A
//       inc: EXPRESSION_BODY
//         CONST Int type=kotlin.Int value='0'
//       BLOCK_BODY
//         RETURN type=kotlin.Nothing from='bar(A, Int = ...): A'
//           CALL 'constructor A(Int)' type=defaults.A origin=null
//             a: CALL 'plus(Int): Int' type=kotlin.Int origin=PLUS
//               $this: CALL '<get-a>(): Int' type=kotlin.Int origin=GET_PROPERTY
//                 $this: GET_VAR 'value-parameter a: A' type=defaults.A origin=null
//               other: GET_VAR 'value-parameter inc: Int = ...' type=kotlin.Int origin=null
fun bar(a:A, inc:Int = 0) = A(a.a + inc)


fun box(): String {

//  if: CALL 'NOT(Boolean): Boolean' type=kotlin.Boolean origin=EXCLEQ
//    arg0: CALL 'EQEQ(Any?, Any?): Boolean' type=kotlin.Boolean origin=EXCLEQ
//      arg0: CALL 'foo(A = ...): Int' type=kotlin.Int origin=null
//      arg1: CALL '<get-a>(): Int' type=kotlin.Int origin=GET_PROPERTY
//        $this: CALL '<get-magic>(): A' type=defaults.A origin=GET_PROPERTY
//          $this: GET_OBJECT 'companion object of A' type=defaults.A.Companion
    if (foo() != A.magic.a) {
        println("magic failed")
        throw Error()
    }

//  if: CALL 'NOT(Boolean): Boolean' type=kotlin.Boolean origin=EXCLEQ
//    arg0: CALL 'EQEQ(Any?, Any?): Boolean' type=kotlin.Boolean origin=EXCLEQ
//      arg0: CALL 'foo(A = ...): Int' type=kotlin.Int origin=null
//        a: CALL 'constructor A(Int)' type=defaults.A origin=null
//          a: CONST Int type=kotlin.Int value='1'
//      arg1: CONST Int type=kotlin.Int value='1'
    if (foo(A(1)) != 1) {
        println("one failed: foo(A(1))")
        throw Error()
    }

//  if: CALL 'NOT(Boolean): Boolean' type=kotlin.Boolean origin=EXCLEQ
//    arg0: CALL 'EQEQ(Any?, Any?): Boolean' type=kotlin.Boolean origin=EXCLEQ
//      arg0: CALL 'bar(A, Int = ...): A' type=defaults.A origin=null
//        a: CALL '<get-one>(): A' type=defaults.A origin=GET_PROPERTY <---
//          $this: GET_OBJECT 'companion object of A' type=defaults.A.Companion
//      arg1: CALL '<get-one>(): A' type=defaults.A origin=GET_PROPERTY
//        $this: GET_OBJECT 'companion object of A' type=defaults.A.Companion

    if (bar(A.one) != A.one) {
        println("A one failed")
        throw Error()
    }


//  if: CALL 'NOT(Boolean): Boolean' type=kotlin.Boolean origin=EXCLEQ
//    arg0: CALL 'EQEQ(Any?, Any?): Boolean' type=kotlin.Boolean origin=EXCLEQ
//      arg0: CALL '<get-a>(): Int' type=kotlin.Int origin=GET_PROPERTY
//        $this: CALL 'bar(A, Int = ...): A' type=defaults.A origin=null
//          a: CALL '<get-one>(): A' type=defaults.A origin=GET_PROPERTY
//            $this: GET_OBJECT 'companion object of A' type=defaults.A.Companion
//          inc: CONST Int type=kotlin.Int value='1'
//      arg1: CONST Int type=kotlin.Int value='2'
    if (bar(A.one, 1).a != 2) {
        println("A one + 1 failed")
        throw Error()
    }
    return "OK"
}
