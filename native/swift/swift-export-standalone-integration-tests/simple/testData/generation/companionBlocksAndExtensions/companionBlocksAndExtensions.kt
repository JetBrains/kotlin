// KIND: STANDALONE
// LANGUAGE: +CompanionBlocks +CompanionExtensions

class Foo {
    companion {
        val xVal = 0
        var xVar = 0
        val yVal
            get() = 0
        var yVar
            get() = 0
            set(value) {}
        fun f() {}
    }
}

class Bar

companion val Bar.xVal = 0
companion var Bar.xVar = 0
companion val Bar.yVal
    get() = 0
companion var Bar.yVar
    get() = 0
    set(value) {}
companion fun Bar.f() {}
