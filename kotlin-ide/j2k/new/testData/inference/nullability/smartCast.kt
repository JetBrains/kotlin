fun foo(o: /*T0@*/Int??) {
    if (o/*T0@Int*/ == null/*LIT*/) return
    val a: /*T1@*/Int = o/*T0@Int!!L*/
}

//T0 := UPPER due to 'COMPARE_WITH_NULL'
//LOWER <: T1 due to 'INITIALIZER'
