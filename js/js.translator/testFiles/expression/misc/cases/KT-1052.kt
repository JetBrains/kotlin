fun main(args: Array<String>) {
    println(true.and1(true))
}

fun Boolean.and1(other: Boolean): Boolean {
    if (other == true) {
        if (this == true) {
            return true ;
        }
        else {
            return false;
        }
    }
    else {
        return false;
    }
}
