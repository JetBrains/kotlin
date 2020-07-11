package com.example.thirdparty

expect fun thirdPartyFun(): String

private fun useStdlibInCommonMain() = listOf(1, 2, 3).joinToString()