fun test() {
    val x: /*T6@*/Function2</*T3@*/Int, /*T4@*/String, /*T5@*/Boolean> = l@{ a: /*T0@*/Int, b: /*T1@*/String ->
        val y: /*T2@*/Boolean = true/*LIT*/
        if (a == 3) {
            return@l y/*T2@Boolean*/
        }
        true/*LIT*/
    }/*Function2<T0@Int, T1@String, T7@Boolean>*//*Function2<T0@Int, T1@String, T7@Boolean>*/
}

//LOWER <: T2 due to 'INITIALIZER'
//T2 <: T7 due to 'RETURN'
//LOWER <: T7 due to 'RETURN'
//T3 <: T0 due to 'INITIALIZER'
//T4 <: T1 due to 'INITIALIZER'
//T7 <: T5 due to 'INITIALIZER'
