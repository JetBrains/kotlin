fun b(m: /*T2@*/Map</*T0@*/Int, /*T1@*/String>): /*T3@*/String? {
    return m/*T2@Map<T0@Int, T1@String>*/.get(42/*LIT*/)/*T1@String!!U*/
}

//T0 := T0 due to 'RECEIVER_PARAMETER'
//T1 <: T1 due to 'RECEIVER_PARAMETER'
//LOWER <: T0 due to 'PARAMETER'
//T2 := LOWER due to 'USE_AS_RECEIVER'
//UPPER <: T3 due to 'RETURN'
