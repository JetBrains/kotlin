// ISSUE: KT-91357
// LANGUAGE: +FullValueClasses

value class Id(val value: Int)

inline value class InlineId(val value: Int)

value class Box<T>(val value: T)

inline value class InlineBox<T>(val value: T)

open class Base(id: Id, inlineId: InlineId)

class Derived : Base(Id(0), InlineId(0))

interface Repository {
    val current: Id
    val currentInline: InlineId

    fun find(id: Id): Id?
    fun findInline(id: InlineId): InlineId?
    fun findAll(ids: List<Id>): Map<Id, Box<String>>
    fun unbox(box: Box<List<Id>>): List<Id>
}

interface Wildcards {
    fun full(box: Box<List<CharSequence>>, comparable: Box<Comparable<CharSequence>>): Box<List<CharSequence>>
    fun fullOfFunction(box: Box<(CharSequence) -> CharSequence>): Box<Box<CharSequence>>
    fun inline(box: InlineBox<List<CharSequence>>, comparable: InlineBox<Comparable<CharSequence>>): InlineBox<List<CharSequence>>
}
