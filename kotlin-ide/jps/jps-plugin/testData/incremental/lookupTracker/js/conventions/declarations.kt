package foo.bar

/*p:foo.bar*/class A {
    operator fun plus(a: /*c:foo.bar.A p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Int) = /*p:foo.bar(A)*/this
    operator fun timesAssign(a: /*c:foo.bar.A p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Any?) {}
    operator fun inc(): /*c:foo.bar.A p:foo.bar*/A = /*p:foo.bar(A)*/this

    operator fun get(i: /*c:foo.bar.A p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Int) = /*p:kotlin(Int)*/1
    operator fun contains(a: /*c:foo.bar.A p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Int): /*c:foo.bar.A p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Boolean = /*p:kotlin(Boolean)*/false
    operator fun invoke() {}

    operator fun compareTo(a: /*c:foo.bar.A p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Int) = /*p:kotlin(Int)*/0

    operator fun component1() = /*p:foo.bar(A)*/this

    operator fun iterator() = /*p:foo.bar(A)*/this
    operator fun next() = /*p:foo.bar(A)*/this
}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.minus(a: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Int) = /*p:foo.bar(A)*/this
/*p:foo.bar*/operator fun /*p:foo.bar*/A.divAssign(a: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Any?) {}
/*p:foo.bar*/operator fun /*p:foo.bar*/A.dec(): /*p:foo.bar*/A = /*p:foo.bar(A)*/this

/*p:foo.bar*/operator fun /*p:foo.bar*/A.not() {}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.set(i: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Int, v: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Int) {}
/*p:foo.bar*/operator fun /*p:foo.bar*/A.contains(a: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Any): /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Boolean = /*p:kotlin(Boolean)*/true
/*p:foo.bar*/operator fun /*p:foo.bar*/A.invoke(i: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Int) {}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.compareTo(a: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Any) = /*p:kotlin(Int)*/0

/*p:foo.bar*/operator fun /*p:foo.bar*/A.component2() = /*p:foo.bar(A)*/this

/*p:foo.bar*/operator fun /*p:foo.bar*/A?.iterator() = /*p:foo.bar(A)*/this!!
/*p:foo.bar*/operator fun /*p:foo.bar*/A.hasNext(): /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Boolean = /*p:kotlin(Boolean)*/false
