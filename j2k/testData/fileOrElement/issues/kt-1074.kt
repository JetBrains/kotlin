package demo

class Test {
    default object {
        fun subListRangeCheck(fromIndex: Int, toIndex: Int, size: Int) {
            if (fromIndex < 0)
                throw IndexOutOfBoundsException("fromIndex = " + fromIndex)
            if (toIndex > size)
                throw IndexOutOfBoundsException("toIndex = " + toIndex)
            if (fromIndex > toIndex)
                throw IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")")
        }
    }
}