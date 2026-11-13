package app

import lib.Lib

fun names(): List<String> = listOf(Lib.name(), Lib().baseName())
