package inlineFun1

class A<T> {
    fun test() = 1

    inline fun myFun1(f: () -> T): T {
        test()
        return f()
    }
}