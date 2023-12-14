/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun eqeqB  (a:Byte,   b:Byte  ) = a == b
fun eqeqS  (a:Short,  b:Short ) = a == b
fun eqeqI  (a:Int,    b:Int   ) = a == b
fun eqeqL  (a:Long,   b:Long  ) = a == b
fun eqeqF  (a:Float,  b:Float ) = a == b
fun eqeqD  (a:Double, b:Double) = a == b
fun eqeqStr(a:String, b:String) = a == b

fun eqeqN  (a: Nothing, b: Nothing) = a == b
fun eqeqNN (a: Nothing?, b: Nothing?) = a == b

fun eqeqeq (a: Any?, b: Any?) = a === b

fun gtI  (a:Int,    b:Int   ) = a >  b
fun ltI  (a:Int,    b:Int   ) = a <  b
fun geI  (a:Int,    b:Int   ) = a >= b
fun leI  (a:Int,    b:Int   ) = a <= b
fun neI  (a:Int,    b:Int   ) = a != b

fun gtF  (a:Float,  b:Float ) = a >  b
fun ltF  (a:Float,  b:Float ) = a <  b
fun geF  (a:Float,  b:Float ) = a >= b
fun leF  (a:Float,  b:Float ) = a <= b
fun neF  (a:Float,  b:Float ) = a != b

fun helloString()   =  "Hello"
fun goodbyeString() =  "Goodbye"

fun box(): String {
    if (!eqeqB(3   , 3   )) throw Error()
    if (!eqeqS(3   , 3   )) throw Error()
    if (!eqeqI(3   , 3   )) throw Error()
    if (!eqeqL(3L , 3L   )) throw Error()
    if (!eqeqF(3.0f, 3.0f)) throw Error()
    if (!eqeqD(3.0 , 3.0 )) throw Error()

    if (!eqeqStr(helloString(), helloString()))   throw Error()

    if (!eqeqNN(null, null)) throw Error()

    if (!eqeqeq(helloString(),  helloString()))   throw Error()
    if (eqeqeq(helloString(),   goodbyeString())) throw Error()

    if (gtI   (2   , 3   )) throw Error()
    if (ltI   (3   , 2   )) throw Error()
    if (geI   (2   , 3   )) throw Error()
    if (leI   (3   , 2   )) throw Error()
    if (neI   (2   , 2   )) throw Error()

    if (gtF   (2.0f , 3.0f )) throw Error()
    if (ltF   (3.0f , 2.0f )) throw Error()
    if (geF   (2.0f , 3.0f )) throw Error()
    if (leF   (3.0f , 2.0f )) throw Error()
    if (neF   (2.0f , 2.0f )) throw Error()

    return "OK"
}