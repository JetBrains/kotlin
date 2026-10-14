// TARGET_BACKEND: JVM
// WITH_STDLIB

// A Java class whose type-parameter bound names its own nested type. With a declaration-generating plugin in the
// session, probing the nested type through the composite symbol provider re-enters the outer class while its own
// declarations are being built, so the bound must be resolved from the Java source alone.

// FILE: com/example/RequestBuilder.java
package com.example;

public interface RequestBuilder<T, R extends RequestBuilder.RequestFormBodyPart> {
    T build(R part);

    interface RequestFormBodyPart {
        String header();
    }
}

// FILE: main.kt
package com.other

import com.example.RequestBuilder

class Part(val value: String) : RequestBuilder.RequestFormBodyPart {
    override fun header(): String = value
}

fun <T, R : RequestBuilder.RequestFormBodyPart> fill(builder: RequestBuilder<T, R>, part: R): T =
    builder.build(part)

fun box(): String {
    return fill(RequestBuilder<String, Part> { it.header() }, Part("OK"))
}
