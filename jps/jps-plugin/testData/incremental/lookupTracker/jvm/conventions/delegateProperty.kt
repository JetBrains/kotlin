package foo.bar

/*p:kotlin.reflect(KProperty)*/import kotlin.reflect.KProperty

/*p:foo.bar*/class D1 {
    operator fun getValue(t: /*p:foo.bar p:kotlin.reflect*/Any?, p: /*p:foo.bar p:kotlin.reflect*/KProperty<*>) = 1
}

/*p:foo.bar*/operator fun /*p:foo.bar p:kotlin.reflect*/D1.setValue(t: /*p:foo.bar p:kotlin.reflect*/Any?, p: /*p:foo.bar p:kotlin.reflect*/KProperty<*>, v: /*p:foo.bar p:kotlin.reflect*/Int) {}

/*p:foo.bar(D2)*/open class D2 {
    operator fun setValue(t: /*p:foo.bar p:kotlin.reflect*/Any?, p: /*p:foo.bar p:kotlin.reflect*/KProperty<*>, v: /*p:foo.bar p:kotlin.reflect*/Int) {}
}

/*p:foo.bar*/operator fun /*p:foo.bar p:kotlin.reflect*/D2.getValue(t: /*p:foo.bar p:kotlin.reflect*/Any?, p: /*p:foo.bar p:kotlin.reflect*/KProperty<*>) = 1
/*p:foo.bar*/operator fun /*p:foo.bar p:kotlin.reflect*/D2.provideDelegate(p: /*p:foo.bar p:kotlin.reflect*/Any?, k: /*p:foo.bar p:kotlin.reflect*/Any) = this

/*p:foo.bar*/class D3 : /*p:foo.bar p:kotlin.reflect*/D2() {
    operator fun provideDelegate(p: /*p:foo.bar p:kotlin.reflect*/Any?, k: /*p:foo.bar p:kotlin.reflect*/Any) = this
}


/*p:foo.bar*/val x1 by /*p:D1(getValue) p:foo.bar p:foo.bar(D2) p:foo.bar(provideDelegate) p:foo.bar.D1(getValue) p:foo.bar.D1(provideDelegate) p:kotlin(Int) p:kotlin.reflect p:kotlin.reflect(KProperty0) p:kotlin.reflect(provideDelegate)*/D1()
/*p:foo.bar*/var y1 by /*p:D1(getValue) p:foo.bar p:foo.bar(D2) p:foo.bar(provideDelegate) p:foo.bar(setValue) p:foo.bar.D1(getValue) p:foo.bar.D1(provideDelegate) p:foo.bar.D1(setValue) p:kotlin(Int) p:kotlin(Unit) p:kotlin.reflect p:kotlin.reflect(KMutableProperty0) p:kotlin.reflect(provideDelegate) p:kotlin.reflect(setValue)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar p:foo.bar(getValue) p:foo.bar(provideDelegate) p:foo.bar.D2(getValue) p:foo.bar.D2(provideDelegate) p:kotlin(Int) p:kotlin.reflect p:kotlin.reflect(KProperty0) p:kotlin.reflect(getValue) p:kotlin.reflect(provideDelegate)*/D2()
/*p:foo.bar*/var y2 by /*p:D2(setValue) p:foo.bar p:foo.bar(getValue) p:foo.bar(provideDelegate) p:foo.bar.D2(getValue) p:foo.bar.D2(provideDelegate) p:foo.bar.D2(setValue) p:kotlin(Int) p:kotlin(Unit) p:kotlin.reflect p:kotlin.reflect(KMutableProperty0) p:kotlin.reflect(getValue) p:kotlin.reflect(provideDelegate)*/D2()

/*p:foo.bar*/val x3 by /*p:D3(provideDelegate) p:foo.bar p:foo.bar(getValue) p:foo.bar.D3(getValue) p:foo.bar.D3(provideDelegate) p:kotlin(Int) p:kotlin.reflect p:kotlin.reflect(KProperty0) p:kotlin.reflect(getValue)*/D3()
/*p:foo.bar*/var y3 by /*p:D2(setValue) p:D3(provideDelegate) p:foo.bar p:foo.bar(getValue) p:foo.bar.D3(getValue) p:foo.bar.D3(provideDelegate) p:foo.bar.D3(setValue) p:kotlin(Int) p:kotlin(Unit) p:kotlin.reflect p:kotlin.reflect(KMutableProperty0) p:kotlin.reflect(getValue)*/D3()
