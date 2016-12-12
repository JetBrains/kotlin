package foo.bar

import kotlin.reflect./*p:kotlin.reflect*/KProperty

/*p:foo.bar*/class D1 {
    operator fun getValue(t: /*c:foo.bar.D1 p:foo.bar*/Any?, p: /*c:foo.bar.D1*/KProperty<*>) = /*p:kotlin(Int)*/1
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D1.setValue(t: /*p:foo.bar*/Any?, p: KProperty<*>, v: /*p:foo.bar*/Int) {}

/*p:foo.bar(D2)*/open class D2 {
    operator fun setValue(t: /*c:foo.bar.D2 p:foo.bar*/Any?, p: /*c:foo.bar.D2*/KProperty<*>, v: /*c:foo.bar.D2 p:foo.bar*/Int) {}
}

/*p:foo.bar*/operator fun /*p:foo.bar*/D2.getValue(t: /*p:foo.bar*/Any?, p: KProperty<*>) = /*p:kotlin(Int)*/1
/*p:foo.bar*/fun /*p:foo.bar*/D2.propertyDelegated(p: /*p:foo.bar*/Any?) {}

/*p:foo.bar*/class D3 : /*p:foo.bar*/D2() {
    fun propertyDelegated(p: /*c:foo.bar.D3 c:foo.bar.D2 p:foo.bar*/Any?) {}
}


/*p:foo.bar*/val x1 by /*p:foo.bar c:foo.bar.D1(toDelegateFor) c:foo.bar.D1(getToDelegateFor) c:foo.bar.D1(getTODelegateFor) p:foo.bar(toDelegateFor) p:kotlin(toDelegateFor) p:kotlin.annotation(toDelegateFor) p:kotlin.collections(toDelegateFor) p:kotlin.coroutines(toDelegateFor) p:kotlin.ranges(toDelegateFor) p:kotlin.sequences(toDelegateFor) p:kotlin.text(toDelegateFor) p:java.lang(toDelegateFor) p:kotlin.jvm(toDelegateFor) p:kotlin.io(toDelegateFor) c:foo.bar.D1(getValue)*/D1()
/*p:foo.bar*/var y1 by /*p:foo.bar c:foo.bar.D1(toDelegateFor) c:foo.bar.D1(getToDelegateFor) c:foo.bar.D1(getTODelegateFor) p:foo.bar(toDelegateFor) p:kotlin(toDelegateFor) p:kotlin.annotation(toDelegateFor) p:kotlin.collections(toDelegateFor) p:kotlin.coroutines(toDelegateFor) p:kotlin.ranges(toDelegateFor) p:kotlin.sequences(toDelegateFor) p:kotlin.text(toDelegateFor) p:java.lang(toDelegateFor) p:kotlin.jvm(toDelegateFor) p:kotlin.io(toDelegateFor) c:foo.bar.D1(getValue) c:foo.bar.D1(setValue) c:foo.bar.D1(getSetValue) c:foo.bar.D1(getSETValue) p:foo.bar(setValue)*/D1()

/*p:foo.bar*/val x2 by /*p:foo.bar c:foo.bar.D2(toDelegateFor) c:foo.bar.D2(getToDelegateFor) c:foo.bar.D2(getTODelegateFor) p:foo.bar(toDelegateFor) p:kotlin(toDelegateFor) p:kotlin.annotation(toDelegateFor) p:kotlin.collections(toDelegateFor) p:kotlin.coroutines(toDelegateFor) p:kotlin.ranges(toDelegateFor) p:kotlin.sequences(toDelegateFor) p:kotlin.text(toDelegateFor) p:java.lang(toDelegateFor) p:kotlin.jvm(toDelegateFor) p:kotlin.io(toDelegateFor) c:foo.bar.D2(getValue) c:foo.bar.D2(getGetValue) c:foo.bar.D2(getGETValue) p:foo.bar(getValue)*/D2()
/*p:foo.bar*/var y2 by /*p:foo.bar c:foo.bar.D2(toDelegateFor) c:foo.bar.D2(getToDelegateFor) c:foo.bar.D2(getTODelegateFor) p:foo.bar(toDelegateFor) p:kotlin(toDelegateFor) p:kotlin.annotation(toDelegateFor) p:kotlin.collections(toDelegateFor) p:kotlin.coroutines(toDelegateFor) p:kotlin.ranges(toDelegateFor) p:kotlin.sequences(toDelegateFor) p:kotlin.text(toDelegateFor) p:java.lang(toDelegateFor) p:kotlin.jvm(toDelegateFor) p:kotlin.io(toDelegateFor) c:foo.bar.D2(getValue) c:foo.bar.D2(getGetValue) c:foo.bar.D2(getGETValue) p:foo.bar(getValue) c:foo.bar.D2(setValue)*/D2()

/*p:foo.bar*/val x3 by /*p:foo.bar c:foo.bar.D3(toDelegateFor) c:foo.bar.D2(toDelegateFor) c:foo.bar.D3(getToDelegateFor) c:foo.bar.D3(getTODelegateFor) p:foo.bar(toDelegateFor) p:kotlin(toDelegateFor) p:kotlin.annotation(toDelegateFor) p:kotlin.collections(toDelegateFor) p:kotlin.coroutines(toDelegateFor) p:kotlin.ranges(toDelegateFor) p:kotlin.sequences(toDelegateFor) p:kotlin.text(toDelegateFor) p:java.lang(toDelegateFor) p:kotlin.jvm(toDelegateFor) p:kotlin.io(toDelegateFor) c:foo.bar.D3(getValue) c:foo.bar.D2(getValue) c:foo.bar.D3(getGetValue) c:foo.bar.D3(getGETValue) p:foo.bar(getValue)*/D3()
/*p:foo.bar*/var y3 by /*p:foo.bar c:foo.bar.D3(toDelegateFor) c:foo.bar.D2(toDelegateFor) c:foo.bar.D3(getToDelegateFor) c:foo.bar.D3(getTODelegateFor) p:foo.bar(toDelegateFor) p:kotlin(toDelegateFor) p:kotlin.annotation(toDelegateFor) p:kotlin.collections(toDelegateFor) p:kotlin.coroutines(toDelegateFor) p:kotlin.ranges(toDelegateFor) p:kotlin.sequences(toDelegateFor) p:kotlin.text(toDelegateFor) p:java.lang(toDelegateFor) p:kotlin.jvm(toDelegateFor) p:kotlin.io(toDelegateFor) c:foo.bar.D3(getValue) c:foo.bar.D2(getValue) c:foo.bar.D3(getGetValue) c:foo.bar.D3(getGETValue) p:foo.bar(getValue) c:foo.bar.D3(setValue) c:foo.bar.D2(setValue)*/D3()
