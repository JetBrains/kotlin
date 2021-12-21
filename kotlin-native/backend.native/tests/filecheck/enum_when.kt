/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

enum class COLOR {
    RED,
    GREEN,
    BLUE
}

// CHECK-LABEL: "kfun:#main(){}"
fun main() {
    for (i in COLOR.values()) {
        // CHECK: = call i32 @"kfun:kotlin.Enum#<get-ordinal>(){}kotlin.Int"(
        print(when (i) {
            // we can't check the register is same, because it can be saved on stack and loaded back
            // CHECK: icmp eq i32 %{{[0-9a-z]*}}, 0
            COLOR.RED -> 0xff0000
            // CHECK: icmp eq i32 %{{[0-9a-z]*}}, 1
            COLOR.GREEN -> 0x00ff00
            // CHECK: icmp eq i32 %{{[0-9a-z]*}}, 2
            COLOR.BLUE -> 0x0000ff
        })
    }
    // CHECK-LABEL: epilogue:
}