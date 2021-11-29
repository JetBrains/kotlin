// DONT_TARGET_EXACT_BACKEND: JS
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP

// This test file verifies the boxing rules for value classes when instances of those classes are passed
// through language boundaries in external declarations.

package foo

data class Foo(val s: String)

external class NativeFoo(s: String) {
    val s: String

    override fun toString(): String
}

external fun describeValueOfProperty(o: dynamic, name: String): String
external fun nullifyTestProperties(o: dynamic): Unit

/* Value classes under test */
value class IntWrapper(val value: Int)
value class IntNWrapper(val value: Int?)
value class FooWrapper(val foo: Foo)
value class FooNWrapper(val fooN: Foo?)
value class NativeFooWrapper(val nativeFoo: NativeFoo)
value class NativeFooNWrapper(val nativeFooN: NativeFoo?)

external fun describeIntWrapper(x: IntWrapper): String
external fun describeIntWrapperN(x: IntWrapper?): String
external fun describeIntNWrapper(x: IntNWrapper): String
external fun describeIntNWrapperN(x: /*boxed*/ IntNWrapper?): String
external fun describeFooWrapper(x: FooWrapper): String
external fun describeFooWrapperN(x: FooWrapper?): String
external fun describeFooNWrapper(x: FooNWrapper): String
external fun describeFooNWrapperN(x: /*boxed*/ FooNWrapper?): String
external fun describeNativeFooWrapper(x: NativeFooWrapper): String
external fun describeNativeFooWrapperN(x: NativeFooWrapper?): String
external fun describeNativeFooNWrapper(x: NativeFooNWrapper): String
external fun describeNativeFooNWrapperN(x: /*boxed*/ NativeFooNWrapper?): String

fun testFreeFunctionsWithValueClassesInArgs() {
    assertEquals("42 (number)", describeIntWrapper(IntWrapper(42)))
    assertEquals("100 (number)", describeIntWrapperN(IntWrapper(100)))
    assertEquals("null (object)", describeIntWrapperN(null))

    assertEquals("42 (number)", describeIntNWrapper(IntNWrapper(42)))
    assertEquals("null (object)", describeIntNWrapper(IntNWrapper(null)))
    assertEquals("IntNWrapper(value=100) (object)", describeIntNWrapperN(IntNWrapper(100)))
    assertEquals("IntNWrapper(value=null) (object)", describeIntNWrapperN(IntNWrapper(null)))
    assertEquals("null (object)", describeIntNWrapperN(null))

    assertEquals("Foo(s=hello) (object)", describeFooWrapper(FooWrapper(Foo("hello"))))
    assertEquals("Foo(s=goodbye) (object)", describeFooWrapperN(FooWrapper(Foo("goodbye"))))
    assertEquals("null (object)", describeFooWrapperN(null))

    assertEquals("Foo(s=hello) (object)", describeFooNWrapper(FooNWrapper(Foo("hello"))))
    assertEquals("null (object)", describeFooNWrapper(FooNWrapper(null)))
    assertEquals("FooNWrapper(fooN=Foo(s=goodbye)) (object)", describeFooNWrapperN(FooNWrapper(Foo("goodbye"))))
    assertEquals("FooNWrapper(fooN=null) (object)", describeFooNWrapperN(FooNWrapper(null)))
    assertEquals("null (object)", describeFooNWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", describeNativeFooWrapper(NativeFooWrapper(NativeFoo("hello"))))
    assertEquals("NativeFoo('goodbye') (object)", describeNativeFooWrapperN(NativeFooWrapper(NativeFoo("goodbye"))))
    assertEquals("null (object)", describeNativeFooWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", describeNativeFooNWrapper(NativeFooNWrapper(NativeFoo("hello"))))
    assertEquals("null (object)", describeNativeFooNWrapper(NativeFooNWrapper(null)))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('goodbye')) (object)", describeNativeFooNWrapperN(NativeFooNWrapper(NativeFoo("goodbye"))))
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", describeNativeFooNWrapperN(NativeFooNWrapper(null)))
    assertEquals("null (object)", describeNativeFooNWrapperN(null))
}

external var intWrapper: IntWrapper
external var intWrapperN: IntWrapper?
external var intNWrapper: IntNWrapper
external var intNWrapperN: /*boxed*/ IntNWrapper?
external var fooWrapper: FooWrapper
external var fooWrapperN: FooWrapper?
external var fooNWrapper: FooNWrapper
external var fooNWrapperN: /*boxed*/ FooNWrapper?
external var nativeFooWrapper: NativeFooWrapper
external var nativeFooWrapperN: NativeFooWrapper?
external var nativeFooNWrapper: NativeFooNWrapper
external var nativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

fun testWritableGlobalProperties() {
    assertEquals("null (object)", describeValueOfProperty(null, "intWrapper"))
    intWrapper = IntWrapper(42)
    assertEquals("42 (number)", describeValueOfProperty(null, "intWrapper"))
    assertEquals(42, intWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(null, "intWrapperN"))
    intWrapperN = IntWrapper(100)
    assertEquals("100 (number)", describeValueOfProperty(null, "intWrapperN"))
    assertEquals(100, intWrapperN?.value)
    intWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(null, "intWrapperN"))
    assertEquals(null, intWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(null, "intNWrapper"))
    intNWrapper = IntNWrapper(23)
    assertEquals("23 (number)", describeValueOfProperty(null, "intNWrapper"))
    assertEquals(23, intNWrapper.value)
    intNWrapper = IntNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(null, "intNWrapper"))
    assertEquals(null, intNWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(null, "intNWrapperN"))
    intNWrapperN = IntNWrapper(65)
    assertEquals("IntNWrapper(value=65) (object)", describeValueOfProperty(null, "intNWrapperN"))
    assertEquals(65, intNWrapperN?.value)
    intNWrapperN = IntNWrapper(null)
    assertEquals("IntNWrapper(value=null) (object)", describeValueOfProperty(null, "intNWrapperN"))
    assertEquals(null, intNWrapperN?.value)
    intNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(null, "intNWrapperN"))
    assertEquals(null, intNWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(null, "fooWrapper"))
    fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("Foo(s=cat) (object)", describeValueOfProperty(null, "fooWrapper"))
    assertEquals("cat", fooWrapper.foo.s)

    assertEquals("null (object)", describeValueOfProperty(null, "fooWrapperN"))
    fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("Foo(s=dog) (object)", describeValueOfProperty(null, "fooWrapperN"))
    assertEquals("dog", fooWrapperN?.foo?.s)
    fooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(null, "fooWrapperN"))
    assertEquals(null, fooWrapperN?.foo?.s)

    assertEquals("null (object)", describeValueOfProperty(null, "fooNWrapper"))
    fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("Foo(s=mouse) (object)", describeValueOfProperty(null, "fooNWrapper"))
    assertEquals("mouse", fooNWrapper.fooN?.s)
    fooNWrapper = FooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(null, "fooNWrapper"))
    assertEquals(null, fooNWrapper.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(null, "fooNWrapperN"))
    fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("FooNWrapper(fooN=Foo(s=fox)) (object)", describeValueOfProperty(null, "fooNWrapperN"))
    assertEquals("fox", fooNWrapperN?.fooN?.s)
    fooNWrapperN = FooNWrapper(null)
    assertEquals("FooNWrapper(fooN=null) (object)", describeValueOfProperty(null, "fooNWrapperN"))
    assertEquals(null, fooNWrapperN?.fooN?.s)
    fooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(null, "fooNWrapperN"))
    assertEquals(null, fooNWrapperN?.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(null, "nativeFooWrapper"))
    nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("NativeFoo('Berlin') (object)", describeValueOfProperty(null, "nativeFooWrapper"))
    assertEquals("Berlin", nativeFooWrapper.nativeFoo.s)

    assertEquals("null (object)", describeValueOfProperty(null, "nativeFooWrapperN"))
    nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("NativeFoo('Amsterdam') (object)", describeValueOfProperty(null, "nativeFooWrapperN"))
    assertEquals("Amsterdam", nativeFooWrapperN?.nativeFoo?.s)
    nativeFooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(null, "nativeFooWrapperN"))
    assertEquals(null, nativeFooWrapperN?.nativeFoo?.s)

    assertEquals("null (object)", describeValueOfProperty(null, "nativeFooNWrapper"))
    nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("NativeFoo('Saint-Petersburg') (object)", describeValueOfProperty(null, "nativeFooNWrapper"))
    assertEquals("Saint-Petersburg", nativeFooNWrapper.nativeFooN?.s)
    nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(null, "nativeFooNWrapper"))
    assertEquals(null, nativeFooNWrapper.nativeFooN?.s)

    assertEquals("null (object)", describeValueOfProperty(null, "nativeFooNWrapperN"))
    nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('Boston')) (object)", describeValueOfProperty(null, "nativeFooNWrapperN"))
    assertEquals("Boston", nativeFooNWrapperN?.nativeFooN?.s)
    nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", describeValueOfProperty(null, "nativeFooNWrapperN"))
    assertEquals(null, nativeFooNWrapperN?.nativeFooN?.s)
    nativeFooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(null, "nativeFooNWrapperN"))
    assertEquals(null, nativeFooNWrapperN?.nativeFooN?.s)

    nullifyTestProperties(null)
}

external val readOnlyIntWrapper: IntWrapper
external val readOnlyIntWrapperN: IntWrapper?
external val readOnlyIntNWrapper: IntNWrapper
external val readOnlyIntNWrapperN: /*boxed*/ IntNWrapper?
external val readOnlyFooWrapper: FooWrapper
external val readOnlyFooWrapperN: FooWrapper?
external val readOnlyFooNWrapper: FooNWrapper
external val readOnlyFooNWrapperN: /*boxed*/ FooNWrapper?
external val readOnlyNativeFooWrapper: NativeFooWrapper
external val readOnlyNativeFooWrapperN: NativeFooWrapper?
external val readOnlyNativeFooNWrapper: NativeFooNWrapper
external val readOnlyNativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

