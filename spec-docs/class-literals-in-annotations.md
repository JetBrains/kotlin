# Class Literals as Annotation Arguments

Prior to M12 Kotlin annotation allowed parameters of type `java.lang.Class<...>` whose values could be of the form `javaClass<...>()`. 
Since M12 both these options are deprecated, and subject to deletion in further milestones.

## Annotation Parameters

Annotation parameters of type `java.lang.Class<T>` are deprecated.

Annotation parameters of type `kotlin.reflect.KClass<T>` are supported.

A quick-fix transforming one into the other is provided.

## Annotation Arguments

Arguments of the form `javaClass<T>()` are deprecated.

Arguments of teh form `ClassName::class` are supported.

A quick-fix transforming one into the other is provided.

## Loading Annotation Classes from Java

Java annotation `@interfaces` may declare methods of type `java.lang.Class<T>`. And on the JVM this is the only representation we can compile our annotations to. These should be processed specially and mapped to `kotlin.reflect.KClass<T>`. At the call sites for annotations delcared in Kotlin as well as in Java, when a property of type `KClass<T>` is accessed, we have to map its value from `java.lang.Class` to `kotlin.reflect.KClass`. Same needs to happen when we access those properties through reflection.

> This is unprecedented in Kotlin: never before we mapped a Java class to another *real* class, all mappings we had before were fictitious, e.g. existed at compile time only.

Since it is likely to be rather common that the value of an annotation property will only be used to retrieve an instance of `java.lang.Class`, e.g. `annInstance.implClass.java`, to avoid runtime overhead, we should optimize such cases in the JVM back-end by skipping the steps of converting a `java.lamg.Class` to `KClass` and then back.

## Constant Expressions

Class literals become constant expressions (only in annotations).

Usage of `javaClass<T>()` in constant expressions is deprecated.
