package foo.bar

/*p:foo.bar*/class A {
    operator fun plus(a: /*p:foo.bar*/Int) = this
    operator fun timesAssign(a: /*p:foo.bar*/Any?) {}
    operator fun inc(): /*p:foo.bar*/A = this

    operator fun get(i: /*p:foo.bar*/Int) = 1
    operator fun contains(a: /*p:foo.bar*/Int): /*p:foo.bar*/Boolean = false
    operator fun invoke() {}

    operator fun compareTo(a: /*p:foo.bar*/Int) = 0

    operator fun component1() = this

    operator fun iterator() = this
    operator fun next() = this
}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.minus(a: /*p:foo.bar*/Int) = this
/*p:foo.bar*/operator fun /*p:foo.bar*/A.divAssign(a: /*p:foo.bar*/Any?) {}
/*p:foo.bar*/operator fun /*p:foo.bar*/A.dec(): /*p:foo.bar*/A = this

/*p:foo.bar*/operator fun /*p:foo.bar*/A.not() {}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.set(i: /*p:foo.bar*/Int, v: /*p:foo.bar*/Int) {}
/*p:foo.bar*/operator fun /*p:foo.bar*/A.contains(a: /*p:foo.bar*/Any): /*p:foo.bar*/Boolean = true
/*p:foo.bar*/operator fun /*p:foo.bar*/A.invoke(i: /*p:foo.bar*/Int) {}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.compareTo(a: /*p:foo.bar*/Any) = 0

/*p:foo.bar*/operator fun /*p:foo.bar*/A.component2() = this

/*p:foo.bar*/operator fun /*p:foo.bar*/A?.iterator() = /*p:foo.bar(A)*/this!!
/*p:foo.bar*/operator fun /*p:foo.bar*/A.hasNext(): /*p:foo.bar*/Boolean = false
