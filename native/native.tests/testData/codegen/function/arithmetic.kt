/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

fun square(a:Int):Int = a * a
fun sumOfSquares(a:Int, b:Int):Int = square(a) + square(b)
fun diffOfSquares(a:Int, b:Int):Int = square(a) - square(b)
fun mod(a:Int,b:Int):Int = a / b
fun remainder(a:Int, b:Int):Int = a % b

fun box(): String {
    if (square(2)             != 4)   throw Error()
    if (sumOfSquares(2, 4)    != 20)  throw Error()
    if (diffOfSquares(2, 4)   != -12) throw Error()
    if (mod(5, 2)             != 2)   throw Error()
    if (remainder(5, 2)       != 1)   throw Error()

    return "OK"
}