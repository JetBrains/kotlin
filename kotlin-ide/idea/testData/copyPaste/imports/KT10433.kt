package p

class X {
    companion object { val v = 1 }
}

fun c(value:Int, text:String) {}

fun f() {
    c(<selection>X.v</selection>, "")
}

