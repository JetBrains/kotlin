package test.pkg

class Context {
    inline fun <reified T> ownSystemService1() = getSystemService(T::class.java)
    inline fun ownSystemService2() = getSystemService(String::class.java)
}


inline fun <reified T> Context.systemService1() = getSystemService(T::class.java)
inline fun Context.systemService2() = getSystemService(String::class.java)
