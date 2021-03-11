fun f() {
    array(1, 2, 3).map { v -> v }
    array(1, 2, 3).map<Int> { v -> v }
    array(1, 2, 3).map() { v -> v }
    array(1, 2, 3).map<Int>() { v -> v }
}