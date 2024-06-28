package foo.bar

/*p:kotlin.reflect(KProperty)*/import kotlin.reflect.KProperty

/*p:foo.bar*/class D1 {
    operator fun getValue(t: /*p:foo.bar p:foo.bar.D1*/Any?, p: /*p:foo.bar.D1 p:kotlin.reflect*/KProperty<*>) = 1
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D1.setValue(t: /*p:foo.bar*/Any?, p: /*p:kotlin.reflect*/KProperty<*>, v: /*p:foo.bar*/Int) {}

/*p:foo.bar(D2)*/open class D2 {
    operator fun setValue(t: /*p:foo.bar p:foo.bar.D2*/Any?, p: /*p:foo.bar.D2 p:kotlin.reflect*/KProperty<*>, v: /*p:foo.bar p:foo.bar.D2*/Int) {}
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D2.getValue(t: /*p:foo.bar*/Any?, p: /*p:kotlin.reflect*/KProperty<*>) = 1
/*p:foo.bar*/operator fun /*p:foo.bar*/D2.provideDelegate(p: /*p:foo.bar*/Any?, k: /*p:foo.bar*/Any) = /*p:foo.bar(D2)*/this

/*p:foo.bar*/class D3 : /*p:foo.bar*/D2() {
    operator fun provideDelegate(p: /*p:foo.bar p:foo.bar.D2 p:foo.bar.D3*/Any?, k: /*p:foo.bar p:foo.bar.D2 p:foo.bar.D3*/Any) = /*p:foo.bar(D3)*/this
}


/*p:foo.bar*/val x1 by /*p:foo.bar p:foo.bar(provideDelegate) p:foo.bar.D1(getPROVIDEDelegate) p:foo.bar.D1(getProvideDelegate) p:foo.bar.D1(getValue) p:foo.bar.D1(provideDelegate) p:java.lang(provideDelegate) p:kotlin(provideDelegate) p:kotlin.annotation(provideDelegate) p:kotlin.collections(provideDelegate) p:kotlin.comparisons(provideDelegate) p:kotlin.io(provideDelegate) p:kotlin.jvm(provideDelegate) p:kotlin.ranges(provideDelegate) p:kotlin.sequences(provideDelegate) p:kotlin.text(provideDelegate)*/D1()
/*p:foo.bar*/var y1 by /*p:foo.bar p:foo.bar(provideDelegate) p:foo.bar(setValue) p:foo.bar.D1(getPROVIDEDelegate) p:foo.bar.D1(getProvideDelegate) p:foo.bar.D1(getSETValue) p:foo.bar.D1(getSetValue) p:foo.bar.D1(getValue) p:foo.bar.D1(provideDelegate) p:foo.bar.D1(setValue) p:java.lang(provideDelegate) p:kotlin(provideDelegate) p:kotlin.annotation(provideDelegate) p:kotlin.collections(provideDelegate) p:kotlin.comparisons(provideDelegate) p:kotlin.io(provideDelegate) p:kotlin.jvm(provideDelegate) p:kotlin.ranges(provideDelegate) p:kotlin.sequences(provideDelegate) p:kotlin.text(provideDelegate)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar p:foo.bar(getValue) p:foo.bar(provideDelegate) p:foo.bar.D2(getGETValue) p:foo.bar.D2(getGetValue) p:foo.bar.D2(getPROVIDEDelegate) p:foo.bar.D2(getProvideDelegate) p:foo.bar.D2(getValue) p:foo.bar.D2(provideDelegate)*/D2()
/*p:foo.bar*/var y2 by /*p:foo.bar p:foo.bar(getValue) p:foo.bar(provideDelegate) p:foo.bar.D2(getGETValue) p:foo.bar.D2(getGetValue) p:foo.bar.D2(getPROVIDEDelegate) p:foo.bar.D2(getProvideDelegate) p:foo.bar.D2(getValue) p:foo.bar.D2(provideDelegate) p:foo.bar.D2(setValue)*/D2()

/*p:foo.bar*/val x3 by /*p:foo.bar p:foo.bar(getValue) p:foo.bar.D2(getValue) p:foo.bar.D2(provideDelegate) p:foo.bar.D3(getGETValue) p:foo.bar.D3(getGetValue) p:foo.bar.D3(getValue) p:foo.bar.D3(provideDelegate)*/D3()
/*p:foo.bar*/var y3 by /*p:foo.bar p:foo.bar(getValue) p:foo.bar.D2(getValue) p:foo.bar.D2(provideDelegate) p:foo.bar.D2(setValue) p:foo.bar.D3(getGETValue) p:foo.bar.D3(getGetValue) p:foo.bar.D3(getValue) p:foo.bar.D3(provideDelegate) p:foo.bar.D3(setValue)*/D3()
