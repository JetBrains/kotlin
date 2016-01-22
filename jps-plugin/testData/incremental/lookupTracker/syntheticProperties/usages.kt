package foo.bar

import /*p:<root>*/JavaClass
import foo./*p:foo*/KotlinClass

/*p:foo.bar*/fun test() {
    val j = JavaClass()
    val k = KotlinClass()

    j./*c:JavaClass*/getFoo()
    j./*c:JavaClass c:JavaClass(getSetFoo) c:JavaClass(getSETFoo) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io*/setFoo(2)
    j./*c:JavaClass c:JavaClass(getFoo) c:JavaClass(getFOO) c:JavaClass(setFoo)*/foo = 2
    j./*c:JavaClass c:JavaClass(getFoo) c:JavaClass(getFOO) c:JavaClass(setFoo)*/foo
    j./*c:JavaClass c:JavaClass(getBar) c:JavaClass(getBAR) c:JavaClass(setBar)*/bar
    j./*c:JavaClass c:JavaClass(getBar) c:JavaClass(getBAR) c:JavaClass(setBar)*/bar = ""
    j./*c:JavaClass c:JavaClass(getBazBaz) c:JavaClass(getBAZBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io*/bazBaz
    j./*c:JavaClass c:JavaClass(getBazBaz) c:JavaClass(getBAZBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io*/bazBaz = ""
    j./*c:JavaClass*/setBoo(2)
    j./*c:JavaClass c:JavaClass(getBoo) c:JavaClass(getBOO) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io*/boo = 2
    k./*c:foo.KotlinClass c:JavaClass*/getFoo() // getFoo may be an inner class in JavaClass
    k./*c:foo.KotlinClass c:JavaClass*/setFoo(2)
    k./*c:foo.KotlinClass c:foo.KotlinClass(getFoo) c:foo.KotlinClass(getFOO) c:foo.KotlinClass(setFoo)*/foo = 2
    k./*c:foo.KotlinClass c:foo.KotlinClass(getFoo) c:foo.KotlinClass(getFOO) c:foo.KotlinClass(setFoo)*/foo
    k./*c:foo.KotlinClass c:foo.KotlinClass(getBar) c:foo.KotlinClass(getBAR) c:foo.KotlinClass(setBar)*/bar
    k./*c:foo.KotlinClass c:foo.KotlinClass(getBar) c:foo.KotlinClass(getBAR) c:foo.KotlinClass(setBar)*/bar = ""
    k./*c:foo.KotlinClass c:foo.KotlinClass(getBazBaz) c:foo.KotlinClass(getBAZBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io c:JavaClass*/bazBaz
    k./*c:foo.KotlinClass c:foo.KotlinClass(getBazBaz) c:foo.KotlinClass(getBAZBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io c:JavaClass*/bazBaz = ""
    k./*c:foo.KotlinClass c:JavaClass*/setBoo(2)
    k./*c:foo.KotlinClass c:foo.KotlinClass(getBoo) c:foo.KotlinClass(getBOO) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io c:JavaClass*/boo = 2
}
