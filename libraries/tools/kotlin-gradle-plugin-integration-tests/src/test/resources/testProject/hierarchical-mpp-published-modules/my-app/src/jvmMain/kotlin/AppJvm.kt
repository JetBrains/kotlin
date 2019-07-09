package com.example.app

import com.example.bar.barCommon
import com.example.bar.barJvmAndJs
import com.example.foo.fooCommon
import com.example.foo.fooJvmAndJs
import com.example.thirdparty.thirdPartyFun
import com.example.thirdparty.thirdPartyJvmFun

fun main() {
    thirdPartyFun()
    thirdPartyJvmFun()

    fooCommon()
    fooJvmAndJs()

    barCommon()
    barJvmAndJs()
}