fun testReadOnlyGlobalProperties() {
    intWrapper = IntWrapper(42)
    assertEquals(42, readOnlyIntWrapper.value)

    assertEquals(null, readOnlyIntWrapperN?.value)
    intWrapperN = IntWrapper(100)
    assertEquals(100, readOnlyIntWrapperN?.value)

    intNWrapper = IntNWrapper(23)
    assertEquals(23, readOnlyIntNWrapper.value)
    intNWrapper = IntNWrapper(null)
    assertEquals(null, readOnlyIntNWrapper.value)

    assertEquals(null, readOnlyIntNWrapperN?.value)
    intNWrapperN = IntNWrapper(65)
    assertEquals(65, readOnlyIntNWrapperN?.value)
    intNWrapperN = IntNWrapper(null)
    assertEquals(null, readOnlyIntNWrapperN?.value)

    fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", readOnlyFooWrapper.foo.s)

    assertEquals(null, readOnlyFooWrapperN?.foo?.s)
    fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", readOnlyFooWrapperN?.foo?.s)

    fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", readOnlyFooNWrapper.fooN?.s)
    fooNWrapper = FooNWrapper(null)
    assertEquals(null, readOnlyFooNWrapper.fooN?.s)

    assertEquals(null, readOnlyFooNWrapperN?.fooN?.s)
    fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", readOnlyFooNWrapperN?.fooN?.s)
    fooNWrapperN = FooNWrapper(null)
    assertEquals(null, readOnlyFooNWrapperN?.fooN?.s)

    nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", readOnlyNativeFooWrapper.nativeFoo.s)

    assertEquals(null, readOnlyNativeFooWrapperN?.nativeFoo?.s)
    nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", readOnlyNativeFooWrapperN?.nativeFoo?.s)

    nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", readOnlyNativeFooNWrapper.nativeFooN?.s)
    nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, readOnlyNativeFooNWrapper.nativeFooN?.s)

    assertEquals(null, readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, readOnlyNativeFooNWrapperN?.nativeFooN?.s)

    nullifyTestProperties(null)
}

external fun getIntWrapper(): IntWrapper
external fun getIntWrapperN(): IntWrapper?
external fun getIntNWrapper(): IntNWrapper
external fun getIntNWrapperN(): /*boxed*/ IntNWrapper?
external fun getFooWrapper(): FooWrapper
external fun getFooWrapperN(): FooWrapper?
external fun getFooNWrapper(): FooNWrapper
external fun getFooNWrapperN(): /*boxed*/ FooNWrapper?
external fun getNativeFooWrapper(): NativeFooWrapper
external fun getNativeFooWrapperN(): NativeFooWrapper?
external fun getNativeFooNWrapper(): NativeFooNWrapper
external fun getNativeFooNWrapperN(): /*boxed*/ NativeFooNWrapper?

fun testFreeFunctionsWithValueClassInReturnType() {
    intWrapper = IntWrapper(42)
    assertEquals(42, getIntWrapper().value)

    assertEquals(null, getIntWrapperN()?.value)
    intWrapperN = IntWrapper(100)
    assertEquals(100, getIntWrapperN()?.value)

    intNWrapper = IntNWrapper(23)
    assertEquals(23, getIntNWrapper().value)
    intNWrapper = IntNWrapper(null)
    assertEquals(null, getIntNWrapper().value)

    assertEquals(null, getIntNWrapperN()?.value)
    intNWrapperN = IntNWrapper(65)
    assertEquals(65, getIntNWrapperN()?.value)
    intNWrapperN = IntNWrapper(null)
    assertEquals(null, getIntNWrapperN()?.value)

    fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", getFooWrapper().foo.s)

    assertEquals(null, getFooWrapperN()?.foo?.s)
    fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", getFooWrapperN()?.foo?.s)

    fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", getFooNWrapper().fooN?.s)
    fooNWrapper = FooNWrapper(null)
    assertEquals(null, getFooNWrapper().fooN?.s)

    assertEquals(null, getFooNWrapperN()?.fooN?.s)
    fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", getFooNWrapperN()?.fooN?.s)
    fooNWrapperN = FooNWrapper(null)
    assertEquals(null, getFooNWrapperN()?.fooN?.s)

    nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", getNativeFooWrapper().nativeFoo.s)

    assertEquals(null, getNativeFooWrapperN()?.nativeFoo?.s)
    nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", getNativeFooWrapperN()?.nativeFoo?.s)

    nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", getNativeFooNWrapper().nativeFooN?.s)
    nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, getNativeFooNWrapper().nativeFooN?.s)

    assertEquals(null, getNativeFooNWrapperN()?.nativeFooN?.s)
    nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", getNativeFooNWrapperN()?.nativeFooN?.s)
    nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, getNativeFooNWrapperN()?.nativeFooN?.s)

    nullifyTestProperties(null)
}

external class TestClass(
    intWrapper: IntWrapper,
    intWrapperN: IntWrapper?,
    intNWrapper: IntNWrapper,
    intNWrapperN: /*boxed*/ IntNWrapper?,
    fooWrapper: FooWrapper,
    fooWrapperN: FooWrapper?,
    fooNWrapper: FooNWrapper,
    fooNWrapperN: /*boxed*/ FooNWrapper?,
    nativeFooWrapper: NativeFooWrapper,
    nativeFooWrapperN: NativeFooWrapper?,
    nativeFooNWrapper: NativeFooNWrapper,
    nativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?
) {
    fun describeIntWrapper(x: IntWrapper): String
    fun describeIntWrapperN(x: IntWrapper?): String
    fun describeIntNWrapper(x: IntNWrapper): String
    fun describeIntNWrapperN(x: /*boxed*/ IntNWrapper?): String
    fun describeFooWrapper(x: FooWrapper): String
    fun describeFooWrapperN(x: FooWrapper?): String
    fun describeFooNWrapper(x: FooNWrapper): String
    fun describeFooNWrapperN(x: /*boxed*/ FooNWrapper?): String
    fun describeNativeFooWrapper(x: NativeFooWrapper): String
    fun describeNativeFooWrapperN(x: NativeFooWrapper?): String
    fun describeNativeFooNWrapper(x: NativeFooNWrapper): String
    fun describeNativeFooNWrapperN(x: /*boxed*/ NativeFooNWrapper?): String

    var intWrapper: IntWrapper
    var intWrapperN: IntWrapper?
    var intNWrapper: IntNWrapper
    var intNWrapperN: /*boxed*/ IntNWrapper?
    var fooWrapper: FooWrapper
    var fooWrapperN: FooWrapper?
    var fooNWrapper: FooNWrapper
    var fooNWrapperN: /*boxed*/ FooNWrapper?
    var nativeFooWrapper: NativeFooWrapper
    var nativeFooWrapperN: NativeFooWrapper?
    var nativeFooNWrapper: NativeFooNWrapper
    var nativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

    val readOnlyIntWrapper: IntWrapper
    val readOnlyIntWrapperN: IntWrapper?
    val readOnlyIntNWrapper: IntNWrapper
    val readOnlyIntNWrapperN: /*boxed*/ IntNWrapper?
    val readOnlyFooWrapper: FooWrapper
    val readOnlyFooWrapperN: FooWrapper?
    val readOnlyFooNWrapper: FooNWrapper
    val readOnlyFooNWrapperN: /*boxed*/ FooNWrapper?
    val readOnlyNativeFooWrapper: NativeFooWrapper
    val readOnlyNativeFooWrapperN: NativeFooWrapper?
    val readOnlyNativeFooNWrapper: NativeFooNWrapper
    val readOnlyNativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

    fun getIntWrapper(): IntWrapper
    fun getIntWrapperN(): IntWrapper?
    fun getIntNWrapper(): IntNWrapper
    fun getIntNWrapperN(): /*boxed*/ IntNWrapper?
    fun getFooWrapper(): FooWrapper
    fun getFooWrapperN(): FooWrapper?
    fun getFooNWrapper(): FooNWrapper
    fun getFooNWrapperN(): /*boxed*/ FooNWrapper?
    fun getNativeFooWrapper(): NativeFooWrapper
    fun getNativeFooWrapperN(): NativeFooWrapper?
    fun getNativeFooNWrapper(): NativeFooNWrapper
    fun getNativeFooNWrapperN(): /*boxed*/ NativeFooNWrapper?

    companion object {
        fun describeIntWrapper(x: IntWrapper): String
        fun describeIntWrapperN(x: IntWrapper?): String
        fun describeIntNWrapper(x: IntNWrapper): String
        fun describeIntNWrapperN(x: /*boxed*/ IntNWrapper?): String
        fun describeFooWrapper(x: FooWrapper): String
        fun describeFooWrapperN(x: FooWrapper?): String
        fun describeFooNWrapper(x: FooNWrapper): String
        fun describeFooNWrapperN(x: /*boxed*/ FooNWrapper?): String
        fun describeNativeFooWrapper(x: NativeFooWrapper): String
        fun describeNativeFooWrapperN(x: NativeFooWrapper?): String
        fun describeNativeFooNWrapper(x: NativeFooNWrapper): String
        fun describeNativeFooNWrapperN(x: /*boxed*/ NativeFooNWrapper?): String

        var intWrapper: IntWrapper
        var intWrapperN: IntWrapper?
        var intNWrapper: IntNWrapper
        var intNWrapperN: /*boxed*/ IntNWrapper?
        var fooWrapper: FooWrapper
        var fooWrapperN: FooWrapper?
        var fooNWrapper: FooNWrapper
        var fooNWrapperN: /*boxed*/ FooNWrapper?
        var nativeFooWrapper: NativeFooWrapper
        var nativeFooWrapperN: NativeFooWrapper?
        var nativeFooNWrapper: NativeFooNWrapper
        var nativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

        val readOnlyIntWrapper: IntWrapper
        val readOnlyIntWrapperN: IntWrapper?
        val readOnlyIntNWrapper: IntNWrapper
        val readOnlyIntNWrapperN: /*boxed*/ IntNWrapper?
        val readOnlyFooWrapper: FooWrapper
        val readOnlyFooWrapperN: FooWrapper?
        val readOnlyFooNWrapper: FooNWrapper
        val readOnlyFooNWrapperN: /*boxed*/ FooNWrapper?
        val readOnlyNativeFooWrapper: NativeFooWrapper
        val readOnlyNativeFooWrapperN: NativeFooWrapper?
        val readOnlyNativeFooNWrapper: NativeFooNWrapper
        val readOnlyNativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

        fun getIntWrapper(): IntWrapper
        fun getIntWrapperN(): IntWrapper?
        fun getIntNWrapper(): IntNWrapper
        fun getIntNWrapperN(): /*boxed*/ IntNWrapper?
        fun getFooWrapper(): FooWrapper
        fun getFooWrapperN(): FooWrapper?
        fun getFooNWrapper(): FooNWrapper
        fun getFooNWrapperN(): /*boxed*/ FooNWrapper?
        fun getNativeFooWrapper(): NativeFooWrapper
        fun getNativeFooWrapperN(): NativeFooWrapper?
        fun getNativeFooNWrapper(): NativeFooNWrapper
        fun getNativeFooNWrapperN(): /*boxed*/ NativeFooNWrapper?
    }
}

