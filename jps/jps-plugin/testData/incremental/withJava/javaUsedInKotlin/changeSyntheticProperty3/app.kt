fun main() {
    val call = Jaba::foo
    println(call.invoke(Jaba()))
}

//KT-55393