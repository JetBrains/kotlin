package foo.bar

import kotlin.reflect./*p:kotlin.reflect*/KProperty

/*p:foo.bar*/class D1 {
    fun get(t: /*c:foo.bar.D1 p:foo.bar*/Any?, p: /*c:foo.bar.D1*/KProperty<*>) = 1
}

/*p:foo.bar*/fun /*p:foo.bar*/D1.set(t: /*p:foo.bar*/Any?, p: KProperty<*>, v: /*p:foo.bar*/Int) {}

/*p:foo.bar(D2)*/open class D2 {
    fun set(t: /*c:foo.bar.D2 p:foo.bar*/Any?, p: /*c:foo.bar.D2*/KProperty<*>, v: /*c:foo.bar.D2 p:foo.bar*/Int) {}
}

/*p:foo.bar*/fun /*p:foo.bar*/D2.get(t: /*p:foo.bar*/Any?, p: KProperty<*>) = 1
/*p:foo.bar*/fun /*p:foo.bar*/D2.propertyDelegated(p: /*p:foo.bar*/Any?) {}

/*p:foo.bar*/class D3 : /*p:foo.bar*/D2() {
    fun propertyDelegated(p: /*c:foo.bar.D3 c:foo.bar.D2 p:foo.bar*/Any?) {}
}


/*p:foo.bar*/val x1 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D1(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D1(getGetValue) c:foo.bar.D1(getGETValue) c:foo.bar.D1(get) c:foo.bar.D1(propertyDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.io(propertyDelegated) c:foo.bar.D1(getPropertyDelegated) c:foo.bar.D1(getPROPERTYDelegated)*/D1()
/*p:foo.bar*/var y1 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D1(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D1(getGetValue) c:foo.bar.D1(getGETValue) c:foo.bar.D1(get) c:foo.bar.D1(setValue) p:foo.bar(setValue) p:java.lang(setValue) p:kotlin(setValue) p:kotlin.annotation(setValue) p:kotlin.jvm(setValue) p:kotlin.io(setValue) c:foo.bar.D1(getSetValue) c:foo.bar.D1(getSETValue) c:foo.bar.D1(set) p:foo.bar(set) p:java.lang(set) p:kotlin(set) p:kotlin.annotation(set) p:kotlin.jvm(set) p:kotlin.io(set) c:foo.bar.D1(propertyDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.io(propertyDelegated) c:foo.bar.D1(getPropertyDelegated) c:foo.bar.D1(getPROPERTYDelegated)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D2(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D2(getGetValue) c:foo.bar.D2(getGETValue) c:foo.bar.D2(get) p:foo.bar(get) p:java.lang(get) p:kotlin(get) p:kotlin.annotation(get) p:kotlin.jvm(get) p:kotlin.io(get) c:foo.bar.D2(propertyDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.io(propertyDelegated)*/D2()
/*p:foo.bar*/var y2 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D2(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D2(getGetValue) c:foo.bar.D2(getGETValue) c:foo.bar.D2(get) p:foo.bar(get) p:java.lang(get) p:kotlin(get) p:kotlin.annotation(get) p:kotlin.jvm(get) p:kotlin.io(get) c:foo.bar.D2(setValue) p:foo.bar(setValue) p:java.lang(setValue) p:kotlin(setValue) p:kotlin.annotation(setValue) p:kotlin.jvm(setValue) p:kotlin.io(setValue) c:foo.bar.D2(getSetValue) c:foo.bar.D2(getSETValue) c:foo.bar.D2(set) c:foo.bar.D2(propertyDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.io(propertyDelegated)*/D2()

/*p:foo.bar*/val x3 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D3(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D3(getGetValue) c:foo.bar.D3(getGETValue) c:foo.bar.D3(get) p:foo.bar(get) p:java.lang(get) p:kotlin(get) p:kotlin.annotation(get) p:kotlin.jvm(get) p:kotlin.io(get) c:foo.bar.D3(propertyDelegated)*/D3()
/*p:foo.bar*/var y3 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D3(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D3(getGetValue) c:foo.bar.D3(getGETValue) c:foo.bar.D3(get) p:foo.bar(get) p:java.lang(get) p:kotlin(get) p:kotlin.annotation(get) p:kotlin.jvm(get) p:kotlin.io(get) c:foo.bar.D3(setValue) p:foo.bar(setValue) p:java.lang(setValue) p:kotlin(setValue) p:kotlin.annotation(setValue) p:kotlin.jvm(setValue) p:kotlin.io(setValue) c:foo.bar.D3(getSetValue) c:foo.bar.D3(getSETValue) c:foo.bar.D3(set) c:foo.bar.D3(propertyDelegated)*/D3()
