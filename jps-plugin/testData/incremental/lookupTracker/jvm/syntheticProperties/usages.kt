package foo.bar

/*p:<root>(JavaClass)*/import JavaClass
/*p:foo(KotlinClass)*/import foo.KotlinClass

/*p:foo.bar*/fun test() {
    val j = /*p:<root>*/JavaClass()
    val k = /*p:foo*/KotlinClass()

    /*p:<root>(JavaClass) p:kotlin(Int)*/j./*c:JavaClass*/getFoo()
    /*p:<root>(JavaClass)*/j./*c:JavaClass c:JavaClass(getSETFoo) c:JavaClass(getSetFoo) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/setFoo(2)
    /*p:<root>(JavaClass) p:kotlin(Int)*/j./*c:JavaClass c:JavaClass(getFOO) c:JavaClass(getFoo) c:JavaClass(setFoo)*/foo = /*p:kotlin(Int)*/2
    /*p:<root>(JavaClass) p:kotlin(Int)*/j./*c:JavaClass c:JavaClass(getFOO) c:JavaClass(getFoo) c:JavaClass(setFoo)*/foo
    /*p:<root>(JavaClass) p:kotlin(String)*/j./*c:JavaClass c:JavaClass(getBAR) c:JavaClass(getBar) c:JavaClass(setBar)*/bar
    /*p:<root>(JavaClass) p:kotlin(String)*/j./*c:JavaClass c:JavaClass(getBAR) c:JavaClass(getBar) c:JavaClass(setBar)*/bar = /*p:kotlin(String)*/""
    /*p:<root>(JavaClass)*/j./*c:JavaClass c:JavaClass(getBAZBaz) c:JavaClass(getBazBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/bazBaz
    /*p:<root>(JavaClass)*/j./*c:JavaClass c:JavaClass(getBAZBaz) c:JavaClass(getBazBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/bazBaz = /*p:kotlin(String)*/""
    /*p:<root>(JavaClass)*/j./*c:JavaClass*/setBoo(2)
    /*p:<root>(JavaClass)*/j./*c:JavaClass c:JavaClass(getBOO) c:JavaClass(getBoo) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/boo = /*p:kotlin(Int)*/2
    /*p:foo(KotlinClass) p:kotlin(Int)*/k./*c:JavaClass c:foo.KotlinClass*/getFoo() // getFoo may be an inner class in JavaClass
    /*p:foo(KotlinClass)*/k./*c:JavaClass c:foo.KotlinClass*/setFoo(2)
    /*p:foo(KotlinClass) p:kotlin(Int)*/k./*c:foo.KotlinClass c:foo.KotlinClass(getFOO) c:foo.KotlinClass(getFoo) c:foo.KotlinClass(setFoo)*/foo = /*p:kotlin(Int)*/2
    /*p:foo(KotlinClass) p:kotlin(Int)*/k./*c:foo.KotlinClass c:foo.KotlinClass(getFOO) c:foo.KotlinClass(getFoo) c:foo.KotlinClass(setFoo)*/foo
    /*p:foo(KotlinClass) p:kotlin(String)*/k./*c:foo.KotlinClass c:foo.KotlinClass(getBAR) c:foo.KotlinClass(getBar) c:foo.KotlinClass(setBar)*/bar
    /*p:foo(KotlinClass) p:kotlin(String)*/k./*c:foo.KotlinClass c:foo.KotlinClass(getBAR) c:foo.KotlinClass(getBar) c:foo.KotlinClass(setBar)*/bar = /*p:kotlin(String)*/""
    /*p:foo(KotlinClass)*/k./*c:JavaClass c:foo.KotlinClass c:foo.KotlinClass(getBAZBaz) c:foo.KotlinClass(getBazBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/bazBaz
    /*p:foo(KotlinClass)*/k./*c:JavaClass c:foo.KotlinClass c:foo.KotlinClass(getBAZBaz) c:foo.KotlinClass(getBazBaz) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/bazBaz = /*p:kotlin(String)*/""
    /*p:foo(KotlinClass)*/k./*c:JavaClass c:foo.KotlinClass*/setBoo(2)
    /*p:foo(KotlinClass)*/k./*c:JavaClass c:foo.KotlinClass c:foo.KotlinClass(getBOO) c:foo.KotlinClass(getBoo) p:foo.bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/boo = /*p:kotlin(Int)*/2
}
