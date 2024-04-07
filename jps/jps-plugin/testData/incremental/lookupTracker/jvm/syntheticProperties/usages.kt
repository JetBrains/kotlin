package foo.bar

/*p:<root>(JavaClass)*/import JavaClass
/*p:foo(KotlinClass)*/import foo.KotlinClass

/*p:foo.bar*/fun test() {
    val j = /*p:<root>*/JavaClass()
    val k = /*p:foo*/KotlinClass()

    /*p:<root>(JavaClass) p:JavaClass(getFoo)*/j.getFoo()
    /*p:<root>(JavaClass) p:JavaClass(setFoo) p:foo.bar(setFoo)*/j.setFoo(2)
    /*p:<root>(JavaClass) p:JavaClass(foo)*/j.foo = 2
    /*p:<root>(JavaClass) p:JavaClass(foo)*/j.foo
    /*p:<root>(JavaClass) p:JavaClass(bar)*/j.bar
    /*p:<root>(JavaClass) p:JavaClass(bar)*/j.bar = ""
    /*p:<root>(JavaClass) p:JavaClass(bazBaz)*/j.bazBaz
    /*p:<root>(JavaClass) p:JavaClass(bazBaz)*/j.bazBaz = ""
    /*p:<root>(JavaClass) p:JavaClass(setBoo)*/j.setBoo(2)
    /*p:<root>(JavaClass) p:JavaClass(boo) p:foo.bar(boo)*/j.boo = 2
    /*p:foo(KotlinClass) p:foo.KotlinClass(getFoo)*/k.getFoo() // getFoo may be an inner class in JavaClass
    /*p:foo(KotlinClass) p:foo.KotlinClass(setFoo)*/k.setFoo(2)
    /*p:foo(KotlinClass) p:foo.KotlinClass(foo)*/k.foo = 2
    /*p:foo(KotlinClass) p:foo.KotlinClass(foo)*/k.foo
    /*p:JavaClass(bar) p:foo(KotlinClass) p:foo.KotlinClass(bar)*/k.bar
    /*p:JavaClass(bar) p:foo(KotlinClass) p:foo.KotlinClass(bar)*/k.bar = ""
    /*p:JavaClass(bazBaz) p:foo(KotlinClass) p:foo.KotlinClass(bazBaz)*/k.bazBaz
    /*p:JavaClass(bazBaz) p:foo(KotlinClass) p:foo.KotlinClass(bazBaz)*/k.bazBaz = ""
    /*p:JavaClass(setBoo) p:foo(KotlinClass) p:foo.KotlinClass(setBoo)*/k.setBoo(2)
    /*p:foo(KotlinClass) p:foo.KotlinClass(boo) p:foo.bar(boo)*/k.boo = 2
}
