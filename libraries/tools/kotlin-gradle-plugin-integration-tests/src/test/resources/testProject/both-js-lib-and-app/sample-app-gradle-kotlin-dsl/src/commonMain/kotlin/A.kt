package com.example.app

import com.example.lib.expectedFun
import com.example.lib.id

fun idUsage(x: Int): Int = id(x)

fun main(args: Array<String>) {
    expectedFun()
}