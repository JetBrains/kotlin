fun test(a: /*T0@*/Boolean) {
    while (a/*T0@Boolean*/) {}
}

//T0 := LOWER due to 'USE_AS_RECEIVER'