external fun makeEmptyTestClassInstance(): TestClass

external fun makeTestInterfaceInstance(): TestInterface

fun testClassConstructor() {
    val allNonNull = TestClass(
        IntWrapper(42),
        IntWrapper(100),
        IntNWrapper(23),
        IntNWrapper(65),
        FooWrapper(Foo("cat")),
        FooWrapper(Foo("dog")),
        FooNWrapper(Foo("mouse")),
        FooNWrapper(Foo("fox")),
        NativeFooWrapper(NativeFoo("Berlin")),
        NativeFooWrapper(NativeFoo("Amsterdam")),
        NativeFooNWrapper(NativeFoo("Saint-Petersburg")),
        NativeFooNWrapper(NativeFoo("Boston")),
    )

    assertEquals("42 (number)", describeValueOfProperty(allNonNull, "intWrapper"))
    assertEquals("100 (number)", describeValueOfProperty(allNonNull, "intWrapperN"))
    assertEquals("23 (number)", describeValueOfProperty(allNonNull, "intNWrapper"))
    assertEquals("IntNWrapper(value=65) (object)", describeValueOfProperty(allNonNull, "intNWrapperN"))
    assertEquals("Foo(s=cat) (object)", describeValueOfProperty(allNonNull, "fooWrapper"))
    assertEquals("Foo(s=dog) (object)", describeValueOfProperty(allNonNull, "fooWrapperN"))
    assertEquals("Foo(s=mouse) (object)", describeValueOfProperty(allNonNull, "fooNWrapper"))
    assertEquals("FooNWrapper(fooN=Foo(s=fox)) (object)", describeValueOfProperty(allNonNull, "fooNWrapperN"))
    assertEquals("NativeFoo('Berlin') (object)", describeValueOfProperty(allNonNull, "nativeFooWrapper"))
    assertEquals("NativeFoo('Amsterdam') (object)", describeValueOfProperty(allNonNull, "nativeFooWrapperN"))
    assertEquals("NativeFoo('Saint-Petersburg') (object)", describeValueOfProperty(allNonNull, "nativeFooNWrapper"))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('Boston')) (object)", describeValueOfProperty(allNonNull, "nativeFooNWrapperN"))

    val topLevelNull = TestClass(
        IntWrapper(42),
        null,
        IntNWrapper(23),
        null,
        FooWrapper(Foo("cat")),
        null,
        FooNWrapper(Foo("mouse")),
        null,
        NativeFooWrapper(NativeFoo("Berlin")),
        null,
        NativeFooNWrapper(NativeFoo("Saint-Petersburg")),
        null,
    )

    assertEquals("null (object)", describeValueOfProperty(topLevelNull, "intWrapperN"))
    assertEquals("null (object)", describeValueOfProperty(topLevelNull, "intNWrapperN"))
    assertEquals("null (object)", describeValueOfProperty(topLevelNull, "fooWrapperN"))
    assertEquals("null (object)", describeValueOfProperty(topLevelNull, "fooNWrapperN"))
    assertEquals("null (object)", describeValueOfProperty(topLevelNull, "nativeFooWrapperN"))
    assertEquals("null (object)", describeValueOfProperty(topLevelNull, "nativeFooNWrapperN"))

    val wrappingNull = TestClass(
        IntWrapper(42),
        null,
        IntNWrapper(null),
        IntNWrapper(null),
        FooWrapper(Foo("cat")),
        null,
        FooNWrapper(null),
        FooNWrapper(null),
        NativeFooWrapper(NativeFoo("Berlin")),
        null,
        NativeFooNWrapper(null),
        NativeFooNWrapper(null),
    )

    assertEquals("null (object)", describeValueOfProperty(wrappingNull, "intNWrapper"))
    assertEquals("IntNWrapper(value=null) (object)", describeValueOfProperty(wrappingNull, "intNWrapperN"))
    assertEquals("null (object)", describeValueOfProperty(wrappingNull, "fooNWrapper"))
    assertEquals("FooNWrapper(fooN=null) (object)", describeValueOfProperty(wrappingNull, "fooNWrapperN"))
    assertEquals("null (object)", describeValueOfProperty(wrappingNull, "nativeFooNWrapper"))
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", describeValueOfProperty(wrappingNull, "nativeFooNWrapperN"))
}

