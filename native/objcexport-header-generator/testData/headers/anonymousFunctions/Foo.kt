val funProperty: () -> Unit = {}
fun funParam(param: () -> Unit) {}
fun funParamDefault(param: () -> Unit = {}) {}
fun funReturnsFun(): () -> Unit = {}
fun funReturnsNullableFun(): (() -> Unit)? = null