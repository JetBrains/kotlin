package foo

import bar.*

/*p:foo*/fun <T : /*p:foo*/A?, B : /*p:foo p:bar*/Iterable</*p:foo p:bar*/Number>, C, D> test()
        where C : /*p:foo p:bar*/Number, C : /*p:foo p:bar*/Comparable</*p:foo p:bar*/Number>, D : B
{}
