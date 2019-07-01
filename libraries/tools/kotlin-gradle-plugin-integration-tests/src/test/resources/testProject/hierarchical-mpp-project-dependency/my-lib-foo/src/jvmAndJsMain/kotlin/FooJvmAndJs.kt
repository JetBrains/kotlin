package com.example.foo

import com.example.thirdparty.thirdPartyFun

actual fun foo() = fooJvmAndJs()

fun fooJvmAndJs(): String {
    thirdPartyFun()
    return fooCommon()
}