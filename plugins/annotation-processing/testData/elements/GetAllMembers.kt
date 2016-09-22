annotation class Anno

@Anno
class MyClass {
    var myProperty: String = "A"
    
    fun myFunction() = object : java.lang.Runnable {
        override fun run() {}
    }
}