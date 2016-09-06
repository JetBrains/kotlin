annotation class Anno

@Anno
class Test

@Anno
interface Intf

class Test2 {
    @Anno
    fun test2Fun() {}
}

class Test3 {
    @Anno
    class Test3Nested
    
    @Anno
    inner class Test3Inner
}

class Test4

class Test5 {
    fun test5Fun() {} 
}

annotation class Surprise

@Surprise
class Test6

class Test7 {
    @Surprise
    fun test7Fun(): String = "ABC"
}

class Test8 {
    @Anno
    fun test8Fun1() {}

    @Anno
    fun test8Fun2() {}
}