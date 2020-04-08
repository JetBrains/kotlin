class X<T>(x: /*T0@*/T) {
    constructor(x: /*T1@*/T, y: /*T2@*/Int): this(x/*T1@T*/)
}

//T1 <: T0 due to 'PARAMETER'
