internal class Test {
    val c: Char = 1.toChar()
    val i = 1

    fun operationsWithChar() {
        b(i > c.toInt())
        b(i >= c.toInt())
        b(i < c.toInt())
        b(i <= c.toInt())

        b(c.toInt() > i)
        b(c.toInt() >= i)
        b(c.toInt() < i)
        b(c.toInt() <= i)

        b(c.toInt() == i)
        b(c.toInt() != i)

        b(i == c.toInt())
        b(i != c.toInt())

        i(i + c.toInt())
        i(i - c.toInt())
        i(i / c.toInt())
        i(i * c.toInt())
        i(i % c.toInt())
        i(i or c.toInt())
        i(i and c.toInt())
        i(i shl c.toInt())
        i(i shr c.toInt())
        i(i ushr c.toInt())

        i(c.toInt() + i)
        i(c.toInt() - i)
        i(c.toInt() / i)
        i(c.toInt() * i)
        i(c.toInt() % i)
        i(c.toInt() or i)
        i(c.toInt() and i)
        i(c.toInt() shl i)
        i(c.toInt() shr i)
        i(c.toInt() ushr i)

        // TODO i(~c);
        // TODO i(-c);
        // TODO i(+c);
    }

    fun operationsWithCharLiteral() {
        b('0' > c)
        b('0' >= c)
        b('0' < c)
        b('0' <= c)

        b(c > '0')
        b(c >= '0')
        b(c < '0')
        b(c <= '0')

        b(c == '0')
        b(c != '0')

        b('0' == c)
        b('0' != c)

        b(i > '0'.toInt())
        b(i >= '0'.toInt())
        b(i < '0'.toInt())
        b(i <= '0'.toInt())

        b('0'.toInt() > i)
        b('0'.toInt() >= i)
        b('0'.toInt() < i)
        b('0'.toInt() <= i)

        b('0'.toInt() == i)
        b('0'.toInt() != i)

        b(i == '0'.toInt())
        b(i != '0'.toInt())

        i(i + '0'.toInt())
        i(i - '0'.toInt())
        i(i / '0'.toInt())
        i(i * '0'.toInt())
        i(i % '0'.toInt())
        i(i or '0'.toInt())
        i(i and '0'.toInt())
        i(i shl '0'.toInt())
        i(i shr '0'.toInt())
        i(i ushr '0'.toInt())

        i('0'.toInt() + i)
        i('0'.toInt() - i)
        i('0'.toInt() / i)
        i('0'.toInt() * i)
        i('0'.toInt() % i)
        i('0'.toInt() or i)
        i('0'.toInt() and i)
        i('0'.toInt() shl i)
        i('0'.toInt() shr i)
        i('0'.toInt() ushr i)

        // TODO i(~'0');
        // TODO i(-'0');
        // TODO i(+'0');
    }

    fun operationsWithCharLiterals() {
        b('A' > '0')
        b('A' >= '0')
        b('A' < '0')
        b('A' <= '0')

        b('0' == 'A')
        b('0' != 'A')

        i('A'.toInt() + '0'.toInt())
        i('A'.toInt() - '0'.toInt())
        i('A'.toInt() / '0'.toInt())
        i('A'.toInt() * '0'.toInt())
        i('A'.toInt() % '0'.toInt())
        i('A'.toInt() or '0'.toInt())
        i('A'.toInt() and '0'.toInt())
        i('A'.toInt() shl '0'.toInt())
        i('A'.toInt() shr '0'.toInt())
        i('A'.toInt() ushr '0'.toInt())
    }

    fun b(b: Boolean) {}
    fun i(i: Int) {}
}
