/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

value class C(val x: Any)
val c = C(42)

fun main() {
    println(c.x)
}
// CHECK: {{call|invoke}} %struct.ObjHeader* @"kfun:#<C-unbox>

// Note: <C-unbox> is called from IR of generated methods like
// FUN BRIDGE_METHOD(target=FUN GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER name:equals
// FUN BRIDGE_METHOD(target=FUN GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER name:hashCode
// FUN BRIDGE_METHOD(target=FUN GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER name:toString
