class Test {
    fun foo1(r: /*T2@*/Function1</*T0@*/Int, /*T1@*/String>) {}
    fun foo() {
        foo1 { i: /*T3@*/Int ->
            val str: /*T4@*/String = ""/*LIT*/
            val str2: /*T5@*/String = ""/*LIT*/

            if (i > 1) {
                return@foo1 str/*T4@String*/
            }
            str2/*T5@String*/
        }/*Function1<T3@Int, T6@String>*/
    }
}

//LOWER <: T4 due to 'INITIALIZER'
//LOWER <: T5 due to 'INITIALIZER'
//T4 <: T6 due to 'RETURN'
//T5 <: T6 due to 'RETURN'
//T0 <: T3 due to 'PARAMETER'
//T6 <: T1 due to 'PARAMETER'
