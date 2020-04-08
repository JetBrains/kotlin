fun test() {
   <warning descr="[DEPRECATION] 'companion object of MyClass' is deprecated. Use A instead">MyClass</warning>.test
   MyClass()
   val a: MyClass? = null
   val b: MyTrait? = null
   <warning descr="[DEPRECATION] 'companion object of MyTrait' is deprecated. Use A instead">MyTrait</warning>.test
   MyTrait.<warning descr="[DEPRECATION] 'companion object of MyTrait' is deprecated. Use A instead">Companion</warning>
   <warning descr="[DEPRECATION] 'companion object of MyTrait' is deprecated. Use A instead">MyTrait</warning>
   MyClass.<warning descr="[DEPRECATION] 'companion object of MyClass' is deprecated. Use A instead">Companion</warning>
   MyClass.<warning descr="[DEPRECATION] 'companion object of MyClass' is deprecated. Use A instead">Companion</warning>.test

   a == b
}

class MyClass(): MyTrait {
    @Deprecated("Use A instead") companion object {
        val test: String = ""
    }
}

interface MyTrait {
    @Deprecated("Use A instead") companion object {
        val test: String = ""
    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
