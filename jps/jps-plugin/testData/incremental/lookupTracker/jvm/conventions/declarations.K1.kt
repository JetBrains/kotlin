package foo.bar

/*p:foo.bar*/class A {
    operator fun plus(a: /*p:foo.bar p:foo.bar.A*/Int) = /*p:foo.bar(A)*/this
    operator fun timesAssign(a: /*p:foo.bar p:foo.bar.A*/Any?) {}
    operator fun inc(): /*p:foo.bar p:foo.bar.A*/A = /*p:foo.bar(A)*/this

    operator fun get(i: /*p:foo.bar p:foo.bar.A*/Int) = 1
    operator fun contains(a: /*p:foo.bar p:foo.bar.A*/Int): /*p:foo.bar p:foo.bar.A*/Boolean = false
    operator fun invoke() {}

    operator fun compareTo(a: /*p:foo.bar p:foo.bar.A*/Int) = 0

    operator fun component1() = /*p:foo.bar(A)*/this

    operator fun iterator() = /*p:foo.bar(A)*/this
    operator fun next() = /*p:foo.bar(A)*/this
}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.minus(a: /*p:foo.bar*/Int) = /*p:foo.bar(A)*/this
/*p:foo.bar*/operator fun /*p:foo.bar*/A.divAssign(a: /*p:foo.bar*/Any?) {}
/*p:foo.bar*/operator fun /*p:foo.bar*/A.dec(): /*p:foo.bar*/A = /*p:foo.bar(A)*/this

/*p:foo.bar*/operator fun /*p:foo.bar*/A.not() {}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.set(i: /*p:foo.bar*/Int, v: /*p:foo.bar*/Int) {}
/*p:foo.bar*/operator fun /*p:foo.bar*/A.contains(a: /*p:foo.bar*/Any): /*p:foo.bar*/Boolean = true
/*p:foo.bar*/operator fun /*p:foo.bar*/A.invoke(i: /*p:foo.bar*/Int) {}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.compareTo(a: /*p:foo.bar*/Any) = 0

/*p:foo.bar*/operator fun /*p:foo.bar*/A.component2() = /*p:foo.bar(A)*/this

/*p:foo.bar*/operator fun /*p:foo.bar*/A?.iterator() = /*p:foo.bar(A)*/this!!
/*p:foo.bar*/operator fun /*p:foo.bar*/A.hasNext(): /*p:foo.bar*/Boolean = false
