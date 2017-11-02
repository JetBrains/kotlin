// FILE: test/JavaClass.java
package test;

class JavaClass {
    class Foo {
        class Bar {}
    }
}

//FILE: test/J$B.java
package test;

public class J$B {
    public class C {}

    public class D$E {
        class F {}
        class F$G {}
    }

    public class D$$E {}
    public class D$$$E {}
}

// FILE: a.kt
package test

interface IFoo {
    interface IBar {
        annotation class Anno(vararg val value: kotlin.reflect.KClass<*>)

        @Anno(IZoo::class)
        interface IZoo
    }
}

class Experiment {
    annotation class Type

    @Type
    data class Group(s: String)
}

class Foo {
    open class Bar {
        object Zoo
    }
}

class `A$B` {
    class C

    @JvmField lateinit var c: C
    @JvmField lateinit var de: `D$E`

    @JvmField lateinit var jc: `J$B`.C
    @JvmField lateinit var jde: `J$B`.`D$E`

    class `D$E` {
        class F
        class `F$G`

        @JvmField lateinit var f: F
        @JvmField lateinit var fg: `F$G`

        @JvmField lateinit var jf: `J$B`.`D$E`.F
        @JvmField lateinit var jfg: `J$B`.`D$E`.`F$G`
    }

    class `D$$E`
    class `D$$$E`

    @JvmField lateinit var dee: `D$$E`
    @JvmField lateinit var deee: `D$$$E`

    @JvmField lateinit var jdee: `J$B`.`D$$E`
    @JvmField lateinit var jdeee: `J$B`.`D$$$E`
}

@IFoo.IBar.Anno(IFoo.IBar.IZoo::class, Foo.Bar::class)
class Test1(val zoo: Foo.Bar.Zoo) : Foo.Bar(), IFoo.IBar, IFoo.IBar.IZoo {
    fun a(): Thread.State = Thread.State.NEW
    fun b(foo: JavaClass.Foo, bar: JavaClass.Foo.Bar) {}
}