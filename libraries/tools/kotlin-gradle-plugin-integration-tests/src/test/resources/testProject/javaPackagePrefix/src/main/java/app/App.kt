package my.pack.name.app

import my.pack.name.util.Appender
import my.pack.name.util.JUtil

class MyApp {
    fun method(arg: String): String {
        return JUtil.util() + Appender().append(arg)
    }
}
