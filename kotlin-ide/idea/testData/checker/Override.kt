package override

    interface MyTrait {
        fun foo()
    }

    abstract class MyAbstractClass {
        abstract fun bar()
    }

    open class MyClass : MyTrait, MyAbstractClass() {
        override fun foo() {}
        override fun bar() {}
    }

    class MyChildClass : MyClass() {}

    <error>class MyIllegalClass</error> : MyTrait, MyAbstractClass() {}

    <error>class MyIllegalClass2</error> : MyTrait, MyAbstractClass() {
        override fun foo() {}
    }

    <error>class MyIllegalClass3</error> : MyTrait, MyAbstractClass() {
        override fun bar() {}
    }

    <error>class MyIllegalClass4</error> : MyTrait, MyAbstractClass() {
        fun <error>foo</error>() {}
        <error>override</error> fun other() {}
    }

    class MyChildClass1 : MyClass() {
        fun <error>foo</error>() {}
        override fun bar() {}
    }
