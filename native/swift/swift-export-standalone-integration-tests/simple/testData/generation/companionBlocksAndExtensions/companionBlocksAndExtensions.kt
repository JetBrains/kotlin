// KIND: STANDALONE
// LANGUAGE: +CompanionBlocksAndExtensions

class Foo {
    companion {
        val staticField = 0
        val staticGetter: Int
            get() = 0
        fun staticFun() {}
    }
}

companion val Foo.staticGetterExtension: Int
    get() = 0

companion fun Foo.staticFunExtension() {}
