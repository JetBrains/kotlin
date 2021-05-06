class A {

    operator fun invoke(){
        this()
    }

    operator fun invoke(f: ()-> Unit){

    }

}

fun main(a: A?){
    a!!()
    val p = A()
    a!! {
        p {
            A()()
        }
    }
}
