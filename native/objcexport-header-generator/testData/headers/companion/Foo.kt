class Foo2 {
    companion object {
        const val constant = 42
        val refToItself = Foo2
        val refToFoo1 = Foo1
        fun publicFoo() {}
    }
}

class Foo3 {
    companion object {

    }
}

class Foo1 {
    companion object {
        const val constant = 42
        val refToItself = Foo1
        val refToFoo2 = Foo2
        fun publicFoo() {}
    }
}