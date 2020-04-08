fun f(list: List<Int>) {
    val result = list.filter { it % 2 == 0 }
            .max() ?: -1
    list.filter { it % 2 == 0 }
            .max()
            ?: -1
}

// SET_INT: METHOD_CALL_CHAIN_WRAP = 2
// SET_FALSE: WRAP_FIRST_METHOD_IN_CALL_CHAIN