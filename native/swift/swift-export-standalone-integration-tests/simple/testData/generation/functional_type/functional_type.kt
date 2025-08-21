// KIND: STANDALONE
// MODULE: simple
// EXPORT_TO_SWIFT
// FILE: simple.kt

fun foo_sus(): suspend ()->Unit = TODO()
fun foo_1(): ()->Unit = TODO()

fun foo_consume_simple(block: ()->Unit): Unit = TODO()

fun foo_consume_recursive(block: (()->Unit)->(()->Unit)): Unit = TODO()

var closure_property: () -> Unit = {}

// MODULE: strings
// EXPORT_TO_SWIFT
// FILE: strings.kt

fun consume_block_with_string_id(block: (String) -> String): String = TODO()

// MODULE: primitive_types
// EXPORT_TO_SWIFT
// FILE: primitive_types.kt

fun consume_block_with_uint_id(block: (UInt) -> UInt): UInt = TODO()
fun consume_block_with_byte_id(block: (Byte) -> Byte): Byte = TODO()
// todo: crashes konan backend: Internal compiler error: doesn't correspond to any C type: kotlin.Unit
// at convertBlockPtrToKotlinFunction<(Unit)->Byte>(block)
// fun consume_block_with_Unit_id(block: (Unit) -> Byte): Byte = TODO()

// MODULE: never_type
// EXPORT_TO_SWIFT
// FILE: never_type.kt

// todo: support(currently code gen tries to dereference Nothing)
// fun consume_block_with_never(block: (Nothing) -> Nothing): Nothing = TODO()
// todo: support(as with functions - we can drop optional nothings from C representation)
// fun consume_block_with_opt_never(block: (Int, Nothing?) -> Int): Nothing = TODO()

// MODULE: optional_closure
// EXPORT_TO_SWIFT
// FILE: optional_closure.kt

fun consume_opt_closure(arg: (()->Unit)?): Unit = TODO()
fun produce_opt_closure(arg: Unit): (()->Unit)? = TODO()
fun consume_producing_opt_closure(arg: (()->(()->Unit)?)?): Unit = TODO()
fun consume_consuming_opt_closure(arg: (((()->Unit)?)->Unit)?): Unit = TODO()

// MODULE: functional_types
// EXPORT_TO_SWIFT
// FILE: functional_types.kt

// more complex types are not supported
// todo: current generation has some assumptions about variable names created before the bridge. Sould be reworked.
// fun consume_block_consuming_block(block: (()->Unit) -> Unit): Unit = TODO()

// MODULE: collections(data)
// EXPORT_TO_SWIFT
// FILE: collections.kt

fun consume_block_with_listRef_id(block: (List<Foo>) -> List<Foo>): List<Foo> = TODO()
// todo: KT-75183 problem with settranslation, not with closures
// fun consume_block_with_setRef_id(block: (Set<Foo>) -> Set<Foo>): Set<Foo> = TODO()
fun consume_block_with_dictRef_id(block: (Map<String, Foo>) -> Map<String, Foo>): Map<String, Foo> = TODO()

fun consume_block_with_list_id(block: (List<Int>) -> List<Int>): List<Int> = TODO()
fun consume_block_with_set_id(block: (Set<Int>) -> Set<Int>): Set<Int> = TODO()
fun consume_block_with_dict_id(block: (Map<Int, Int>) -> Map<Int, Int>): Map<Int, Int> = TODO()

// MODULE: ref_types(data)
// EXPORT_TO_SWIFT
// FILE: ref_types.kt

fun consume_block_with_reftype_factory(block: () -> Foo): Foo = TODO()
fun consume_block_with_reftype_consumer(block: (Foo) -> Unit): Unit = TODO()
fun consume_block_with_reftype_zip(block: Function2<Foo, Foo, Bar>): Bar = TODO()
fun consume_block_with_reftype_unzip(block: Function1<Bar, Foo>): Foo = TODO()

fun consume_block_with_opt_reftype(block: Function4<Int?, Bar?, String?, Set<Any>?, Foo?>): Unit = TODO()

// MODULE: receivers
// EXPORT_TO_SWIFT
// FILE: receivers.kt

fun foo(i: Int.()->Unit): Unit = TODO()
fun fooString(i: String?.()->Unit): Unit = TODO()
fun fooAny(i: Any.()->Unit): Unit = TODO()
fun fooList(i: List<Int>.()->Unit): Unit = TODO()

// MODULE: typealias_to_closure
// EXPORT_TO_SWIFT
// FILE: typealias_to_closure.kt

typealias Closure = () -> Unit

fun typealias_demo(input: Closure): Closure = TODO()

// MODULE: data
// EXPORT_TO_SWIFT
// FILE: data.kt

class Foo
class Bar
