package foo

var c = 2

fun loop(var times : Int) {
   while(times > 0) {
        val u : (value : Int) -> Unit = {
            c++
        }
        u(times--)
   }
}

fun box() : Any? {
    loop(5)
    return if (c == 7) return "OK" else c
}