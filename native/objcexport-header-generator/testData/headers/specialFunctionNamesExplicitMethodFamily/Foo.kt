class Foo {
    var init = 42
    var _init = 42
    var initializer = 42
    var initWith = 42
    var initOther = 42
    fun alloc() {}
    fun allocation() {}
    fun allocWith(x: Int) {}
    fun copy(): Foo = Foo()
    fun copymachine(): Foo = Foo()
    fun copyWith(x: Int): Foo = Foo()
    fun init(): Foo = Foo()
    fun initializer(): Foo = Foo()
    fun initWith(x: Int): Foo = Foo()
    fun initOther(x: Int): Foo = Foo()
    fun _init(): Foo = Foo()
    fun _initializer(): Foo = Foo()
    fun _initWith(x: Int): Foo = Foo()
    fun _initOther(x: Int): Foo = Foo()
    fun __init(): Foo = Foo()
    fun __initializer(): Foo = Foo()
    fun __initWith(x: Int): Foo = Foo()
    fun __initOther(x: Int): Foo = Foo()
    fun mutableCopy(): Foo = Foo()
    fun mutableCopymachine(): Foo = Foo()
    fun mutableCopyWith(x: Int): Foo = Foo()
    fun new(): Foo = Foo()
    fun newer(): Foo = Foo()
    fun newWith(x: Int): Foo = Foo()
}