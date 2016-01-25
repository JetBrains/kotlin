package foo.bar

import kotlin.reflect./*p:kotlin.reflect*/KProperty

/*p:foo.bar*/class D1 {
    operator fun getValue(t: /*c:foo.bar.D1 p:foo.bar*/Any?, p: /*c:foo.bar.D1*/KProperty<*>) = 1
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D1.setValue(t: /*p:foo.bar*/Any?, p: KProperty<*>, v: /*p:foo.bar*/Int) {}

/*p:foo.bar(D2)*/open class D2 {
    operator fun setValue(t: /*c:foo.bar.D2 p:foo.bar*/Any?, p: /*c:foo.bar.D2*/KProperty<*>, v: /*c:foo.bar.D2 p:foo.bar*/Int) {}
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D2.getValue(t: /*p:foo.bar*/Any?, p: KProperty<*>) = 1
/*p:foo.bar*/fun /*p:foo.bar*/D2.propertyDelegated(p: /*p:foo.bar*/Any?) {}

/*p:foo.bar*/class D3 : /*p:foo.bar*/D2() {
    fun propertyDelegated(p: /*c:foo.bar.D3 c:foo.bar.D2 p:foo.bar*/Any?) {}
}


/*p:foo.bar*/val x1 by /*p:foo.bar c:foo.bar.D1(getValue) c:foo.bar.D1(propertyDelegated) c:foo.bar.D1(getPropertyDelegated) c:foo.bar.D1(getPROPERTYDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.collections(propertyDelegated) p:kotlin.ranges(propertyDelegated) p:kotlin.sequences(propertyDelegated) p:kotlin.text(propertyDelegated) p:kotlin.io(propertyDelegated)*/D1()
/*p:foo.bar*/var y1 by /*p:foo.bar c:foo.bar.D1(getValue) c:foo.bar.D1(setValue) c:foo.bar.D1(getSetValue) c:foo.bar.D1(getSETValue) p:foo.bar(setValue) c:foo.bar.D1(propertyDelegated) c:foo.bar.D1(getPropertyDelegated) c:foo.bar.D1(getPROPERTYDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.collections(propertyDelegated) p:kotlin.ranges(propertyDelegated) p:kotlin.sequences(propertyDelegated) p:kotlin.text(propertyDelegated) p:kotlin.io(propertyDelegated)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar c:foo.bar.D2(getValue) c:foo.bar.D2(getGetValue) c:foo.bar.D2(getGETValue) p:foo.bar(getValue) c:foo.bar.D2(propertyDelegated) c:foo.bar.D2(getPropertyDelegated) c:foo.bar.D2(getPROPERTYDelegated) p:foo.bar(propertyDelegated)*/D2()
/*p:foo.bar*/var y2 by /*p:foo.bar c:foo.bar.D2(getValue) c:foo.bar.D2(getGetValue) c:foo.bar.D2(getGETValue) p:foo.bar(getValue) c:foo.bar.D2(setValue) c:foo.bar.D2(propertyDelegated) c:foo.bar.D2(getPropertyDelegated) c:foo.bar.D2(getPROPERTYDelegated) p:foo.bar(propertyDelegated)*/D2()

/*p:foo.bar*/val x3 by /*p:foo.bar c:foo.bar.D3(getValue) c:foo.bar.D2(getValue) c:foo.bar.D3(getGetValue) c:foo.bar.D3(getGETValue) p:foo.bar(getValue) c:foo.bar.D3(propertyDelegated) c:foo.bar.D2(propertyDelegated)*/D3()
/*p:foo.bar*/var y3 by /*p:foo.bar c:foo.bar.D3(getValue) c:foo.bar.D2(getValue) c:foo.bar.D3(getGetValue) c:foo.bar.D3(getGETValue) p:foo.bar(getValue) c:foo.bar.D3(setValue) c:foo.bar.D2(setValue) c:foo.bar.D3(propertyDelegated) c:foo.bar.D2(propertyDelegated)*/D3()
