package foo

import bar.*

/*p:foo*/fun <T : /*p:foo*/A?, B : /*p:foo*/Iterable</*p:foo*/Number>, C, D> test()
        where C : /*p:foo*/Number, C : /*p:foo*/Comparable</*p:foo*/Number>, D : B
{}
