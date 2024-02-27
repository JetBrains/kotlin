package foo.bar

/*p:<root>(JavaClass)*/import JavaClass
/*p:foo(KotlinClass)*/import foo.KotlinClass

/*p:foo.bar*/fun test() {
    val j = /*p:<root>*/JavaClass()
    val k = /*p:foo*/KotlinClass()

    /*p:JavaClass(getFoo)*/j.getFoo()
    /*p:JavaClass(setFoo) p:foo.bar(setFoo)*/j.setFoo(2)
    /*p:JavaClass(foo)*/j.foo = 2
    /*p:JavaClass(foo)*/j.foo
    /*p:JavaClass(bar)*/j.bar
    /*p:JavaClass(bar)*/j.bar = ""
    /*p:JavaClass(bazBaz)*/j.bazBaz
    /*p:JavaClass(bazBaz)*/j.bazBaz = ""
    /*p:JavaClass(setBoo)*/j.setBoo(2)
    /*p:JavaClass(boo) p:foo.bar(boo)*/j.boo = 2
    /*p:foo.KotlinClass(getFoo)*/k.getFoo() // getFoo may be an inner class in JavaClass
    /*p:foo.KotlinClass(setFoo)*/k.setFoo(2)
    /*p:foo.KotlinClass(foo)*/k.foo = 2
    /*p:foo.KotlinClass(foo)*/k.foo
    /*p:JavaClass(bar) p:foo.KotlinClass(bar)*/k.bar
    /*p:JavaClass(bar) p:foo.KotlinClass(bar)*/k.bar = ""
    /*p:JavaClass(bazBaz) p:foo.KotlinClass(bazBaz)*/k.bazBaz
    /*p:JavaClass(bazBaz) p:foo.KotlinClass(bazBaz)*/k.bazBaz = ""
    /*p:JavaClass(setBoo) p:foo.KotlinClass(setBoo)*/k.setBoo(2)
    /*p:foo.KotlinClass(boo) p:foo.bar(boo)*/k.boo = 2
}
