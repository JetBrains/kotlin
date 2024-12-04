package foo.bar

/*p:kotlin.reflect(KProperty)*/import kotlin.reflect.KProperty

/*p:foo.bar*/class D1 {
    operator fun getValue(t: /*p:foo.bar*/Any?, p: /*p:foo.bar p:kotlin.reflect*/KProperty<*>) = 1
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D1.setValue(t: /*p:foo.bar*/Any?, p: /*p:foo.bar p:kotlin.reflect*/KProperty<*>, v: /*p:foo.bar*/Int) {}

/*p:foo.bar(D2)*/open class D2 {
    operator fun setValue(t: /*p:foo.bar*/Any?, p: /*p:foo.bar p:kotlin.reflect*/KProperty<*>, v: /*p:foo.bar*/Int) {}
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D2.getValue(t: /*p:foo.bar*/Any?, p: /*p:foo.bar p:kotlin.reflect*/KProperty<*>) = 1
/*p:foo.bar*/operator fun /*p:foo.bar*/D2.provideDelegate(p: /*p:foo.bar*/Any?, k: /*p:foo.bar*/Any) = this

/*p:foo.bar*/class D3 : /*p:foo.bar*/D2() {
    operator fun provideDelegate(p: /*p:foo.bar*/Any?, k: /*p:foo.bar*/Any) = this
}


/*p:foo.bar*/val x1 by /*p:foo.bar p:foo.bar(D2) p:foo.bar(provideDelegate) p:foo.bar.D1(getValue) p:foo.bar.D1(provideDelegate) p:kotlin.reflect(KProperty0)*/D1()
/*p:foo.bar*/var y1 by /*p:foo.bar p:foo.bar(D2) p:foo.bar(provideDelegate) p:foo.bar(setValue) p:foo.bar.D1(getValue) p:foo.bar.D1(provideDelegate) p:foo.bar.D1(setValue) p:kotlin.reflect(KMutableProperty0)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar p:foo.bar(getValue) p:foo.bar(provideDelegate) p:foo.bar.D2(getValue) p:foo.bar.D2(provideDelegate) p:kotlin.reflect(KProperty0)*/D2()
/*p:foo.bar*/var y2 by /*p:foo.bar p:foo.bar(getValue) p:foo.bar(provideDelegate) p:foo.bar.D2(getValue) p:foo.bar.D2(provideDelegate) p:foo.bar.D2(setValue) p:kotlin.reflect(KMutableProperty0)*/D2()

/*p:foo.bar*/val x3 by /*p:foo.bar p:foo.bar(getValue) p:foo.bar.D3(getValue) p:foo.bar.D3(provideDelegate) p:kotlin.reflect(KProperty0)*/D3()
/*p:foo.bar*/var y3 by /*p:foo.bar p:foo.bar(getValue) p:foo.bar.D2(setValue) p:foo.bar.D3(getValue) p:foo.bar.D3(provideDelegate) p:foo.bar.D3(setValue) p:kotlin.reflect(KMutableProperty0)*/D3()
