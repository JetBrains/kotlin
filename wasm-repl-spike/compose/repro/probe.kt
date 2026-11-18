@file:OptIn(kotlin.ExperimentalStdlibApi::class)
import kotlin.jvm.JvmInline

// shape A: plain public value class
@JvmInline value class VCPub(val x: Int)
// shape B: internal @PublishedApi constructor (compose's MultiValueMap shape)
@JvmInline value class VCPub2<K, V> @PublishedApi internal constructor(@PublishedApi internal val backing: MutableList<Any?>)
// shape C: private constructor property
@JvmInline value class VCPriv(val x: Int)

fun usePub(a: Any): Int = (a as VCPub).x
fun usePub2(a: Any): Int = (a as VCPub2<Int, Int>).backing.size
fun usePriv(a: Any): Int = (a as VCPriv).x

inline fun inlineUsePub(a: Any): Int = (a as VCPub).x
