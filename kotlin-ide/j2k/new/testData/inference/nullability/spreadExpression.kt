import java.lang.reflect.Constructor

fun <T> foo(constructor: /*T1@*/Constructor</*T0@*/T>, args: /*T3@*/Array</*T2@*/Any?>) {
    constructor/*T1@Constructor<T0@T>*/.newInstance(*args/*T3@Array<T2@Any>*/)
}

//T3 := LOWER due to 'USE_AS_RECEIVER'
//T0 := T0 due to 'RECEIVER_PARAMETER'
//T1 := LOWER due to 'USE_AS_RECEIVER'
