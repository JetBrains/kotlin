// WITH_RUNTIME
// PROBLEM: none

interface Foo
interface Bar : Foo

fun instanceOfMarkerInterface(x: List<Bar>): List<Bar> = x.<caret>filter { it is Foo }