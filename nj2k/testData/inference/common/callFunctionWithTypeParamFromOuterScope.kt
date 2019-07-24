class X<T> {
    fun x(): /*T1@*/List</*T0@*/T> {
        TODO()
    }

    fun y() {
        val a: /*T3@*/List</*T2@*/T> = x()/*T1@List<T0@T>*/
    }
}

//T0 <: T2 due to 'INITIALIZER'
//T1 <: T3 due to 'INITIALIZER'