fun testClassMethodsWithValueClassesInArgs() {
    val o = makeEmptyTestClassInstance()

    assertEquals("42 (number)", o.describeIntWrapper(IntWrapper(42)))
    assertEquals("100 (number)", o.describeIntWrapperN(IntWrapper(100)))
    assertEquals("null (object)", o.describeIntWrapperN(null))

    assertEquals("42 (number)", o.describeIntNWrapper(IntNWrapper(42)))
    assertEquals("null (object)", o.describeIntNWrapper(IntNWrapper(null)))
    assertEquals("IntNWrapper(value=100) (object)", o.describeIntNWrapperN(IntNWrapper(100)))
    assertEquals("IntNWrapper(value=null) (object)", o.describeIntNWrapperN(IntNWrapper(null)))
    assertEquals("null (object)", o.describeIntNWrapperN(null))

    assertEquals("Foo(s=hello) (object)", o.describeFooWrapper(FooWrapper(Foo("hello"))))
    assertEquals("Foo(s=goodbye) (object)", o.describeFooWrapperN(FooWrapper(Foo("goodbye"))))
    assertEquals("null (object)", o.describeFooWrapperN(null))

    assertEquals("Foo(s=hello) (object)", o.describeFooNWrapper(FooNWrapper(Foo("hello"))))
    assertEquals("null (object)", o.describeFooNWrapper(FooNWrapper(null)))
    assertEquals("FooNWrapper(fooN=Foo(s=goodbye)) (object)", o.describeFooNWrapperN(FooNWrapper(Foo("goodbye"))))
    assertEquals("FooNWrapper(fooN=null) (object)", o.describeFooNWrapperN(FooNWrapper(null)))
    assertEquals("null (object)", o.describeFooNWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", o.describeNativeFooWrapper(NativeFooWrapper(NativeFoo("hello"))))
    assertEquals("NativeFoo('goodbye') (object)", o.describeNativeFooWrapperN(NativeFooWrapper(NativeFoo("goodbye"))))
    assertEquals("null (object)", o.describeNativeFooWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", o.describeNativeFooNWrapper(NativeFooNWrapper(NativeFoo("hello"))))
    assertEquals("null (object)", o.describeNativeFooNWrapper(NativeFooNWrapper(null)))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('goodbye')) (object)", o.describeNativeFooNWrapperN(NativeFooNWrapper(NativeFoo("goodbye"))))
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", o.describeNativeFooNWrapperN(NativeFooNWrapper(null)))
    assertEquals("null (object)", o.describeNativeFooNWrapperN(null))
}

fun testWritableClassProperties() {
    val o = makeEmptyTestClassInstance()

    assertEquals("null (object)", describeValueOfProperty(o, "intWrapper"))
    o.intWrapper = IntWrapper(42)
    assertEquals("42 (number)", describeValueOfProperty(o, "intWrapper"))
    assertEquals(42, o.intWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(o, "intWrapperN"))
    o.intWrapperN = IntWrapper(100)
    assertEquals("100 (number)", describeValueOfProperty(o, "intWrapperN"))
    assertEquals(100, o.intWrapperN?.value)
    o.intWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "intWrapperN"))
    assertEquals(null, o.intWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(o, "intNWrapper"))
    o.intNWrapper = IntNWrapper(23)
    assertEquals("23 (number)", describeValueOfProperty(o, "intNWrapper"))
    assertEquals(23, o.intNWrapper.value)
    o.intNWrapper = IntNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(o, "intNWrapper"))
    assertEquals(null, o.intNWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(o, "intNWrapperN"))
    o.intNWrapperN = IntNWrapper(65)
    assertEquals("IntNWrapper(value=65) (object)", describeValueOfProperty(o, "intNWrapperN"))
    assertEquals(65, o.intNWrapperN?.value)
    o.intNWrapperN = IntNWrapper(null)
    assertEquals("IntNWrapper(value=null) (object)", describeValueOfProperty(o, "intNWrapperN"))
    assertEquals(null, o.intNWrapperN?.value)
    o.intNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "intNWrapperN"))
    assertEquals(null, o.intNWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(o, "fooWrapper"))
    o.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("Foo(s=cat) (object)", describeValueOfProperty(o, "fooWrapper"))
    assertEquals("cat", o.fooWrapper.foo.s)

    assertEquals("null (object)", describeValueOfProperty(o, "fooWrapperN"))
    o.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("Foo(s=dog) (object)", describeValueOfProperty(o, "fooWrapperN"))
    assertEquals("dog", o.fooWrapperN?.foo?.s)
    o.fooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "fooWrapperN"))
    assertEquals(null, o.fooWrapperN?.foo?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "fooNWrapper"))
    o.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("Foo(s=mouse) (object)", describeValueOfProperty(o, "fooNWrapper"))
    assertEquals("mouse", o.fooNWrapper.fooN?.s)
    o.fooNWrapper = FooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(o, "fooNWrapper"))
    assertEquals(null, o.fooNWrapper.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "fooNWrapperN"))
    o.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("FooNWrapper(fooN=Foo(s=fox)) (object)", describeValueOfProperty(o, "fooNWrapperN"))
    assertEquals("fox", o.fooNWrapperN?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(null)
    assertEquals("FooNWrapper(fooN=null) (object)", describeValueOfProperty(o, "fooNWrapperN"))
    assertEquals(null, o.fooNWrapperN?.fooN?.s)
    o.fooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "fooNWrapperN"))
    assertEquals(null, o.fooNWrapperN?.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooWrapper"))
    o.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("NativeFoo('Berlin') (object)", describeValueOfProperty(o, "nativeFooWrapper"))
    assertEquals("Berlin", o.nativeFooWrapper.nativeFoo.s)

    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooWrapperN"))
    o.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("NativeFoo('Amsterdam') (object)", describeValueOfProperty(o, "nativeFooWrapperN"))
    assertEquals("Amsterdam", o.nativeFooWrapperN?.nativeFoo?.s)
    o.nativeFooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooWrapperN"))
    assertEquals(null, o.nativeFooWrapperN?.nativeFoo?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooNWrapper"))
    o.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("NativeFoo('Saint-Petersburg') (object)", describeValueOfProperty(o, "nativeFooNWrapper"))
    assertEquals("Saint-Petersburg", o.nativeFooNWrapper.nativeFooN?.s)
    o.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooNWrapper"))
    assertEquals(null, o.nativeFooNWrapper.nativeFooN?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooNWrapperN"))
    o.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('Boston')) (object)", describeValueOfProperty(o, "nativeFooNWrapperN"))
    assertEquals("Boston", o.nativeFooNWrapperN?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", describeValueOfProperty(o, "nativeFooNWrapperN"))
    assertEquals(null, o.nativeFooNWrapperN?.nativeFooN?.s)
    o.nativeFooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooNWrapperN"))
    assertEquals(null, o.nativeFooNWrapperN?.nativeFooN?.s)
}

fun testReadOnlyClassProperties() {
    val o = makeEmptyTestClassInstance()

    o.intWrapper = IntWrapper(42)
    assertEquals(42, o.readOnlyIntWrapper.value)

    assertEquals(null, o.readOnlyIntWrapperN?.value)
    o.intWrapperN = IntWrapper(100)
    assertEquals(100, o.readOnlyIntWrapperN?.value)

    o.intNWrapper = IntNWrapper(23)
    assertEquals(23, o.readOnlyIntNWrapper.value)
    o.intNWrapper = IntNWrapper(null)
    assertEquals(null, o.readOnlyIntNWrapper.value)

    assertEquals(null, o.readOnlyIntNWrapperN?.value)
    o.intNWrapperN = IntNWrapper(65)
    assertEquals(65, o.readOnlyIntNWrapperN?.value)
    o.intNWrapperN = IntNWrapper(null)
    assertEquals(null, o.readOnlyIntNWrapperN?.value)

    o.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", o.readOnlyFooWrapper.foo.s)

    assertEquals(null, o.readOnlyFooWrapperN?.foo?.s)
    o.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", o.readOnlyFooWrapperN?.foo?.s)

    o.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", o.readOnlyFooNWrapper.fooN?.s)
    o.fooNWrapper = FooNWrapper(null)
    assertEquals(null, o.readOnlyFooNWrapper.fooN?.s)

    assertEquals(null, o.readOnlyFooNWrapperN?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", o.readOnlyFooNWrapperN?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(null)
    assertEquals(null, o.readOnlyFooNWrapperN?.fooN?.s)

    o.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", o.readOnlyNativeFooWrapper.nativeFoo.s)

    assertEquals(null, o.readOnlyNativeFooWrapperN?.nativeFoo?.s)
    o.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", o.readOnlyNativeFooWrapperN?.nativeFoo?.s)

    o.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", o.readOnlyNativeFooNWrapper.nativeFooN?.s)
    o.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, o.readOnlyNativeFooNWrapper.nativeFooN?.s)

    assertEquals(null, o.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", o.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, o.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
}

fun testClassMethodsWithValueClassInReturnType() {
    val o = makeEmptyTestClassInstance()

    o.intWrapper = IntWrapper(42)
    assertEquals(42, o.getIntWrapper().value)

    assertEquals(null, o.getIntWrapperN()?.value)
    o.intWrapperN = IntWrapper(100)
    assertEquals(100, o.getIntWrapperN()?.value)

    o.intNWrapper = IntNWrapper(23)
    assertEquals(23, o.getIntNWrapper().value)
    o.intNWrapper = IntNWrapper(null)
    assertEquals(null, o.getIntNWrapper().value)

    assertEquals(null, o.getIntNWrapperN()?.value)
    o.intNWrapperN = IntNWrapper(65)
    assertEquals(65, o.getIntNWrapperN()?.value)
    o.intNWrapperN = IntNWrapper(null)
    assertEquals(null, o.getIntNWrapperN()?.value)

    o.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", o.getFooWrapper().foo.s)

    assertEquals(null, o.getFooWrapperN()?.foo?.s)
    o.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", o.getFooWrapperN()?.foo?.s)

    o.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", o.getFooNWrapper().fooN?.s)
    o.fooNWrapper = FooNWrapper(null)
    assertEquals(null, o.getFooNWrapper().fooN?.s)

    assertEquals(null, o.getFooNWrapperN()?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", o.getFooNWrapperN()?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(null)
    assertEquals(null, o.getFooNWrapperN()?.fooN?.s)

    o.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", o.getNativeFooWrapper().nativeFoo.s)

    assertEquals(null, o.getNativeFooWrapperN()?.nativeFoo?.s)
    o.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", o.getNativeFooWrapperN()?.nativeFoo?.s)

    o.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", o.getNativeFooNWrapper().nativeFooN?.s)
    o.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, o.getNativeFooNWrapper().nativeFooN?.s)

    assertEquals(null, o.getNativeFooNWrapperN()?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", o.getNativeFooNWrapperN()?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, o.getNativeFooNWrapperN()?.nativeFooN?.s)
}

fun testCompanionObjectMethodsWithValueClassesInArgs() {
    assertEquals("42 (number)", TestClass.describeIntWrapper(IntWrapper(42)))
    assertEquals("100 (number)", TestClass.describeIntWrapperN(IntWrapper(100)))
    assertEquals("null (object)", TestClass.describeIntWrapperN(null))

    assertEquals("42 (number)", TestClass.describeIntNWrapper(IntNWrapper(42)))
    assertEquals("null (object)", TestClass.describeIntNWrapper(IntNWrapper(null)))
    assertEquals("IntNWrapper(value=100) (object)", TestClass.describeIntNWrapperN(IntNWrapper(100)))
    assertEquals("IntNWrapper(value=null) (object)", TestClass.describeIntNWrapperN(IntNWrapper(null)))
    assertEquals("null (object)", TestClass.describeIntNWrapperN(null))

    assertEquals("Foo(s=hello) (object)", TestClass.describeFooWrapper(FooWrapper(Foo("hello"))))
    assertEquals("Foo(s=goodbye) (object)", TestClass.describeFooWrapperN(FooWrapper(Foo("goodbye"))))
    assertEquals("null (object)", TestClass.describeFooWrapperN(null))

    assertEquals("Foo(s=hello) (object)", TestClass.describeFooNWrapper(FooNWrapper(Foo("hello"))))
    assertEquals("null (object)", TestClass.describeFooNWrapper(FooNWrapper(null)))
    assertEquals("FooNWrapper(fooN=Foo(s=goodbye)) (object)", TestClass.describeFooNWrapperN(FooNWrapper(Foo("goodbye"))))
    assertEquals("FooNWrapper(fooN=null) (object)", TestClass.describeFooNWrapperN(FooNWrapper(null)))
    assertEquals("null (object)", TestClass.describeFooNWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", TestClass.describeNativeFooWrapper(NativeFooWrapper(NativeFoo("hello"))))
    assertEquals("NativeFoo('goodbye') (object)", TestClass.describeNativeFooWrapperN(NativeFooWrapper(NativeFoo("goodbye"))))
    assertEquals("null (object)", TestClass.describeNativeFooWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", TestClass.describeNativeFooNWrapper(NativeFooNWrapper(NativeFoo("hello"))))
    assertEquals("null (object)", TestClass.describeNativeFooNWrapper(NativeFooNWrapper(null)))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('goodbye')) (object)", TestClass.describeNativeFooNWrapperN(NativeFooNWrapper(NativeFoo("goodbye"))))
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", TestClass.describeNativeFooNWrapperN(NativeFooNWrapper(null)))
    assertEquals("null (object)", TestClass.describeNativeFooNWrapperN(null))
}

fun testWritableCompanionObjectProperties() {
    assertEquals("null (object)", describeValueOfProperty(TestClass, "intWrapper"))
    TestClass.intWrapper = IntWrapper(42)
    assertEquals("42 (number)", describeValueOfProperty(TestClass, "intWrapper"))
    assertEquals(42, TestClass.intWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "intWrapperN"))
    TestClass.intWrapperN = IntWrapper(100)
    assertEquals("100 (number)", describeValueOfProperty(TestClass, "intWrapperN"))
    assertEquals(100, TestClass.intWrapperN?.value)
    TestClass.intWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestClass, "intWrapperN"))
    assertEquals(null, TestClass.intWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "intNWrapper"))
    TestClass.intNWrapper = IntNWrapper(23)
    assertEquals("23 (number)", describeValueOfProperty(TestClass, "intNWrapper"))
    assertEquals(23, TestClass.intNWrapper.value)
    TestClass.intNWrapper = IntNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(TestClass, "intNWrapper"))
    assertEquals(null, TestClass.intNWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "intNWrapperN"))
    TestClass.intNWrapperN = IntNWrapper(65)
    assertEquals("IntNWrapper(value=65) (object)", describeValueOfProperty(TestClass, "intNWrapperN"))
    assertEquals(65, TestClass.intNWrapperN?.value)
    TestClass.intNWrapperN = IntNWrapper(null)
    assertEquals("IntNWrapper(value=null) (object)", describeValueOfProperty(TestClass, "intNWrapperN"))
    assertEquals(null, TestClass.intNWrapperN?.value)
    TestClass.intNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestClass, "intNWrapperN"))
    assertEquals(null, TestClass.intNWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "fooWrapper"))
    TestClass.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("Foo(s=cat) (object)", describeValueOfProperty(TestClass, "fooWrapper"))
    assertEquals("cat", TestClass.fooWrapper.foo.s)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "fooWrapperN"))
    TestClass.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("Foo(s=dog) (object)", describeValueOfProperty(TestClass, "fooWrapperN"))
    assertEquals("dog", TestClass.fooWrapperN?.foo?.s)
    TestClass.fooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestClass, "fooWrapperN"))
    assertEquals(null, TestClass.fooWrapperN?.foo?.s)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "fooNWrapper"))
    TestClass.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("Foo(s=mouse) (object)", describeValueOfProperty(TestClass, "fooNWrapper"))
    assertEquals("mouse", TestClass.fooNWrapper.fooN?.s)
    TestClass.fooNWrapper = FooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(TestClass, "fooNWrapper"))
    assertEquals(null, TestClass.fooNWrapper.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "fooNWrapperN"))
    TestClass.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("FooNWrapper(fooN=Foo(s=fox)) (object)", describeValueOfProperty(TestClass, "fooNWrapperN"))
    assertEquals("fox", TestClass.fooNWrapperN?.fooN?.s)
    TestClass.fooNWrapperN = FooNWrapper(null)
    assertEquals("FooNWrapper(fooN=null) (object)", describeValueOfProperty(TestClass, "fooNWrapperN"))
    assertEquals(null, TestClass.fooNWrapperN?.fooN?.s)
    TestClass.fooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestClass, "fooNWrapperN"))
    assertEquals(null, TestClass.fooNWrapperN?.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "nativeFooWrapper"))
    TestClass.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("NativeFoo('Berlin') (object)", describeValueOfProperty(TestClass, "nativeFooWrapper"))
    assertEquals("Berlin", TestClass.nativeFooWrapper.nativeFoo.s)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "nativeFooWrapperN"))
    TestClass.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("NativeFoo('Amsterdam') (object)", describeValueOfProperty(TestClass, "nativeFooWrapperN"))
    assertEquals("Amsterdam", TestClass.nativeFooWrapperN?.nativeFoo?.s)
    TestClass.nativeFooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestClass, "nativeFooWrapperN"))
    assertEquals(null, TestClass.nativeFooWrapperN?.nativeFoo?.s)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "nativeFooNWrapper"))
    TestClass.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("NativeFoo('Saint-Petersburg') (object)", describeValueOfProperty(TestClass, "nativeFooNWrapper"))
    assertEquals("Saint-Petersburg", TestClass.nativeFooNWrapper.nativeFooN?.s)
    TestClass.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(TestClass, "nativeFooNWrapper"))
    assertEquals(null, TestClass.nativeFooNWrapper.nativeFooN?.s)

    assertEquals("null (object)", describeValueOfProperty(TestClass, "nativeFooNWrapperN"))
    TestClass.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('Boston')) (object)", describeValueOfProperty(TestClass, "nativeFooNWrapperN"))
    assertEquals("Boston", TestClass.nativeFooNWrapperN?.nativeFooN?.s)
    TestClass.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", describeValueOfProperty(TestClass, "nativeFooNWrapperN"))
    assertEquals(null, TestClass.nativeFooNWrapperN?.nativeFooN?.s)
    TestClass.nativeFooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestClass, "nativeFooNWrapperN"))
    assertEquals(null, TestClass.nativeFooNWrapperN?.nativeFooN?.s)

    nullifyTestProperties(TestClass)
}

