package foo.bar

import /*p:<root>*/JavaClass
import foo./*p:foo*/KotlinClass

/*p:foo.bar*/fun test() {
    val j = /*p:foo.bar*/JavaClass()
    val k = /*p:foo.bar*/KotlinClass()

    j.getFoo()
    j./*p:foo.bar c:JavaClass(getSetFoo) c:JavaClass(getSETFoo)*/setFoo(2)
    j./*p:foo.bar c:JavaClass(getFoo) c:JavaClass(getFOO) c:JavaClass(setFoo)*/foo = 2
    j./*p:foo.bar c:JavaClass(getFoo) c:JavaClass(getFOO) c:JavaClass(setFoo)*/foo
    j./*p:foo.bar c:JavaClass(getBar) c:JavaClass(getBAR) c:JavaClass(setBar)*/bar
    j./*p:foo.bar c:JavaClass(getBar) c:JavaClass(getBAR) c:JavaClass(setBar)*/bar = ""
    j./*p:foo.bar c:JavaClass(getBazBaz) c:JavaClass(getBAZBaz)*/bazBaz
    j./*p:foo.bar c:JavaClass(getBazBaz) c:JavaClass(getBAZBaz)*/bazBaz = ""
    j.setBoo(2)
    j./*p:foo.bar c:JavaClass(getBoo) c:JavaClass(getBOO)*/boo = 2
    k./*c:foo.KotlinClass*/getFoo()
    k./*c:foo.KotlinClass*/setFoo(2)
    k./*c:foo.KotlinClass p:foo.bar c:foo.KotlinClass(getFoo) c:foo.KotlinClass(getFOO) c:foo.KotlinClass(setFoo)*/foo = 2
    k./*c:foo.KotlinClass p:foo.bar c:foo.KotlinClass(getFoo) c:foo.KotlinClass(getFOO) c:foo.KotlinClass(setFoo)*/foo
    k./*c:foo.KotlinClass p:foo.bar c:foo.KotlinClass(getBar) c:foo.KotlinClass(getBAR) c:foo.KotlinClass(setBar)*/bar
    k./*c:foo.KotlinClass p:foo.bar c:foo.KotlinClass(getBar) c:foo.KotlinClass(getBAR) c:foo.KotlinClass(setBar)*/bar = ""
    k./*c:foo.KotlinClass p:foo.bar c:foo.KotlinClass(getBazBaz) c:foo.KotlinClass(getBAZBaz)*/bazBaz
    k./*c:foo.KotlinClass p:foo.bar c:foo.KotlinClass(getBazBaz) c:foo.KotlinClass(getBAZBaz)*/bazBaz = ""
    k./*c:foo.KotlinClass*/setBoo(2)
    k./*c:foo.KotlinClass p:foo.bar c:foo.KotlinClass(getBoo) c:foo.KotlinClass(getBOO)*/boo = 2
}
