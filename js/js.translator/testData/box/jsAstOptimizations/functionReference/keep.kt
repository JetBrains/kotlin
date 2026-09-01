// CHECK_OPTIMIZED_JS
// WITH_STDLIB

// FILE: keep.kt

import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1

fun interface I {
    fun run(): Int
}

class Named(val k: KFunction<*>)

// EXPECT_GENERATED_JS: function=readName$ref;dynamicName$ref;equalsRefs$ref;hashCodeRef$ref;typeofCheck$ref;samEquals$ref;samHashCode$ref;exportedRef$ref;externalJs$ref;mixedNameAndInvoke$ref;castFromAny$ref;virtualOverride$ref;dynamicIndex$ref;dynamicPlus$ref;boxStoredName$ref expect=keep.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=readName$ref;dynamicName$ref;equalsRefs$ref;hashCodeRef$ref;typeofCheck$ref;samEquals$ref;samHashCode$ref;exportedRef$ref;externalJs$ref;mixedNameAndInvoke$ref;castFromAny$ref;virtualOverride$ref;dynamicIndex$ref;dynamicPlus$ref;boxStoredName$ref expect=keep.es6.js TARGET_BACKENDS=JS_IR_ES6

fun readName() {}

fun dynamicName() {}

fun equalsRefs() {}

fun hashCodeRef() {}

fun typeofCheck() {}

fun samEquals() = 1

fun samHashCode() = 2

fun exportedRef(x: Int) = x

@JsExport
fun exported(): KFunction1<Int, Int> = ::exportedRef

external fun takeExt(f: (Int) -> Int): Int

fun externalJs(x: Int) = x + 1

fun mixedNameAndInvoke(x: Int) = x

fun castFromAny(x: Int) = x + 3

fun virtualOverride(x: Int) = x

fun dynamicIndex() {}

fun dynamicPlus() {}

fun boxStoredName() {}

class Ctor(val x: Int)

fun useCtor(): Int {
    val f = ::Ctor
    return f(1).x
}

open class Base {
    open fun take(f: (Int) -> Int) = f(10)
}

class Sub : Base() {
    override fun take(f: (Int) -> Int) = f(10) + 1
}

val k_prop = 42

fun takeName(k: KFunction<*>) = k.name

fun takeSamEq(a: I, b: I) = a == b

fun takeSamHash(i: I) = i.hashCode()

fun takeAny(x: Any) = (x as (Int) -> Int)(10)

fun takeVirtual(b: Base) = b.take(::virtualOverride)

fun box(): String {
    if (takeName(::readName) != "readName") return "fail name"

    val d: dynamic = ::dynamicName
    val d_name = d.name

    if (::equalsRefs != ::equalsRefs) return "fail equals"
    ::hashCodeRef.hashCode()

    val t = ::typeofCheck
    if (t !is KFunction<*>) return "fail typeof"

    if (!takeSamEq(::samEquals, ::samEquals)) return "fail sam eq"
    takeSamHash(::samHashCode)

    val exportedK: KFunction1<Int, Int> = exported()
    if (exportedK(9) != 9) return "fail export"

    if (takeExt(::externalJs) != 11) return "fail ext"

    val mixed = ::mixedNameAndInvoke
    if (mixed(1) != 1) return "fail mixed invoke"
    if (mixed.name != "mixedNameAndInvoke") return "fail mixed name"

    if (Named(::boxStoredName).k.name != "boxStoredName") return "fail stored name"

    // still keep: cast from Any to FunctionN is not treated as invoke-only
    if (takeAny(::castFromAny) != 13) return "fail any"

    // still keep: overridable virtual call when the receiver type is the open base
    val base: Base = Sub()
    if (takeVirtual(base) != 11) return "fail virtual"

    // still keep: non-INVOKE dynamic
    val di: dynamic = ::dynamicIndex
    di[0]
    val dp: dynamic = ::dynamicPlus
    dp + 1

    // still keep: constructor refs if they still wrap
    if (useCtor() != 1) return "fail ctor"

    // still keep: property refs go through kpropertyBuilder, not this pass
    if (::k_prop.name != "k_prop") return "fail prop"
    if (::k_prop.get() != 42) return "fail prop get"

    return "OK"
}

// FILE: keep_ext.js
function takeExt(f) {
    return f(10);
}