fun testReadOnlyCompanionObjectProperties() {
    TestClass.intWrapper = IntWrapper(42)
    assertEquals(42, TestClass.readOnlyIntWrapper.value)

    assertEquals(null, TestClass.readOnlyIntWrapperN?.value)
    TestClass.intWrapperN = IntWrapper(100)
    assertEquals(100, TestClass.readOnlyIntWrapperN?.value)

    TestClass.intNWrapper = IntNWrapper(23)
    assertEquals(23, TestClass.readOnlyIntNWrapper.value)
    TestClass.intNWrapper = IntNWrapper(null)
    assertEquals(null, TestClass.readOnlyIntNWrapper.value)

    assertEquals(null, TestClass.readOnlyIntNWrapperN?.value)
    TestClass.intNWrapperN = IntNWrapper(65)
    assertEquals(65, TestClass.readOnlyIntNWrapperN?.value)
    TestClass.intNWrapperN = IntNWrapper(null)
    assertEquals(null, TestClass.readOnlyIntNWrapperN?.value)

    TestClass.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", TestClass.readOnlyFooWrapper.foo.s)

    assertEquals(null, TestClass.readOnlyFooWrapperN?.foo?.s)
    TestClass.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", TestClass.readOnlyFooWrapperN?.foo?.s)

    TestClass.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", TestClass.readOnlyFooNWrapper.fooN?.s)
    TestClass.fooNWrapper = FooNWrapper(null)
    assertEquals(null, TestClass.readOnlyFooNWrapper.fooN?.s)

    assertEquals(null, TestClass.readOnlyFooNWrapperN?.fooN?.s)
    TestClass.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", TestClass.readOnlyFooNWrapperN?.fooN?.s)
    TestClass.fooNWrapperN = FooNWrapper(null)
    assertEquals(null, TestClass.readOnlyFooNWrapperN?.fooN?.s)

    TestClass.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", TestClass.readOnlyNativeFooWrapper.nativeFoo.s)

    assertEquals(null, TestClass.readOnlyNativeFooWrapperN?.nativeFoo?.s)
    TestClass.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", TestClass.readOnlyNativeFooWrapperN?.nativeFoo?.s)

    TestClass.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", TestClass.readOnlyNativeFooNWrapper.nativeFooN?.s)
    TestClass.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, TestClass.readOnlyNativeFooNWrapper.nativeFooN?.s)

    assertEquals(null, TestClass.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    TestClass.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", TestClass.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    TestClass.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, TestClass.readOnlyNativeFooNWrapperN?.nativeFooN?.s)

    nullifyTestProperties(TestClass)
}

fun testCompanionObjectMethodsWithValueClassinReturnType() {
    TestClass.intWrapper = IntWrapper(42)
    assertEquals(42, TestClass.getIntWrapper().value)

    assertEquals(null, TestClass.getIntWrapperN()?.value)
    TestClass.intWrapperN = IntWrapper(100)
    assertEquals(100, TestClass.getIntWrapperN()?.value)

    TestClass.intNWrapper = IntNWrapper(23)
    assertEquals(23, TestClass.getIntNWrapper().value)
    TestClass.intNWrapper = IntNWrapper(null)
    assertEquals(null, TestClass.getIntNWrapper().value)

    assertEquals(null, TestClass.getIntNWrapperN()?.value)
    TestClass.intNWrapperN = IntNWrapper(65)
    assertEquals(65, TestClass.getIntNWrapperN()?.value)
    TestClass.intNWrapperN = IntNWrapper(null)
    assertEquals(null, TestClass.getIntNWrapperN()?.value)

    TestClass.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", TestClass.getFooWrapper().foo.s)

    assertEquals(null, TestClass.getFooWrapperN()?.foo?.s)
    TestClass.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", TestClass.getFooWrapperN()?.foo?.s)

    TestClass.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", TestClass.getFooNWrapper().fooN?.s)
    TestClass.fooNWrapper = FooNWrapper(null)
    assertEquals(null, TestClass.getFooNWrapper().fooN?.s)

    assertEquals(null, TestClass.getFooNWrapperN()?.fooN?.s)
    TestClass.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", TestClass.getFooNWrapperN()?.fooN?.s)
    TestClass.fooNWrapperN = FooNWrapper(null)
    assertEquals(null, TestClass.getFooNWrapperN()?.fooN?.s)

    TestClass.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", TestClass.getNativeFooWrapper().nativeFoo.s)

    assertEquals(null, TestClass.getNativeFooWrapperN()?.nativeFoo?.s)
    TestClass.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", TestClass.getNativeFooWrapperN()?.nativeFoo?.s)

    TestClass.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", TestClass.getNativeFooNWrapper().nativeFooN?.s)
    TestClass.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, TestClass.getNativeFooNWrapper().nativeFooN?.s)

    assertEquals(null, TestClass.getNativeFooNWrapperN()?.nativeFooN?.s)
    TestClass.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", TestClass.getNativeFooNWrapperN()?.nativeFooN?.s)
    TestClass.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, TestClass.getNativeFooNWrapperN()?.nativeFooN?.s)

    nullifyTestProperties(TestClass)
}

external interface TestInterface {
    fun describeIntWrapper(x: IntWrapper): String
    fun describeIntWrapperN(x: IntWrapper?): String
    fun describeIntNWrapper(x: IntNWrapper): String
    fun describeIntNWrapperN(x: /*boxed*/ IntNWrapper?): String
    fun describeFooWrapper(x: FooWrapper): String
    fun describeFooWrapperN(x: FooWrapper?): String
    fun describeFooNWrapper(x: FooNWrapper): String
    fun describeFooNWrapperN(x: /*boxed*/ FooNWrapper?): String
    fun describeNativeFooWrapper(x: NativeFooWrapper): String
    fun describeNativeFooWrapperN(x: NativeFooWrapper?): String
    fun describeNativeFooNWrapper(x: NativeFooNWrapper): String
    fun describeNativeFooNWrapperN(x: /*boxed*/ NativeFooNWrapper?): String

    var intWrapper: IntWrapper
    var intWrapperN: IntWrapper?
    var intNWrapper: IntNWrapper
    var intNWrapperN: /*boxed*/ IntNWrapper?
    var fooWrapper: FooWrapper
    var fooWrapperN: FooWrapper?
    var fooNWrapper: FooNWrapper
    var fooNWrapperN: /*boxed*/ FooNWrapper?
    var nativeFooWrapper: NativeFooWrapper
    var nativeFooWrapperN: NativeFooWrapper?
    var nativeFooNWrapper: NativeFooNWrapper
    var nativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

    val readOnlyIntWrapper: IntWrapper
    val readOnlyIntWrapperN: IntWrapper?
    val readOnlyIntNWrapper: IntNWrapper
    val readOnlyIntNWrapperN: /*boxed*/ IntNWrapper?
    val readOnlyFooWrapper: FooWrapper
    val readOnlyFooWrapperN: FooWrapper?
    val readOnlyFooNWrapper: FooNWrapper
    val readOnlyFooNWrapperN: /*boxed*/ FooNWrapper?
    val readOnlyNativeFooWrapper: NativeFooWrapper
    val readOnlyNativeFooWrapperN: NativeFooWrapper?
    val readOnlyNativeFooNWrapper: NativeFooNWrapper
    val readOnlyNativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

    fun getIntWrapper(): IntWrapper
    fun getIntWrapperN(): IntWrapper?
    fun getIntNWrapper(): IntNWrapper
    fun getIntNWrapperN(): /*boxed*/ IntNWrapper?
    fun getFooWrapper(): FooWrapper
    fun getFooWrapperN(): FooWrapper?
    fun getFooNWrapper(): FooNWrapper
    fun getFooNWrapperN(): /*boxed*/ FooNWrapper?
    fun getNativeFooWrapper(): NativeFooWrapper
    fun getNativeFooWrapperN(): NativeFooWrapper?
    fun getNativeFooNWrapper(): NativeFooNWrapper
    fun getNativeFooNWrapperN(): /*boxed*/ NativeFooNWrapper?
}

