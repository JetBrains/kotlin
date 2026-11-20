// ISSUE: KT-89995
// The app doesn't enable FullValueClasses, so it has to skip the pre-release check of the library.
// Remove this test when FullValueClasses is enabled by default.
package app

import lib.*

fun sum(point: Point) = point.first + point.second

fun runAppAndReturnOk(): String = if (sum(Point.of(1)) == 3 && Empty.toString() == "Empty") "OK" else "Fail"
