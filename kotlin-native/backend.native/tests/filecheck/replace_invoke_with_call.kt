/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun flameThrower() {
    throw Throwable("🔥")
}

// CHECK-LABEL: "kfun:#f1(){}"
fun f1() {
    // CHECK: call void @"kfun:#flameThrower(){}"()
    flameThrower()
// CHECK-LABEL: epilogue
}

// CHECK-LABEL: "kfun:#f2(){}"
fun f2() {
    try {
        // CHECK: invoke void @"kfun:#flameThrower(){}"()
        flameThrower()
    } catch (t: Throwable) {}
// CHECK-LABEL: epilogue
}

// CHECK-LABEL: "kfun:#main(){}"
fun main() {
    f1()
    f2()
// CHECK-LABEL: epilogue
}