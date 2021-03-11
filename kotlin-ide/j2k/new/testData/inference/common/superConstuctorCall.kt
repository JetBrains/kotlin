open class X<T>(x: /*T0@*/T) {
}

class Y<T>(x: /*T1@*/T) : X</*T2@*/T>(x/*T1@T*/) {
}

//T1 <: T0 due to 'PARAMETER'
