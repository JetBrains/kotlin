fun notNullParameters(f: /*T3@*/Function2</*T0@*/Int, /*T1@*/Int, /*T2@*/String>) {}
fun nullableParameter(f: /*T7@*/Function2</*T4@*/Int?, /*T5@*/Int, /*T6@*/String>) {}
fun nullableReturnType(f: /*T11@*/Function2</*T8@*/Int, /*T9@*/Int, /*T10@*/String?>) {}


fun test() {
    notNullParameters({ i: /*T12@*/Int, j: /*T13@*/Int ->
                          if (i/*T12@Int*/ < 10/*LIT*//*LIT*/ && j/*T13@Int*/ > 0/*LIT*//*LIT*//*LIT*/) ""/*LIT*/ else ""/*LIT*/
                      }/*Function2<T12@Int, T13@Int, T18@String>!!L*/)

    nullableParameter({ i: /*T14@*/Int?, j: /*T15@*/Int ->
                          if (i/*T14@Int*/ == null/*LIT*/) ""/*LIT*/ else ""/*LIT*/
                      }/*Function2<T14@Int, T15@Int, T19@String>!!L*/)

    nullableReturnType({ i: /*T16@*/Int, j: /*T17@*/Int ->
                           if (i/*T16@Int*/ < 10/*LIT*//*LIT*/) return@nullableReturnType null/*NULL!!U*/
                           return@nullableReturnType "nya"/*LIT*/
                       }/*Function2<T16@Int, T17@Int, T20@String>!!L*/)
}

//T12 := LOWER due to 'USE_AS_RECEIVER'
//T13 := LOWER due to 'USE_AS_RECEIVER'
//LOWER <: T18 due to 'RETURN'
//T0 <: T12 due to 'PARAMETER'
//T1 <: T13 due to 'PARAMETER'
//T18 <: T2 due to 'PARAMETER'
//LOWER <: T3 due to 'PARAMETER'
//T14 := UPPER due to 'COMPARE_WITH_NULL'
//LOWER <: T19 due to 'RETURN'
//T4 <: T14 due to 'PARAMETER'
//T5 <: T15 due to 'PARAMETER'
//T19 <: T6 due to 'PARAMETER'
//LOWER <: T7 due to 'PARAMETER'
//T16 := LOWER due to 'USE_AS_RECEIVER'
//UPPER <: T20 due to 'RETURN'
//LOWER <: T20 due to 'RETURN'
//T8 <: T16 due to 'PARAMETER'
//T9 <: T17 due to 'PARAMETER'
//T20 <: T10 due to 'PARAMETER'
//LOWER <: T11 due to 'PARAMETER'
