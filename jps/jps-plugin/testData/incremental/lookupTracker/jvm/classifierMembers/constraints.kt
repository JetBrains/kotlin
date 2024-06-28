package foo

import bar.*

/*p:foo*/fun <T : /*p:bar p:foo*/A?, B : /*p:bar p:foo p:kotlin(Number) p:kotlin.collections*/Iterable</*p:bar p:foo p:kotlin*/Number>, C, D> test()
        where C : /*p:bar p:foo p:kotlin*/Number, C : /*p:bar p:foo p:kotlin p:kotlin(Number)*/Comparable</*p:bar p:foo p:kotlin*/Number>, D : /*p:bar p:foo*/B
{}