fun testInterfaceMethodsWithValueClassesInArgs() {
    val o = makeTestInterfaceInstance()

    assertEquals("42 (number)", o.describeIntWrapper(IntWrapper(42)))
    assertEquals("100 (number)", o.describeIntWrapperN(IntWrapper(100)))
    assertEquals("null (object)", o.describeIntWrapperN(null))

    assertEquals("42 (number)", o.describeIntNWrapper(IntNWrapper(42)))
    assertEquals("null (object)", o.describeIntNWrapper(IntNWrapper(null)))
    assertEquals("IntNWrapper(value=100) (object)", o.describeIntNWrapperN(IntNWrapper(100)))
    assertEquals("IntNWrapper(value=null) (object)", o.describeIntNWrapperN(IntNWrapper(null)))
    assertEquals("null (object)", o.describeIntNWrapperN(null))

    assertEquals("Foo(s=hello) (object)", o.describeFooWrapper(FooWrapper(Foo("hello"))))
    assertEquals("Foo(s=goodbye) (object)", o.describeFooWrapperN(FooWrapper(Foo("goodbye"))))
    assertEquals("null (object)", o.describeFooWrapperN(null))

    assertEquals("Foo(s=hello) (object)", o.describeFooNWrapper(FooNWrapper(Foo("hello"))))
    assertEquals("null (object)", o.describeFooNWrapper(FooNWrapper(null)))
    assertEquals("FooNWrapper(fooN=Foo(s=goodbye)) (object)", o.describeFooNWrapperN(FooNWrapper(Foo("goodbye"))))
    assertEquals("FooNWrapper(fooN=null) (object)", o.describeFooNWrapperN(FooNWrapper(null)))
    assertEquals("null (object)", o.describeFooNWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", o.describeNativeFooWrapper(NativeFooWrapper(NativeFoo("hello"))))
    assertEquals("NativeFoo('goodbye') (object)", o.describeNativeFooWrapperN(NativeFooWrapper(NativeFoo("goodbye"))))
    assertEquals("null (object)", o.describeNativeFooWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", o.describeNativeFooNWrapper(NativeFooNWrapper(NativeFoo("hello"))))
    assertEquals("null (object)", o.describeNativeFooNWrapper(NativeFooNWrapper(null)))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('goodbye')) (object)", o.describeNativeFooNWrapperN(NativeFooNWrapper(NativeFoo("goodbye"))))
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", o.describeNativeFooNWrapperN(NativeFooNWrapper(null)))
    assertEquals("null (object)", o.describeNativeFooNWrapperN(null))
}

fun testWritableInterfaceProperties() {
    val o = makeTestInterfaceInstance()

    assertEquals("null (object)", describeValueOfProperty(o, "intWrapper"))
    o.intWrapper = IntWrapper(42)
    assertEquals("42 (number)", describeValueOfProperty(o, "intWrapper"))
    assertEquals(42, o.intWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(o, "intWrapperN"))
    o.intWrapperN = IntWrapper(100)
    assertEquals("100 (number)", describeValueOfProperty(o, "intWrapperN"))
    assertEquals(100, o.intWrapperN?.value)
    o.intWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "intWrapperN"))
    assertEquals(null, o.intWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(o, "intNWrapper"))
    o.intNWrapper = IntNWrapper(23)
    assertEquals("23 (number)", describeValueOfProperty(o, "intNWrapper"))
    assertEquals(23, o.intNWrapper.value)
    o.intNWrapper = IntNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(o, "intNWrapper"))
    assertEquals(null, o.intNWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(o, "intNWrapperN"))
    o.intNWrapperN = IntNWrapper(65)
    assertEquals("IntNWrapper(value=65) (object)", describeValueOfProperty(o, "intNWrapperN"))
    assertEquals(65, o.intNWrapperN?.value)
    o.intNWrapperN = IntNWrapper(null)
    assertEquals("IntNWrapper(value=null) (object)", describeValueOfProperty(o, "intNWrapperN"))
    assertEquals(null, o.intNWrapperN?.value)
    o.intNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "intNWrapperN"))
    assertEquals(null, o.intNWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(o, "fooWrapper"))
    o.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("Foo(s=cat) (object)", describeValueOfProperty(o, "fooWrapper"))
    assertEquals("cat", o.fooWrapper.foo.s)

    assertEquals("null (object)", describeValueOfProperty(o, "fooWrapperN"))
    o.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("Foo(s=dog) (object)", describeValueOfProperty(o, "fooWrapperN"))
    assertEquals("dog", o.fooWrapperN?.foo?.s)
    o.fooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "fooWrapperN"))
    assertEquals(null, o.fooWrapperN?.foo?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "fooNWrapper"))
    o.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("Foo(s=mouse) (object)", describeValueOfProperty(o, "fooNWrapper"))
    assertEquals("mouse", o.fooNWrapper.fooN?.s)
    o.fooNWrapper = FooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(o, "fooNWrapper"))
    assertEquals(null, o.fooNWrapper.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "fooNWrapperN"))
    o.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("FooNWrapper(fooN=Foo(s=fox)) (object)", describeValueOfProperty(o, "fooNWrapperN"))
    assertEquals("fox", o.fooNWrapperN?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(null)
    assertEquals("FooNWrapper(fooN=null) (object)", describeValueOfProperty(o, "fooNWrapperN"))
    assertEquals(null, o.fooNWrapperN?.fooN?.s)
    o.fooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "fooNWrapperN"))
    assertEquals(null, o.fooNWrapperN?.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooWrapper"))
    o.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("NativeFoo('Berlin') (object)", describeValueOfProperty(o, "nativeFooWrapper"))
    assertEquals("Berlin", o.nativeFooWrapper.nativeFoo.s)

    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooWrapperN"))
    o.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("NativeFoo('Amsterdam') (object)", describeValueOfProperty(o, "nativeFooWrapperN"))
    assertEquals("Amsterdam", o.nativeFooWrapperN?.nativeFoo?.s)
    o.nativeFooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooWrapperN"))
    assertEquals(null, o.nativeFooWrapperN?.nativeFoo?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooNWrapper"))
    o.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("NativeFoo('Saint-Petersburg') (object)", describeValueOfProperty(o, "nativeFooNWrapper"))
    assertEquals("Saint-Petersburg", o.nativeFooNWrapper.nativeFooN?.s)
    o.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooNWrapper"))
    assertEquals(null, o.nativeFooNWrapper.nativeFooN?.s)

    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooNWrapperN"))
    o.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('Boston')) (object)", describeValueOfProperty(o, "nativeFooNWrapperN"))
    assertEquals("Boston", o.nativeFooNWrapperN?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", describeValueOfProperty(o, "nativeFooNWrapperN"))
    assertEquals(null, o.nativeFooNWrapperN?.nativeFooN?.s)
    o.nativeFooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(o, "nativeFooNWrapperN"))
    assertEquals(null, o.nativeFooNWrapperN?.nativeFooN?.s)
}

