package foo.bar

/*p:kotlin.reflect(KProperty)*/import kotlin.reflect.KProperty

/*p:foo.bar*/class D1 {
    operator fun getValue(t: /*c:foo.bar.D1 p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Any?, p: /*c:foo.bar.D1 p:kotlin.reflect*/KProperty<*>) = /*p:kotlin(Int)*/1
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D1.setValue(t: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Any?, p: /*p:kotlin.reflect*/KProperty<*>, v: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Int) {}

/*p:foo.bar(D2)*/open class D2 {
    operator fun setValue(t: /*c:foo.bar.D2 p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Any?, p: /*c:foo.bar.D2 p:kotlin.reflect*/KProperty<*>, v: /*c:foo.bar.D2 p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Int) {}
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D2.getValue(t: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Any?, p: /*p:kotlin.reflect*/KProperty<*>) = /*p:kotlin(Int)*/1
/*p:foo.bar*/operator fun /*p:foo.bar*/D2.provideDelegate(p: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Any?, k: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Any) = /*p:foo.bar(D2)*/this

/*p:foo.bar*/class D3 : /*p:foo.bar*/D2() {
    operator fun provideDelegate(p: /*c:foo.bar.D3 c:foo.bar.D2 p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Any?, k: /*c:foo.bar.D3 c:foo.bar.D2 p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Any) = /*p:foo.bar(D3)*/this
}


/*p:foo.bar*/val x1 by /*p:foo.bar c:foo.bar.D1(provideDelegate) p:foo.bar(provideDelegate) p:kotlin(provideDelegate) p:kotlin.annotation(provideDelegate) p:kotlin.collections(provideDelegate) p:kotlin.ranges(provideDelegate) p:kotlin.sequences(provideDelegate) p:kotlin.text(provideDelegate) p:kotlin.io(provideDelegate) p:kotlin.comparisons(provideDelegate) p:kotlin.js(provideDelegate) c:foo.bar.D1(getValue)*/D1()
/*p:foo.bar*/var y1 by /*p:foo.bar c:foo.bar.D1(provideDelegate) p:foo.bar(provideDelegate) p:kotlin(provideDelegate) p:kotlin.annotation(provideDelegate) p:kotlin.collections(provideDelegate) p:kotlin.ranges(provideDelegate) p:kotlin.sequences(provideDelegate) p:kotlin.text(provideDelegate) p:kotlin.io(provideDelegate) p:kotlin.comparisons(provideDelegate) p:kotlin.js(provideDelegate) c:foo.bar.D1(getValue) c:foo.bar.D1(setValue) p:foo.bar(setValue)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar c:foo.bar.D2(provideDelegate) p:foo.bar(provideDelegate) c:foo.bar.D2(getValue) p:foo.bar(getValue)*/D2()
/*p:foo.bar*/var y2 by /*p:foo.bar c:foo.bar.D2(provideDelegate) p:foo.bar(provideDelegate) c:foo.bar.D2(getValue) p:foo.bar(getValue) c:foo.bar.D2(setValue)*/D2()

/*p:foo.bar*/val x3 by /*p:foo.bar c:foo.bar.D3(provideDelegate) c:foo.bar.D2(provideDelegate) c:foo.bar.D3(getValue) c:foo.bar.D2(getValue) p:foo.bar(getValue)*/D3()
/*p:foo.bar*/var y3 by /*p:foo.bar c:foo.bar.D3(provideDelegate) c:foo.bar.D2(provideDelegate) c:foo.bar.D3(getValue) c:foo.bar.D2(getValue) p:foo.bar(getValue) c:foo.bar.D3(setValue) c:foo.bar.D2(setValue)*/D3()
