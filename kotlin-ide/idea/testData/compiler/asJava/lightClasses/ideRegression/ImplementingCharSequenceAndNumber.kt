// p1.Container
package p1

class Container {
    class MyString : CharSequence {
        override val length: Int
            get() = 0

        override fun chars(): IntStream = error("")

        override fun codePoints(): IntStream = error("")

        override fun get(index: Int): Char = 'c'

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = MyString()
    }

    class MyNumber : Number {
        override fun toByte(): Byte {
            TODO("not implemented")
        }

        override fun toChar(): Char {
            TODO("not implemented")
        }

        override fun toDouble(): Double {
            TODO("not implemented")
        }

        override fun toFloat(): Float {
            TODO("not implemented")
        }

        override fun toInt(): Int {
            TODO("not implemented")
        }

        override fun toLong(): Long {
            TODO("not implemented")
        }

        override fun toShort(): Short {
            TODO("not implemented")
        }
    }
}