package foo.bar

/*p:foo.bar*/class D1 {
    fun get(t: /*c:foo.bar.D1 p:foo.bar*/Any?, p: /*c:foo.bar.D1 p:foo.bar*/PropertyMetadata) = 1
}

/*p:foo.bar*/fun /*p:foo.bar*/D1.set(t: /*p:foo.bar*/Any?, p: /*p:foo.bar*/PropertyMetadata, v: /*p:foo.bar*/Int) {}

/*p:foo.bar(D2)*/open class D2 {
    fun set(t: /*c:foo.bar.D2 p:foo.bar*/Any?, p: /*c:foo.bar.D2 p:foo.bar*/PropertyMetadata, v: /*c:foo.bar.D2 p:foo.bar*/Int) {}
}

/*p:foo.bar*/fun /*p:foo.bar*/D2.get(t: /*p:foo.bar*/Any?, p: /*p:foo.bar*/PropertyMetadata) = 1
/*p:foo.bar*/fun /*p:foo.bar*/D2.propertyDelegated(p: /*p:foo.bar*/Any?) {}

/*p:foo.bar*/class D3 : /*p:foo.bar*/D2() {
    fun propertyDelegated(p: /*c:foo.bar.D3 p:foo.bar*/Any?) {}
}


/*p:foo.bar*/val x1 by /*p:foo.bar c:foo.bar.D1(get) c:foo.bar.D1(propertyDelegated) p:foo.bar(propertyDelegated)*/D1()
/*p:foo.bar*/var y1 by /*p:foo.bar c:foo.bar.D1(get) c:foo.bar.D1(set) p:foo.bar(set) c:foo.bar.D1(propertyDelegated) p:foo.bar(propertyDelegated)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar c:foo.bar.D2(get) p:foo.bar(get) c:foo.bar.D2(propertyDelegated) p:foo.bar(propertyDelegated)*/D2()
/*p:foo.bar*/var y2 by /*p:foo.bar c:foo.bar.D2(get) p:foo.bar(get) c:foo.bar.D2(set) c:foo.bar.D2(propertyDelegated) p:foo.bar(propertyDelegated)*/D2()

/*p:foo.bar*/val x3 by /*p:foo.bar c:foo.bar.D3(get) p:foo.bar(get) c:foo.bar.D3(propertyDelegated)*/D3()
/*p:foo.bar*/var y3 by /*p:foo.bar c:foo.bar.D3(get) p:foo.bar(get) c:foo.bar.D3(set) c:foo.bar.D3(propertyDelegated)*/D3()
