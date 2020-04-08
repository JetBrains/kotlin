package foo

class Foo : Comparable<Foo> {
    override fun compareTo(other: Foo): Int = TODO()
}

operator fun ClosedRange<Foo>.iterator(): Iterator<Foo> = TODO()