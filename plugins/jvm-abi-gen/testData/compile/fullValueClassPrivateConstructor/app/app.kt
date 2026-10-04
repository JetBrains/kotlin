package app

import lib.*

fun sum(point: Point) = point.first + point.second

fun runAppAndReturnOk(): String = if (sum(Point.of(1)) == 3 && Empty.toString() == "Empty") "OK" else "Fail"