fun testReadOnlyInterfaceProperties() {
    val o = makeTestInterfaceInstance()

    o.intWrapper = IntWrapper(42)
    assertEquals(42, o.readOnlyIntWrapper.value)

    assertEquals(null, o.readOnlyIntWrapperN?.value)
    o.intWrapperN = IntWrapper(100)
    assertEquals(100, o.readOnlyIntWrapperN?.value)

    o.intNWrapper = IntNWrapper(23)
    assertEquals(23, o.readOnlyIntNWrapper.value)
    o.intNWrapper = IntNWrapper(null)
    assertEquals(null, o.readOnlyIntNWrapper.value)

    assertEquals(null, o.readOnlyIntNWrapperN?.value)
    o.intNWrapperN = IntNWrapper(65)
    assertEquals(65, o.readOnlyIntNWrapperN?.value)
    o.intNWrapperN = IntNWrapper(null)
    assertEquals(null, o.readOnlyIntNWrapperN?.value)

    o.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", o.readOnlyFooWrapper.foo.s)

    assertEquals(null, o.readOnlyFooWrapperN?.foo?.s)
    o.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", o.readOnlyFooWrapperN?.foo?.s)

    o.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", o.readOnlyFooNWrapper.fooN?.s)
    o.fooNWrapper = FooNWrapper(null)
    assertEquals(null, o.readOnlyFooNWrapper.fooN?.s)

    assertEquals(null, o.readOnlyFooNWrapperN?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", o.readOnlyFooNWrapperN?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(null)
    assertEquals(null, o.readOnlyFooNWrapperN?.fooN?.s)

    o.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", o.readOnlyNativeFooWrapper.nativeFoo.s)

    assertEquals(null, o.readOnlyNativeFooWrapperN?.nativeFoo?.s)
    o.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", o.readOnlyNativeFooWrapperN?.nativeFoo?.s)

    o.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", o.readOnlyNativeFooNWrapper.nativeFooN?.s)
    o.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, o.readOnlyNativeFooNWrapper.nativeFooN?.s)

    assertEquals(null, o.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", o.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, o.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
}

fun testInterfaceMethodsWithValueClassInReturnType() {
    val o = makeTestInterfaceInstance()

    o.intWrapper = IntWrapper(42)
    assertEquals(42, o.getIntWrapper().value)

    assertEquals(null, o.getIntWrapperN()?.value)
    o.intWrapperN = IntWrapper(100)
    assertEquals(100, o.getIntWrapperN()?.value)

    o.intNWrapper = IntNWrapper(23)
    assertEquals(23, o.getIntNWrapper().value)
    o.intNWrapper = IntNWrapper(null)
    assertEquals(null, o.getIntNWrapper().value)

    assertEquals(null, o.getIntNWrapperN()?.value)
    o.intNWrapperN = IntNWrapper(65)
    assertEquals(65, o.getIntNWrapperN()?.value)
    o.intNWrapperN = IntNWrapper(null)
    assertEquals(null, o.getIntNWrapperN()?.value)

    o.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", o.getFooWrapper().foo.s)

    assertEquals(null, o.getFooWrapperN()?.foo?.s)
    o.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", o.getFooWrapperN()?.foo?.s)

    o.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", o.getFooNWrapper().fooN?.s)
    o.fooNWrapper = FooNWrapper(null)
    assertEquals(null, o.getFooNWrapper().fooN?.s)

    assertEquals(null, o.getFooNWrapperN()?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", o.getFooNWrapperN()?.fooN?.s)
    o.fooNWrapperN = FooNWrapper(null)
    assertEquals(null, o.getFooNWrapperN()?.fooN?.s)

    o.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", o.getNativeFooWrapper().nativeFoo.s)

    assertEquals(null, o.getNativeFooWrapperN()?.nativeFoo?.s)
    o.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", o.getNativeFooWrapperN()?.nativeFoo?.s)

    o.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", o.getNativeFooNWrapper().nativeFooN?.s)
    o.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, o.getNativeFooNWrapper().nativeFooN?.s)

    assertEquals(null, o.getNativeFooNWrapperN()?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", o.getNativeFooNWrapperN()?.nativeFooN?.s)
    o.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, o.getNativeFooNWrapperN()?.nativeFooN?.s)
}

external object TestObject {
    fun describeIntWrapper(x: IntWrapper): String
    fun describeIntWrapperN(x: IntWrapper?): String
    fun describeIntNWrapper(x: IntNWrapper): String
    fun describeIntNWrapperN(x: /*boxed*/ IntNWrapper?): String
    fun describeFooWrapper(x: FooWrapper): String
    fun describeFooWrapperN(x: FooWrapper?): String
    fun describeFooNWrapper(x: FooNWrapper): String
    fun describeFooNWrapperN(x: /*boxed*/ FooNWrapper?): String
    fun describeNativeFooWrapper(x: NativeFooWrapper): String
    fun describeNativeFooWrapperN(x: NativeFooWrapper?): String
    fun describeNativeFooNWrapper(x: NativeFooNWrapper): String
    fun describeNativeFooNWrapperN(x: /*boxed*/ NativeFooNWrapper?): String

    var intWrapper: IntWrapper
    var intWrapperN: IntWrapper?
    var intNWrapper: IntNWrapper
    var intNWrapperN: /*boxed*/ IntNWrapper?
    var fooWrapper: FooWrapper
    var fooWrapperN: FooWrapper?
    var fooNWrapper: FooNWrapper
    var fooNWrapperN: /*boxed*/ FooNWrapper?
    var nativeFooWrapper: NativeFooWrapper
    var nativeFooWrapperN: NativeFooWrapper?
    var nativeFooNWrapper: NativeFooNWrapper
    var nativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

    val readOnlyIntWrapper: IntWrapper
    val readOnlyIntWrapperN: IntWrapper?
    val readOnlyIntNWrapper: IntNWrapper
    val readOnlyIntNWrapperN: /*boxed*/ IntNWrapper?
    val readOnlyFooWrapper: FooWrapper
    val readOnlyFooWrapperN: FooWrapper?
    val readOnlyFooNWrapper: FooNWrapper
    val readOnlyFooNWrapperN: /*boxed*/ FooNWrapper?
    val readOnlyNativeFooWrapper: NativeFooWrapper
    val readOnlyNativeFooWrapperN: NativeFooWrapper?
    val readOnlyNativeFooNWrapper: NativeFooNWrapper
    val readOnlyNativeFooNWrapperN: /*boxed*/ NativeFooNWrapper?

    fun getIntWrapper(): IntWrapper
    fun getIntWrapperN(): IntWrapper?
    fun getIntNWrapper(): IntNWrapper
    fun getIntNWrapperN(): /*boxed*/ IntNWrapper?
    fun getFooWrapper(): FooWrapper
    fun getFooWrapperN(): FooWrapper?
    fun getFooNWrapper(): FooNWrapper
    fun getFooNWrapperN(): /*boxed*/ FooNWrapper?
    fun getNativeFooWrapper(): NativeFooWrapper
    fun getNativeFooWrapperN(): NativeFooWrapper?
    fun getNativeFooNWrapper(): NativeFooNWrapper
    fun getNativeFooNWrapperN(): /*boxed*/ NativeFooNWrapper?
}

fun testObjectMethodsWithValueClassesInArgs() {
    assertEquals("42 (number)", TestObject.describeIntWrapper(IntWrapper(42)))
    assertEquals("100 (number)", TestObject.describeIntWrapperN(IntWrapper(100)))
    assertEquals("null (object)", TestObject.describeIntWrapperN(null))

    assertEquals("42 (number)", TestObject.describeIntNWrapper(IntNWrapper(42)))
    assertEquals("null (object)", TestObject.describeIntNWrapper(IntNWrapper(null)))
    assertEquals("IntNWrapper(value=100) (object)", TestObject.describeIntNWrapperN(IntNWrapper(100)))
    assertEquals("IntNWrapper(value=null) (object)", TestObject.describeIntNWrapperN(IntNWrapper(null)))
    assertEquals("null (object)", TestObject.describeIntNWrapperN(null))

    assertEquals("Foo(s=hello) (object)", TestObject.describeFooWrapper(FooWrapper(Foo("hello"))))
    assertEquals("Foo(s=goodbye) (object)", TestObject.describeFooWrapperN(FooWrapper(Foo("goodbye"))))
    assertEquals("null (object)", TestObject.describeFooWrapperN(null))

    assertEquals("Foo(s=hello) (object)", TestObject.describeFooNWrapper(FooNWrapper(Foo("hello"))))
    assertEquals("null (object)", TestObject.describeFooNWrapper(FooNWrapper(null)))
    assertEquals("FooNWrapper(fooN=Foo(s=goodbye)) (object)", TestObject.describeFooNWrapperN(FooNWrapper(Foo("goodbye"))))
    assertEquals("FooNWrapper(fooN=null) (object)", TestObject.describeFooNWrapperN(FooNWrapper(null)))
    assertEquals("null (object)", TestObject.describeFooNWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", TestObject.describeNativeFooWrapper(NativeFooWrapper(NativeFoo("hello"))))
    assertEquals("NativeFoo('goodbye') (object)", TestObject.describeNativeFooWrapperN(NativeFooWrapper(NativeFoo("goodbye"))))
    assertEquals("null (object)", TestObject.describeNativeFooWrapperN(null))

    assertEquals("NativeFoo('hello') (object)", TestObject.describeNativeFooNWrapper(NativeFooNWrapper(NativeFoo("hello"))))
    assertEquals("null (object)", TestObject.describeNativeFooNWrapper(NativeFooNWrapper(null)))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('goodbye')) (object)", TestObject.describeNativeFooNWrapperN(NativeFooNWrapper(NativeFoo("goodbye"))))
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", TestObject.describeNativeFooNWrapperN(NativeFooNWrapper(null)))
    assertEquals("null (object)", TestObject.describeNativeFooNWrapperN(null))
}

fun testWritableObjectProperties() {
    assertEquals("null (object)", describeValueOfProperty(TestObject, "intWrapper"))
    TestObject.intWrapper = IntWrapper(42)
    assertEquals("42 (number)", describeValueOfProperty(TestObject, "intWrapper"))
    assertEquals(42, TestObject.intWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "intWrapperN"))
    TestObject.intWrapperN = IntWrapper(100)
    assertEquals("100 (number)", describeValueOfProperty(TestObject, "intWrapperN"))
    assertEquals(100, TestObject.intWrapperN?.value)
    TestObject.intWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestObject, "intWrapperN"))
    assertEquals(null, TestObject.intWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "intNWrapper"))
    TestObject.intNWrapper = IntNWrapper(23)
    assertEquals("23 (number)", describeValueOfProperty(TestObject, "intNWrapper"))
    assertEquals(23, TestObject.intNWrapper.value)
    TestObject.intNWrapper = IntNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(TestObject, "intNWrapper"))
    assertEquals(null, TestObject.intNWrapper.value)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "intNWrapperN"))
    TestObject.intNWrapperN = IntNWrapper(65)
    assertEquals("IntNWrapper(value=65) (object)", describeValueOfProperty(TestObject, "intNWrapperN"))
    assertEquals(65, TestObject.intNWrapperN?.value)
    TestObject.intNWrapperN = IntNWrapper(null)
    assertEquals("IntNWrapper(value=null) (object)", describeValueOfProperty(TestObject, "intNWrapperN"))
    assertEquals(null, TestObject.intNWrapperN?.value)
    TestObject.intNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestObject, "intNWrapperN"))
    assertEquals(null, TestObject.intNWrapperN?.value)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "fooWrapper"))
    TestObject.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("Foo(s=cat) (object)", describeValueOfProperty(TestObject, "fooWrapper"))
    assertEquals("cat", TestObject.fooWrapper.foo.s)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "fooWrapperN"))
    TestObject.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("Foo(s=dog) (object)", describeValueOfProperty(TestObject, "fooWrapperN"))
    assertEquals("dog", TestObject.fooWrapperN?.foo?.s)
    TestObject.fooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestObject, "fooWrapperN"))
    assertEquals(null, TestObject.fooWrapperN?.foo?.s)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "fooNWrapper"))
    TestObject.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("Foo(s=mouse) (object)", describeValueOfProperty(TestObject, "fooNWrapper"))
    assertEquals("mouse", TestObject.fooNWrapper.fooN?.s)
    TestObject.fooNWrapper = FooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(TestObject, "fooNWrapper"))
    assertEquals(null, TestObject.fooNWrapper.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "fooNWrapperN"))
    TestObject.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("FooNWrapper(fooN=Foo(s=fox)) (object)", describeValueOfProperty(TestObject, "fooNWrapperN"))
    assertEquals("fox", TestObject.fooNWrapperN?.fooN?.s)
    TestObject.fooNWrapperN = FooNWrapper(null)
    assertEquals("FooNWrapper(fooN=null) (object)", describeValueOfProperty(TestObject, "fooNWrapperN"))
    assertEquals(null, TestObject.fooNWrapperN?.fooN?.s)
    TestObject.fooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestObject, "fooNWrapperN"))
    assertEquals(null, TestObject.fooNWrapperN?.fooN?.s)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "nativeFooWrapper"))
    TestObject.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("NativeFoo('Berlin') (object)", describeValueOfProperty(TestObject, "nativeFooWrapper"))
    assertEquals("Berlin", TestObject.nativeFooWrapper.nativeFoo.s)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "nativeFooWrapperN"))
    TestObject.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("NativeFoo('Amsterdam') (object)", describeValueOfProperty(TestObject, "nativeFooWrapperN"))
    assertEquals("Amsterdam", TestObject.nativeFooWrapperN?.nativeFoo?.s)
    TestObject.nativeFooWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestObject, "nativeFooWrapperN"))
    assertEquals(null, TestObject.nativeFooWrapperN?.nativeFoo?.s)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "nativeFooNWrapper"))
    TestObject.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("NativeFoo('Saint-Petersburg') (object)", describeValueOfProperty(TestObject, "nativeFooNWrapper"))
    assertEquals("Saint-Petersburg", TestObject.nativeFooNWrapper.nativeFooN?.s)
    TestObject.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals("null (object)", describeValueOfProperty(TestObject, "nativeFooNWrapper"))
    assertEquals(null, TestObject.nativeFooNWrapper.nativeFooN?.s)

    assertEquals("null (object)", describeValueOfProperty(TestObject, "nativeFooNWrapperN"))
    TestObject.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("NativeFooNWrapper(nativeFooN=NativeFoo('Boston')) (object)", describeValueOfProperty(TestObject, "nativeFooNWrapperN"))
    assertEquals("Boston", TestObject.nativeFooNWrapperN?.nativeFooN?.s)
    TestObject.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals("NativeFooNWrapper(nativeFooN=null) (object)", describeValueOfProperty(TestObject, "nativeFooNWrapperN"))
    assertEquals(null, TestObject.nativeFooNWrapperN?.nativeFooN?.s)
    TestObject.nativeFooNWrapperN = null
    assertEquals("null (object)", describeValueOfProperty(TestObject, "nativeFooNWrapperN"))
    assertEquals(null, TestObject.nativeFooNWrapperN?.nativeFooN?.s)

    nullifyTestProperties(TestObject)
}

