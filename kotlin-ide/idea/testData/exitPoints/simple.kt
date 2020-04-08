fun f(a: Int): Int {
    if (a < 5) {
        <caret>return 1
    }
    else {
        return 2
    }
}

//HIGHLIGHTED: return 1
//HIGHLIGHTED: return 2