package foo

var b = 0

fun loop(var times : Int) {
   while(times > 0) {
        val u : fun(value : Int) : Unit = {
            b = b + 1
        }
        u(times--)
   }
}

fun box() : Boolean {
    loop(5)
    return b == 5
}