fun testReadOnlyObjectProperties() {
    TestObject.intWrapper = IntWrapper(42)
    assertEquals(42, TestObject.readOnlyIntWrapper.value)

    assertEquals(null, TestObject.readOnlyIntWrapperN?.value)
    TestObject.intWrapperN = IntWrapper(100)
    assertEquals(100, TestObject.readOnlyIntWrapperN?.value)

    TestObject.intNWrapper = IntNWrapper(23)
    assertEquals(23, TestObject.readOnlyIntNWrapper.value)
    TestObject.intNWrapper = IntNWrapper(null)
    assertEquals(null, TestObject.readOnlyIntNWrapper.value)

    assertEquals(null, TestObject.readOnlyIntNWrapperN?.value)
    TestObject.intNWrapperN = IntNWrapper(65)
    assertEquals(65, TestObject.readOnlyIntNWrapperN?.value)
    TestObject.intNWrapperN = IntNWrapper(null)
    assertEquals(null, TestObject.readOnlyIntNWrapperN?.value)

    TestObject.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", TestObject.readOnlyFooWrapper.foo.s)

    assertEquals(null, TestObject.readOnlyFooWrapperN?.foo?.s)
    TestObject.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", TestObject.readOnlyFooWrapperN?.foo?.s)

    TestObject.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", TestObject.readOnlyFooNWrapper.fooN?.s)
    TestObject.fooNWrapper = FooNWrapper(null)
    assertEquals(null, TestObject.readOnlyFooNWrapper.fooN?.s)

    assertEquals(null, TestObject.readOnlyFooNWrapperN?.fooN?.s)
    TestObject.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", TestObject.readOnlyFooNWrapperN?.fooN?.s)
    TestObject.fooNWrapperN = FooNWrapper(null)
    assertEquals(null, TestObject.readOnlyFooNWrapperN?.fooN?.s)

    TestObject.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", TestObject.readOnlyNativeFooWrapper.nativeFoo.s)

    assertEquals(null, TestObject.readOnlyNativeFooWrapperN?.nativeFoo?.s)
    TestObject.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", TestObject.readOnlyNativeFooWrapperN?.nativeFoo?.s)

    TestObject.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", TestObject.readOnlyNativeFooNWrapper.nativeFooN?.s)
    TestObject.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, TestObject.readOnlyNativeFooNWrapper.nativeFooN?.s)

    assertEquals(null, TestObject.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    TestObject.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", TestObject.readOnlyNativeFooNWrapperN?.nativeFooN?.s)
    TestObject.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, TestObject.readOnlyNativeFooNWrapperN?.nativeFooN?.s)

    nullifyTestProperties(TestObject)
}

fun testObjectMethodsWithValueClassinReturnType() {
    TestObject.intWrapper = IntWrapper(42)
    assertEquals(42, TestObject.getIntWrapper().value)

    assertEquals(null, TestObject.getIntWrapperN()?.value)
    TestObject.intWrapperN = IntWrapper(100)
    assertEquals(100, TestObject.getIntWrapperN()?.value)

    TestObject.intNWrapper = IntNWrapper(23)
    assertEquals(23, TestObject.getIntNWrapper().value)
    TestObject.intNWrapper = IntNWrapper(null)
    assertEquals(null, TestObject.getIntNWrapper().value)

    assertEquals(null, TestObject.getIntNWrapperN()?.value)
    TestObject.intNWrapperN = IntNWrapper(65)
    assertEquals(65, TestObject.getIntNWrapperN()?.value)
    TestObject.intNWrapperN = IntNWrapper(null)
    assertEquals(null, TestObject.getIntNWrapperN()?.value)

    TestObject.fooWrapper = FooWrapper(Foo("cat"))
    assertEquals("cat", TestObject.getFooWrapper().foo.s)

    assertEquals(null, TestObject.getFooWrapperN()?.foo?.s)
    TestObject.fooWrapperN = FooWrapper(Foo("dog"))
    assertEquals("dog", TestObject.getFooWrapperN()?.foo?.s)

    TestObject.fooNWrapper = FooNWrapper(Foo("mouse"))
    assertEquals("mouse", TestObject.getFooNWrapper().fooN?.s)
    TestObject.fooNWrapper = FooNWrapper(null)
    assertEquals(null, TestObject.getFooNWrapper().fooN?.s)

    assertEquals(null, TestObject.getFooNWrapperN()?.fooN?.s)
    TestObject.fooNWrapperN = FooNWrapper(Foo("fox"))
    assertEquals("fox", TestObject.getFooNWrapperN()?.fooN?.s)
    TestObject.fooNWrapperN = FooNWrapper(null)
    assertEquals(null, TestObject.getFooNWrapperN()?.fooN?.s)

    TestObject.nativeFooWrapper = NativeFooWrapper(NativeFoo("Berlin"))
    assertEquals("Berlin", TestObject.getNativeFooWrapper().nativeFoo.s)

    assertEquals(null, TestObject.getNativeFooWrapperN()?.nativeFoo?.s)
    TestObject.nativeFooWrapperN = NativeFooWrapper(NativeFoo("Amsterdam"))
    assertEquals("Amsterdam", TestObject.getNativeFooWrapperN()?.nativeFoo?.s)

    TestObject.nativeFooNWrapper = NativeFooNWrapper(NativeFoo("Saint-Petersburg"))
    assertEquals("Saint-Petersburg", TestObject.getNativeFooNWrapper().nativeFooN?.s)
    TestObject.nativeFooNWrapper = NativeFooNWrapper(null)
    assertEquals(null, TestObject.getNativeFooNWrapper().nativeFooN?.s)

    assertEquals(null, TestObject.getNativeFooNWrapperN()?.nativeFooN?.s)
    TestObject.nativeFooNWrapperN = NativeFooNWrapper(NativeFoo("Boston"))
    assertEquals("Boston", TestObject.getNativeFooNWrapperN()?.nativeFooN?.s)
    TestObject.nativeFooNWrapperN = NativeFooNWrapper(null)
    assertEquals(null, TestObject.getNativeFooNWrapperN()?.nativeFooN?.s)

    nullifyTestProperties(TestObject)
}

fun box(): String {
    testFreeFunctionsWithValueClassesInArgs()
    testWritableGlobalProperties()
    testReadOnlyGlobalProperties()
    testFreeFunctionsWithValueClassInReturnType()

    testClassConstructor()
    testClassMethodsWithValueClassesInArgs()
    testWritableClassProperties()
    testReadOnlyClassProperties()
    testClassMethodsWithValueClassInReturnType()

    testCompanionObjectMethodsWithValueClassesInArgs()
    testWritableCompanionObjectProperties()
    testReadOnlyCompanionObjectProperties()
    testCompanionObjectMethodsWithValueClassinReturnType()

    testInterfaceMethodsWithValueClassesInArgs()
    testWritableInterfaceProperties()
    testReadOnlyInterfaceProperties()
    testInterfaceMethodsWithValueClassInReturnType()

    testObjectMethodsWithValueClassesInArgs()
    testWritableObjectProperties()
    testReadOnlyObjectProperties()
    testObjectMethodsWithValueClassinReturnType()

    return "OK"
}
