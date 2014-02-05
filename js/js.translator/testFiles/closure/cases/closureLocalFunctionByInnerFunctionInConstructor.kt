package foo

fun run<T>(f: () -> T) = f()

class Foo {
    val OK = "OK";
    var result: String = ""
    {
        fun bar(s: String? = null) {
            if (s != null) {
                result = s
                return
            }

            run {
                bar(OK)
            }
        }
        bar();
    }

}

fun box(): String {
    return Foo().result
}
