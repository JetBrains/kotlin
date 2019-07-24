fun test() {
    val x: /*T2@*/Array</*T1@*/Int?> = arrayOfNulls</*T0@*/Int?>(10/*LIT*/)/*Array<T0@Int!!U>!!L*/
}

//T1 := UPPER due to 'INITIALIZER'
//LOWER <: T2 due to 'INITIALIZER'
