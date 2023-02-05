# Module kotlin-reflect

## Kotlin JVM reflection extensions

The library provides the runtime component that enables full support of the reflection features in `kotlin.reflect` package 
and extensions for the reflection types.

It's provided as an optional artifact separate from the standard library to reduce the required size of the runtime dependencies
for applications that do not use reflection features. 

# Package kotlin.reflect.full

Extensions for [Kotlin reflection](https://kotlinlang.org/docs/reference/reflection.html) types like [kotlin.reflect.KClass], [kotlin.reflect.KType], and others.

# Package kotlin.reflect.jvm

Extensions for conversion between [Kotlin reflection](https://kotlinlang.org/docs/reference/reflection.html) and
Java reflection types and other JVM-specific extensions.
