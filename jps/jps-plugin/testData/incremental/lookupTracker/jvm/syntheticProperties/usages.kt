package foo.bar

/*p:<root>(JavaClass)*/import JavaClass
/*p:foo(KotlinClass)*/import foo.KotlinClass

/*p:foo.bar*/fun test() {
    val j = /*p:<root> p:foo*/JavaClass()
    val k = /*p:<root> p:foo*/KotlinClass()

    /*p:JavaClass(getFoo) p:kotlin(Int)*/j.getFoo()
    /*p:<root>(setFoo) p:JavaClass(setFoo) p:foo(setFoo) p:foo.bar(setFoo)*/j.setFoo(2)
    /*p:JavaClass(foo) p:kotlin(Int)*/j.foo = 2
    /*p:JavaClass(foo) p:kotlin(Int)*/j.foo
    /*p:JavaClass(bar)*/j.bar
    /*p:JavaClass(bar)*/j.bar = ""
    /*p:JavaClass(bazBaz) p:kotlin(Int)*/j.bazBaz
    /*p:JavaClass(bazBaz) p:kotlin(Int)*/j.bazBaz = ""
    /*p:JavaClass(setBoo) p:kotlin(Unit)*/j.setBoo(2)
    /*p:<root>(boo) p:JavaClass(boo) p:foo(boo) p:foo.bar(boo)*/j.boo = 2
    /*p:KotlinClass(getFoo) p:foo.KotlinClass(getFoo) p:kotlin(Int)*/k.getFoo() // getFoo may be an inner class in JavaClass
    /*p:KotlinClass(setFoo) p:foo.KotlinClass(setFoo) p:kotlin(Unit)*/k.setFoo(2)
    /*p:KotlinClass(foo) p:foo.KotlinClass(foo) p:kotlin(Int)*/k.foo = 2
    /*p:KotlinClass(foo) p:foo.KotlinClass(foo) p:kotlin(Int)*/k.foo
    /*p:JavaClass(bar) p:foo.KotlinClass(bar)*/k.bar
    /*p:JavaClass(bar) p:foo.KotlinClass(bar)*/k.bar = ""
    /*p:JavaClass(bazBaz) p:foo.KotlinClass(bazBaz) p:kotlin(Int)*/k.bazBaz
    /*p:JavaClass(bazBaz) p:foo.KotlinClass(bazBaz) p:kotlin(Int)*/k.bazBaz = ""
    /*p:JavaClass(setBoo) p:foo.KotlinClass(setBoo) p:kotlin(Unit)*/k.setBoo(2)
    /*p:<root>(boo) p:foo(boo) p:foo.KotlinClass(boo) p:foo.bar(boo)*/k.boo = 2
}
