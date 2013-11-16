package demo
open class Test() {
class object {
open fun subListRangeCheck(fromIndex : Int, toIndex : Int, size : Int) : Unit {
if (fromIndex < 0)
throw IndexOutOfBoundsException("fromIndex = " + fromIndex)
if (toIndex > size)
throw IndexOutOfBoundsException("toIndex = " + toIndex)
if (fromIndex > toIndex)
throw IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")")
}
}
}