package custom.pkg

annotation class A

class Foo {
    val simple = 0

    private val privateSimple = 0

    protected val protectedSimple = 0

    var privateSetter = 0
        private set

    @A val annotated = 0

    val annotatedGetter = 0
        @A get

    var annotatedSetter = 0
        @A set

    var annotatedAccessors = 0
        @A set
        @A get
}
