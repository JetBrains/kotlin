package foo

import bar.*

/*p:foo*/fun <T : /*p:foo*/A?, B : /*p:foo p:kotlin.collections*/Iterable</*p:foo p:kotlin*/Number>, C, D> test()
        where C : /*p:foo p:kotlin*/Number, C : /*p:foo p:kotlin*/Comparable</*p:foo p:kotlin*/Number>, D : B
{}
