@kotlin.js.JsName(name = "Array")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.js.ExperimentalJsCollectionsApi
public open external class JsArray<E> : kotlin.js.collections.JsReadonlyArray<E> {
    public constructor JsArray<E>()
}

@kotlin.js.JsName(name = "Map")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.js.ExperimentalJsCollectionsApi
public open external class JsMap<K, V> : kotlin.js.collections.JsReadonlyMap<K, V> {
    public constructor JsMap<K, V>()
}

@kotlin.js.JsName(name = "ReadonlyArray")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.js.ExperimentalJsCollectionsApi
public external interface JsReadonlyArray<out E> {
}

@kotlin.js.JsName(name = "ReadonlyMap")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.js.ExperimentalJsCollectionsApi
public external interface JsReadonlyMap<K, out V> {
}

@kotlin.js.JsName(name = "ReadonlySet")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.js.ExperimentalJsCollectionsApi
public external interface JsReadonlySet<out E> {
}

@kotlin.js.JsName(name = "Set")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.js.ExperimentalJsCollectionsApi
public open external class JsSet<E> : kotlin.js.collections.JsReadonlySet<E> {
    public constructor JsSet<E>()
}
