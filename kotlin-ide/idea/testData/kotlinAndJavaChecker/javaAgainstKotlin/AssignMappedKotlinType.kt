package test

import java.util.ArrayList

fun getInt(): Int = 12
fun getString(): String = "Test"
fun getNumber(): Number? = null

fun getList(): List<Int> = listOf(1, 2, 3)
fun getMutableList(): MutableList<Int> = ArrayList<Int>()
fun getArrayList(): ArrayList<String> = ArrayList<String>()