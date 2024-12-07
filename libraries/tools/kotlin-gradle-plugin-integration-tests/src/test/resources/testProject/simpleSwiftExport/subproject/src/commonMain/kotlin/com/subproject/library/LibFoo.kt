package com.subproject.library

class LibFoo(val param1: String, val param2: Int) {

    fun instanceFun(param: String): String {
        return "param: $param, param1: $param1, param2: $param2"
    }
}