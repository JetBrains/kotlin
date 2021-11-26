/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK-LABEL: "kfun:#id(kotlin.Any?){}kotlin.Any?"
fun id(a: Any?): Any? {
    return a
// CHECK-LABEL: epilogue
}

// CHECK-LABEL: "kfun:#main(){}"
fun main() {
    // CHECK: call %struct.ObjHeader* @"kfun:#id(kotlin.Any?){}kotlin.Any?"
    val x = id("Hello")
    // CHECK: call void @"kfun:kotlin.io#println(kotlin.Any?){}"(%struct.ObjHeader* {{.*}})
    println(x)
// CHECK-LABEL: epilogue
}