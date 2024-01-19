// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

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