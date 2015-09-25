package foo.bar

/*p:foo.bar*/class A {
    fun plus(a: /*c:foo.bar.A p:foo.bar*/Int) = this
    fun timesAssign(a: /*c:foo.bar.A p:foo.bar*/Any?) {}
    fun inc(): /*c:foo.bar.A p:foo.bar*/A = this

    fun get(i: /*c:foo.bar.A p:foo.bar*/Int) = 1
    fun contains(a: /*c:foo.bar.A p:foo.bar*/Int): /*c:foo.bar.A p:foo.bar*/Boolean = false
    fun invoke() {}

    fun compareTo(a: /*c:foo.bar.A p:foo.bar*/Int) = 0

    fun component1() = this

    fun iterator() = this
    fun next() = this
}

/*p:foo.bar*/fun /*p:foo.bar*/A.minus(a: /*p:foo.bar*/Int) = this
/*p:foo.bar*/fun /*p:foo.bar*/A.divAssign(a: /*p:foo.bar*/Any?) {}
/*p:foo.bar*/fun /*p:foo.bar*/A.dec(): /*p:foo.bar*/A = this

/*p:foo.bar*/fun /*p:foo.bar*/A.not() {}

/*p:foo.bar*/fun /*p:foo.bar*/A.set(i: /*p:foo.bar*/Int, v: /*p:foo.bar*/Int) {}
/*p:foo.bar*/fun /*p:foo.bar*/A.contains(a: /*p:foo.bar*/Any): /*p:foo.bar*/Boolean = true
/*p:foo.bar*/fun /*p:foo.bar*/A.invoke(i: /*p:foo.bar*/Int) {}

/*p:foo.bar*/fun /*p:foo.bar*/A.compareTo(a: /*p:foo.bar*/Any) = 0

/*p:foo.bar*/fun /*p:foo.bar*/A.component2() = this

/*p:foo.bar*/fun /*p:foo.bar*/A?.iterator() = this!!
/*p:foo.bar*/fun /*p:foo.bar*/A.hasNext(): /*p:foo.bar*/Boolean = false
