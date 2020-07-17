package foo

import bar.*

/*p:foo*/fun <T : /*p:foo*/A?, B : /*p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Iterable</*p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Number>, C, D> test()
        where C : /*p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Number, C : /*p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Comparable</*p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Number>, D : B
{}
