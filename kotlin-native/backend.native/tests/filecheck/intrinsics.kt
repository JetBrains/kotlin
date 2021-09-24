/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK-LABEL: "kfun:#and(kotlin.Int;kotlin.Int){}kotlin.Int"
fun and(a: Int, b: Int): Int {
    // CHECK: and {{.*}}, {{.*}}
    return a and b
// CHECK-LABEL: epilogue
}

// CHECK-LABEL: "kfun:#ieee754(kotlin.Float;kotlin.Float){}kotlin.Boolean"
fun ieee754(a: Float, b: Float): Boolean {
    // CHECK: fcmp oeq float {{.*}}, {{.*}}
    return a == b
// CHECK-LABEL: epilogue
}

// CHECK-LABEL: "kfun:#main(){}"
fun main() {
    val x = and(1, 2)
    val y = ieee754(0.0f, 1.0f)
// CHECK-LABEL: epilogue
}
