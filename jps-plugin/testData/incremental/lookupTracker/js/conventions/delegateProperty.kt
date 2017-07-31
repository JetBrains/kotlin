package foo.bar

import kotlin.reflect.KProperty

/*p:foo.bar*/class D1 {
    operator fun getValue(t: /*c:foo.bar.D1 p:foo.bar*/Any?, p: /*c:foo.bar.D1*/KProperty<*>) = /*p:kotlin(Int)*/1
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D1.setValue(t: /*p:foo.bar*/Any?, p: KProperty<*>, v: /*p:foo.bar*/Int) {}

/*p:foo.bar(D2)*/open class D2 {
    operator fun setValue(t: /*c:foo.bar.D2 p:foo.bar*/Any?, p: /*c:foo.bar.D2*/KProperty<*>, v: /*c:foo.bar.D2 p:foo.bar*/Int) {}
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D2.getValue(t: /*p:foo.bar*/Any?, p: KProperty<*>) = /*p:kotlin(Int)*/1
/*p:foo.bar*/operator fun /*p:foo.bar*/D2.provideDelegate(p: /*p:foo.bar*/Any?, k: /*p:foo.bar*/Any) = /*p:foo.bar(D2)*/this

/*p:foo.bar*/class D3 : /*p:foo.bar*/D2() {
    operator fun provideDelegate(p: /*c:foo.bar.D3 c:foo.bar.D2 p:foo.bar*/Any?, k: /*c:foo.bar.D3 c:foo.bar.D2 p:foo.bar*/Any) = /*p:foo.bar(D3)*/this
}


/*p:foo.bar*/val x1 by /*p:foo.bar c:foo.bar.D1(provideDelegate) p:foo.bar(provideDelegate) c:foo.bar.D1(getValue)*/D1()
/*p:foo.bar*/var y1 by /*p:foo.bar c:foo.bar.D1(provideDelegate) p:foo.bar(provideDelegate) c:foo.bar.D1(getValue) c:foo.bar.D1(setValue) p:foo.bar(setValue)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar c:foo.bar.D2(provideDelegate) p:foo.bar(provideDelegate) c:foo.bar.D2(getValue) p:foo.bar(getValue)*/D2()
/*p:foo.bar*/var y2 by /*p:foo.bar c:foo.bar.D2(provideDelegate) p:foo.bar(provideDelegate) c:foo.bar.D2(getValue) p:foo.bar(getValue) c:foo.bar.D2(setValue)*/D2()

/*p:foo.bar*/val x3 by /*p:foo.bar c:foo.bar.D3(provideDelegate) c:foo.bar.D2(provideDelegate) c:foo.bar.D3(getValue) c:foo.bar.D2(getValue) p:foo.bar(getValue)*/D3()
/*p:foo.bar*/var y3 by /*p:foo.bar c:foo.bar.D3(provideDelegate) c:foo.bar.D2(provideDelegate) c:foo.bar.D3(getValue) c:foo.bar.D2(getValue) p:foo.bar(getValue) c:foo.bar.D3(setValue) c:foo.bar.D2(setValue)*/D3()
