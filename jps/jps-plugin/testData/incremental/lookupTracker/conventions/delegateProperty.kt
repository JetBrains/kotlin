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
/*p:foo.bar*/operator fun /*p:foo.bar*/D2.propertyDelegated(p: /*p:foo.bar*/Any?) {}

/*p:foo.bar*/class D3 : /*p:foo.bar*/D2() {
    fun propertyDelegated(p: /*c:foo.bar.D3 c:foo.bar.D2 p:foo.bar*/Any?) {}
}


/*p:foo.bar*/val x1 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D1(getValue) c:foo.bar.D1(propertyDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.io(propertyDelegated) c:foo.bar.D1(getPropertyDelegated) c:foo.bar.D1(getPROPERTYDelegated)*/D1()
/*p:foo.bar*/var y1 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D1(getValue) c:foo.bar.D1(setValue) p:foo.bar(setValue) p:java.lang(setValue) p:kotlin(setValue) p:kotlin.annotation(setValue) p:kotlin.jvm(setValue) p:kotlin.io(setValue) c:foo.bar.D1(propertyDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.io(propertyDelegated) c:foo.bar.D1(getPropertyDelegated) c:foo.bar.D1(getPROPERTYDelegated)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D2(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D2(propertyDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.io(propertyDelegated)*/D2()
/*p:foo.bar*/var y2 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D2(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D2(setValue) c:foo.bar.D2(propertyDelegated) p:foo.bar(propertyDelegated) p:java.lang(propertyDelegated) p:kotlin(propertyDelegated) p:kotlin.annotation(propertyDelegated) p:kotlin.jvm(propertyDelegated) p:kotlin.io(propertyDelegated)*/D2()

/*p:foo.bar*/val x3 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D3(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D3(propertyDelegated)*/D3()
/*p:foo.bar*/var y3 by /*p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.bar.D3(getValue) p:foo.bar(getValue) p:java.lang(getValue) p:kotlin(getValue) p:kotlin.annotation(getValue) p:kotlin.jvm(getValue) p:kotlin.io(getValue) c:foo.bar.D3(setValue) c:foo.bar.D3(propertyDelegated)*/D3()
