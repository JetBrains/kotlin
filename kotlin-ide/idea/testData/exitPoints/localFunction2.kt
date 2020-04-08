fun f(a: Int): Int {
    fun localFun() {
        return
    }

    if (a < 5) {
        return 1
    }
    else {
        <caret>return 2
    }
}

//HIGHLIGHTED: return 1
//HIGHLIGHTED: return 2
