class Test {
    fun foo() {
        val ss = Array</*T2@*/Array</*T1@*/String?>>(5/*LIT*/, { arrayOfNulls</*T0@*/String?>(5/*LIT*/)/*Array<T0@String!!U>!!L*/ }/*Function0<Int, T3@Array<T4@String>>!!L*/)
    }
}

//T4 := UPPER due to 'RETURN'
//LOWER <: T3 due to 'RETURN'
//T1 := T4 due to 'PARAMETER'
//T3 <: T2 due to 'PARAMETER'
