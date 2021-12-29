@file:JvmName("Utils")
@file:JvmMultifileClass

val bVal: Int get() = 0

class OuterClass{
    inner class InnerClass {
        val getZero: Int get() = 0
    }
}