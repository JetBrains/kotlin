package foo.bar

/*p:foo.bar*/class A {
    operator fun plus(a: /*c:foo.bar.A p:foo.bar p:kotlin*/Int) = /*p:foo.bar(A)*/this
    operator fun timesAssign(a: /*c:foo.bar.A p:foo.bar p:kotlin*/Any?) {}
    operator fun inc(): /*c:foo.bar.A p:foo.bar*/A = /*p:foo.bar(A)*/this

    operator fun get(i: /*c:foo.bar.A p:foo.bar p:kotlin*/Int) = /*p:kotlin(Int)*/1
    operator fun contains(a: /*c:foo.bar.A p:foo.bar p:kotlin*/Int): /*c:foo.bar.A p:foo.bar p:kotlin*/Boolean = /*p:kotlin(Boolean)*/false
    operator fun invoke() {}

    operator fun compareTo(a: /*c:foo.bar.A p:foo.bar p:kotlin*/Int) = /*p:kotlin(Int)*/0

    operator fun component1() = /*p:foo.bar(A)*/this

    operator fun iterator() = /*p:foo.bar(A)*/this
    operator fun next() = /*p:foo.bar(A)*/this
}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.minus(a: /*p:foo.bar p:kotlin*/Int) = /*p:foo.bar(A)*/this
/*p:foo.bar*/operator fun /*p:foo.bar*/A.divAssign(a: /*p:foo.bar p:kotlin*/Any?) {}
/*p:foo.bar*/operator fun /*p:foo.bar*/A.dec(): /*p:foo.bar*/A = /*p:foo.bar(A)*/this

/*p:foo.bar*/operator fun /*p:foo.bar*/A.not() {}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.set(i: /*p:foo.bar p:kotlin*/Int, v: /*p:foo.bar p:kotlin*/Int) {}
/*p:foo.bar*/operator fun /*p:foo.bar*/A.contains(a: /*p:foo.bar p:kotlin*/Any): /*p:foo.bar p:kotlin*/Boolean = /*p:kotlin(Boolean)*/true
/*p:foo.bar*/operator fun /*p:foo.bar*/A.invoke(i: /*p:foo.bar p:kotlin*/Int) {}

/*p:foo.bar*/operator fun /*p:foo.bar*/A.compareTo(a: /*p:foo.bar p:kotlin*/Any) = /*p:kotlin(Int)*/0

/*p:foo.bar*/operator fun /*p:foo.bar*/A.component2() = /*p:foo.bar(A)*/this

/*p:foo.bar*/operator fun /*p:foo.bar*/A?.iterator() = /*p:foo.bar(A)*/this!!
/*p:foo.bar*/operator fun /*p:foo.bar*/A.hasNext(): /*p:foo.bar p:kotlin*/Boolean = /*p:kotlin(Boolean)*/false
