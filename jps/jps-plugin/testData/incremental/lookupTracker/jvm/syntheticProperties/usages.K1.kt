package foo.bar

/*p:<root>(JavaClass)*/import JavaClass
/*p:foo(KotlinClass)*/import foo.KotlinClass

/*p:foo.bar*/fun test() {
    val j = /*p:<root>*/JavaClass()
    val k = /*p:foo*/KotlinClass()

    /*p:<root>(JavaClass)*/j./*p:JavaClass*/getFoo()
    /*p:<root>(JavaClass)*/j./*p:JavaClass p:JavaClass(getSETFoo) p:JavaClass(getSetFoo) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/setFoo(2)
    /*p:<root>(JavaClass)*/j./*p:JavaClass p:JavaClass(getFOO) p:JavaClass(getFoo) p:JavaClass(setFoo)*/foo = 2
    /*p:<root>(JavaClass)*/j./*p:JavaClass p:JavaClass(getFOO) p:JavaClass(getFoo) p:JavaClass(setFoo)*/foo
    /*p:<root>(JavaClass)*/j./*p:JavaClass p:JavaClass(getBAR) p:JavaClass(getBar) p:JavaClass(setBar)*/bar
    /*p:<root>(JavaClass)*/j./*p:JavaClass p:JavaClass(getBAR) p:JavaClass(getBar) p:JavaClass(setBar)*/bar = ""
    /*p:<root>(JavaClass)*/j./*p:JavaClass p:JavaClass(getBAZBaz) p:JavaClass(getBazBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/bazBaz
    /*p:<root>(JavaClass)*/j./*p:JavaClass p:JavaClass(getBAZBaz) p:JavaClass(getBazBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/bazBaz = ""
    /*p:<root>(JavaClass)*/j./*p:JavaClass*/setBoo(2)
    /*p:<root>(JavaClass)*/j./*p:JavaClass p:JavaClass(getBOO) p:JavaClass(getBoo) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/boo = 2
    /*p:foo(KotlinClass)*/k./*p:JavaClass p:foo.KotlinClass*/getFoo() // getFoo may be an inner class in JavaClass
    /*p:foo(KotlinClass)*/k./*p:JavaClass p:foo.KotlinClass*/setFoo(2)
    /*p:foo(KotlinClass)*/k./*p:foo.KotlinClass p:foo.KotlinClass(getFOO) p:foo.KotlinClass(getFoo) p:foo.KotlinClass(setFoo)*/foo = 2
    /*p:foo(KotlinClass)*/k./*p:foo.KotlinClass p:foo.KotlinClass(getFOO) p:foo.KotlinClass(getFoo) p:foo.KotlinClass(setFoo)*/foo
    /*p:foo(KotlinClass)*/k./*p:foo.KotlinClass p:foo.KotlinClass(getBAR) p:foo.KotlinClass(getBar) p:foo.KotlinClass(setBar)*/bar
    /*p:foo(KotlinClass)*/k./*p:foo.KotlinClass p:foo.KotlinClass(getBAR) p:foo.KotlinClass(getBar) p:foo.KotlinClass(setBar)*/bar = ""
    /*p:foo(KotlinClass)*/k./*p:JavaClass p:foo.KotlinClass p:foo.KotlinClass(getBAZBaz) p:foo.KotlinClass(getBazBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/bazBaz
    /*p:foo(KotlinClass)*/k./*p:JavaClass p:foo.KotlinClass p:foo.KotlinClass(getBAZBaz) p:foo.KotlinClass(getBazBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/bazBaz = ""
    /*p:foo(KotlinClass)*/k./*p:JavaClass p:foo.KotlinClass*/setBoo(2)
    /*p:foo(KotlinClass)*/k./*p:JavaClass p:foo.KotlinClass p:foo.KotlinClass(getBOO) p:foo.KotlinClass(getBoo) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/boo = 2
}
