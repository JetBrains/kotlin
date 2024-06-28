@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public inline fun <E> kotlin.js.collections.JsReadonlyArray<E>.toList(): kotlin.collections.List<E>

@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public inline fun <K, V> kotlin.js.collections.JsReadonlyMap<K, V>.toMap(): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public inline fun <E> kotlin.js.collections.JsReadonlyArray<E>.toMutableList(): kotlin.collections.MutableList<E>

@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public inline fun <K, V> kotlin.js.collections.JsReadonlyMap<K, V>.toMutableMap(): kotlin.collections.MutableMap<K, V>

@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public inline fun <E> kotlin.js.collections.JsReadonlySet<E>.toMutableSet(): kotlin.collections.MutableSet<E>

@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public inline fun <E> kotlin.js.collections.JsReadonlySet<E>.toSet(): kotlin.collections.Set<E>

@kotlin.js.JsName(name = "Array")
@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public open external class JsArray<E> : kotlin.js.collections.JsReadonlyArray<E> {
    public constructor JsArray<E>()
}

@kotlin.js.JsName(name = "Map")
@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public open external class JsMap<K, V> : kotlin.js.collections.JsReadonlyMap<K, V> {
    public constructor JsMap<K, V>()
}

@kotlin.js.JsName(name = "ReadonlyArray")
@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public external interface JsReadonlyArray<out E> {
}

@kotlin.js.JsName(name = "ReadonlyMap")
@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public external interface JsReadonlyMap<K, out V> {
}

@kotlin.js.JsName(name = "ReadonlySet")
@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public external interface JsReadonlySet<out E> {
}

@kotlin.js.JsName(name = "Set")
@kotlin.SinceKotlin(version = "2.0")
@kotlin.js.ExperimentalJsCollectionsApi
public open external class JsSet<E> : kotlin.js.collections.JsReadonlySet<E> {
    public constructor JsSet<E>()
}