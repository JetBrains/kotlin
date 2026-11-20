// TARGET_BACKEND: JVM
// WITH_STDLIB

// Two Java classes whose type-parameter bounds name each other's nested types through imports.
// With a declaration-generating plugin in the session, these must be resolved from the Java source
// rather than through the composite symbol provider, which would re-enter the class being built.

// FILE: single/A.java
package single;

import single.B.BN;

public interface A<T extends BN> {
    T get();

    interface AN {}
}

// FILE: single/B.java
package single;

import single.A.AN;

public interface B<U extends AN> {
    U get();

    interface BN {}
}

// FILE: staticStar/A.java
package staticStar;

import static staticStar.B.*;

public interface A<T extends BN> {
    T get();

    interface AN {}
}

// FILE: staticStar/B.java
package staticStar;

import static staticStar.A.*;

public interface B<U extends AN> {
    U get();

    interface BN {}
}

// FILE: typeStar/A.java
package typeStar;

import typeStar.B.*;

public interface A<T extends BN> {
    T get();

    interface AN {}
}

// FILE: typeStar/B.java
package typeStar;

import typeStar.A.*;

public interface B<U extends AN> {
    U get();

    interface BN {}
}

// FILE: main.kt
package com.other

class Single(val value: String) : single.A.AN, single.B.BN
class StaticStar(val value: String) : staticStar.A.AN, staticStar.B.BN
class TypeStar(val value: String) : typeStar.A.AN, typeStar.B.BN

fun <T : single.B.BN> singleA(a: single.A<T>): T = a.get()
fun <U : single.A.AN> singleB(b: single.B<U>): U = b.get()
fun <T : staticStar.B.BN> staticStarA(a: staticStar.A<T>): T = a.get()
fun <U : staticStar.A.AN> staticStarB(b: staticStar.B<U>): U = b.get()
fun <T : typeStar.B.BN> typeStarA(a: typeStar.A<T>): T = a.get()
fun <U : typeStar.A.AN> typeStarB(b: typeStar.B<U>): U = b.get()

fun box(): String {
    val result = singleA(single.A { Single("O") }).value +
            singleB(single.B { Single("K") }).value +
            staticStarA(staticStar.A { StaticStar("O") }).value +
            staticStarB(staticStar.B { StaticStar("K") }).value +
            typeStarA(typeStar.A { TypeStar("O") }).value +
            typeStarB(typeStar.B { TypeStar("K") }).value
    return if (result == "OKOKOK") "OK" else "Fail: $result"
}
