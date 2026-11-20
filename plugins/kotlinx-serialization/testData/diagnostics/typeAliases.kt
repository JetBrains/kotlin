// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import kotlinx.serialization.*

typealias MyString = String
typealias MyLong = Long

@Serializable
class Box<T>(val t: T)

typealias MyBox<T> = Box<T>

@Serializable
class Foo(
    val s: MyString,
    val l: MyLong,
    val b: Box<MyLong>,
    val bb: MyBox<Long>,
    val bbb: MyBox<MyLong>
)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
