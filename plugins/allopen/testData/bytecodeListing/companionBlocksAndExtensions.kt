// LANGUAGE: +CompanionBlocksAndExtensions
annotation class AllOpen

@AllOpen
class C {
    companion {
        fun foo() {}
        var bar = ""
    }
}

@AllOpen
companion fun C.foo() {}

@AllOpen
companion var C.bar = ""
