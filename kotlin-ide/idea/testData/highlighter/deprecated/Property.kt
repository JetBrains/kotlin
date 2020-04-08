fun test() {
    <warning descr="[DEPRECATION] 'test1: String' is deprecated. Use A instead">test1</warning> == ""
    MyClass().<warning descr="[DEPRECATION] 'test2: String' is deprecated. Use A instead">test2</warning>
    MyClass.<warning descr="[DEPRECATION] 'test3: String' is deprecated. Use A instead">test3</warning>

    <warning descr="[DEPRECATION] 'test4: String' is deprecated. Use A instead">test4</warning> == ""
    MyClass().<warning descr="[DEPRECATION] 'test5: String' is deprecated. Use A instead">test5</warning>
    MyClass.<warning descr="[DEPRECATION] 'test6: String' is deprecated. Use A instead">test6</warning>
}

@Deprecated("Use A instead") val test1: String = ""
@Deprecated("Use A instead") var test4: String = ""

class MyClass() {
    @Deprecated("Use A instead") val test2: String = ""
    @Deprecated("Use A instead") var test5: String = ""

    companion object {
         @Deprecated("Use A instead") val test3: String = ""
         @Deprecated("Use A instead") var test6: String = ""
    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
