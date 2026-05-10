// LANGUAGE: +CompanionBlocksAndExtensions
// FIR_DUMP
annotation class AllOpen

@AllOpen
class C {
    companion {
        fun foo() {}
        var bar = ""
    }
}

@AllOpen
companion fun C.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}

@AllOpen
companion var C.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!> = ""
