package com.example.app

import com.example.bar.barCommon
import com.example.bar.barLinuxAndJs
import com.example.foo.fooCommon
import com.example.foo.fooLinuxAndJs

fun testAppLinuxAndJs() {
    fooCommon()
    fooLinuxAndJs()
    // fooJvmAndJs() // unresolved

    barCommon()
    barLinuxAndJs()
    // barJvmAndJs() // unresolved

    // thirdPartyFun() // unresolved

    appLinuxAndJs()
}