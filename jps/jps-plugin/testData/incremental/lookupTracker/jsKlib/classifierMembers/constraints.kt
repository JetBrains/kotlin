package foo

import bar.*

/*p:foo*/fun <T : /*p:bar p:foo*/A?, B : /*p:bar p:foo*/Iterable</*p:bar p:foo*/Number>, C, D> test()
        where C : /*p:bar p:foo*/Number, C : /*p:bar p:foo*/Comparable</*p:bar p:foo*/Number>, D : /*p:bar p:foo*/B
{}
