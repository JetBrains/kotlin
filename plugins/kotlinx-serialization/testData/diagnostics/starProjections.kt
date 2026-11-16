// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import kotlinx.serialization.*

@Serializable
class Box<T>(val boxed: T)

@Serializable
class Wrapper(val boxed: Box<<!SERIALIZER_NOT_FOUND!>*<!>>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, starProjection,
typeParameter */
