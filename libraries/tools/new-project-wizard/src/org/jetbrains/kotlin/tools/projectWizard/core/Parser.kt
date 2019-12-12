package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import java.nio.file.Paths
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

data class ParsingState(
    val idToTemplate: Map<String, Template>,
    val settingValues: Map<SettingReference<*, *>, Any>
) : ComputeContextState

fun ParsingState.withSettings(newSettings: List<Pair<SettingReference<*, *>, Any>>) =
    copy(settingValues = settingValues + newSettings)

typealias ParsingContext = ComputeContext<ParsingState>

abstract class Parser<out T : Any> {
    abstract fun ParsingContext.parse(value: Any?, path: String): TaskResult<T>
}

fun <T : Any> alwaysFailingParser() = object : Parser<T>() {
    override fun ParsingContext.parse(value: Any?, path: String): TaskResult<T> =
        Failure(object : Error() {
            override val message: String
                get() = "Should not be called"
        })
}

fun <T : Any> Parser<T>.parse(context: ParsingContext, value: Any?, path: String) =
    with(context) { parse(value, path) }

inline fun <reified E : Enum<E>> enumParser(): Parser<E> = object : Parser<E>() {
    override fun ParsingContext.parse(value: Any?, path: String): TaskResult<E> = computeM {
        val (enumName) = value.parseAs<String>(path)
        safe { enumValueOf<E>(enumName) }.mapFailure {
            listOf(
                ParseError(
                    "For setting `$path` one of [${enumValues<E>().joinToString { it.name }}] was expected but `$enumName` was found"
                )
            )
        }
    }
}

inline fun <reified R : Any> listParser(
    elementParser: Parser<R>
) = object : Parser<List<R>>() {
    override fun ParsingContext.parse(value: Any?, path: String): TaskResult<List<R>> = computeM {
        val (list) = value.parseAs<List<*>>(path)
        list.mapComputeM { elementParser.parse(this, it, path) }.sequence()
    }
}


fun <T : Any> mapParser(parseMap: suspend ParsingContext.(map: Map<String, *>, path: String) -> T) =
    object : Parser<T>() {
        override fun ParsingContext.parse(value: Any?, path: String): TaskResult<T> = when (value) {
            is String -> value.parseAs<String, T>(this, path) {
                parseMap(emptyMap<String, Any?>(), path).asSuccess()
            }
            else -> value.parseAs<Map<String, *>, T>(this, path) {
                parseMap(it, path).asSuccess()
            }
        }

    }

fun <T : Any> valueParser(parser: suspend ParsingContext.(value: Any?, path: String) -> T) =
    object : Parser<T>() {
        override fun ParsingContext.parse(value: Any?, path: String): TaskResult<T> = compute {
            parser(value, path)
        }
    }

fun <T : Any> valueParserM(parser: suspend ParsingContext.(value: Any?, path: String) -> TaskResult<T>) =
    object : Parser<T>() {
        override fun ParsingContext.parse(value: Any?, path: String): TaskResult<T> = computeM {
            parser(value, path)
        }
    }

inline fun <reified T : Any> valueParser() = valueParser { value, path ->
    value.parseAs(path, T::class).get()
}


class DisjunctionParser<T : Any>(
    private val keyToParser: Map<String, Parser<T>>
) : Parser<T>() {
    constructor(vararg keyToParser: Pair<String, Parser<T>>) : this(keyToParser.toMap())

    override fun ParsingContext.parse(value: Any?, path: String): TaskResult<T> = when (value) {
        is String -> // consider string value as an empty map
            mapOf(value to emptyMap<Any?, Any?>()).asSuccess()
        else -> value.parseAs<Map<*, *>>(path)
    }.flatMap { map ->
        computeM {
            val (singleItem) = map.entries.singleOrNull()
                ?.takeIf { it.key is String }
                .toResult { ParseError("Setting `$path` should contain a single-key value") }
            val (parser) = keyToParser[singleItem.key as String]
                .toResult { ParseError("`$path` should be one of [${keyToParser.keys.joinToString()}]") }
            parser.parse(this@parse, singleItem.value, path)
        }
    }
}

class CollectingParser<T : Any>(
    private val keyToParser: Map<String, Parser<T>>
) : Parser<List<T>>() {
    override fun ParsingContext.parse(value: Any?, path: String): TaskResult<List<T>> =
        value.parseAs<Map<*, *>, List<T>>(this, path) { map ->
            map.mapNotNull { (key, value) ->
                val parser = keyToParser[key] ?: return@mapNotNull null
                parser.parse(this, value, "$path.$key")
            }.sequence()
        }
}

fun Any?.classMismatchError(path: String, expected: KClass<*>): ParseError {
    val classpath = this?.let { it::class.simpleName } ?: "null"
    return ParseError("Expected ${expected.simpleName!!} for `$path` but $classpath was found")
}

inline fun <reified V : Any> Any?.parseAs(path: String) =
    safeAs<V>().toResult { classMismatchError(path, V::class) }


inline fun <reified T : Any> Any?.parseAs(path: String, klass: KClass<T>): TaskResult<T> =
    this?.takeIf { it::class.isSubclassOf(klass) }?.safeAs<T>()
        .toResult { classMismatchError(path, klass) }


inline fun <reified V : Any> Map<*, *>.parseValue(path: String, name: String) =
    get(name).parseAs<V>("$path.$name")

inline fun <reified V : Any> Map<*, *>.parseValue(path: String, name: String, defaultValue: (() -> V)) =
    get(name)?.parseAs<V>("$path.$name") ?: defaultValue().asSuccess()


inline fun <reified V : Any, R : Any> Map<*, *>.parseValue(
    context: ParsingContext,
    path: String,
    name: String,
    crossinline parser: suspend ParsingContext.(V) -> TaskResult<R>
) = with(context) {
    computeM {
        val (result) = get(path).parseAs<V>("$path.$name")
        parser(result)
    }
}

inline fun <reified T : Any> Map<*, *>.parseValue(
    path: String,
    name: String,
    klass: KClass<T>
) = get(path).parseAs("$path.$name", klass)


fun <R : Any> Map<*, *>.parseValue(
    context: ParsingContext,
    path: String,
    name: String,
    parser: Parser<R>,
    defaultValue: (() -> R)? = null
) = with(context) {
    computeM {
        when (val value = get(name)) {
            null -> defaultValue?.invoke()?.asSuccess() ?: parser.parse(this, value = null, path = "$path.$name")
            else -> parser.parse(this, value, "$path.$name")
        }
    }
}

inline fun <reified V : Any, R : Any> Any?.parseAs(
    context: ParsingContext,
    path: String,
    crossinline parser: suspend ParsingContext.(V) -> TaskResult<R>
) = with(context) {
    computeM {
        val (result) = this@parseAs.parseAs<V>(path)
        parser(result)
    }
}

val pathParser = valueParser { value, path ->
    value.parseAs<String>(path).map { Paths.get(it) }.get()
}

infix fun <V: Any> Parser<V>.or(alternative: Parser<V>): Parser<V>  = object : Parser<V>() {
    override fun ParsingContext.parse(value: Any?, path: String): TaskResult<V> =
        this@or.parse(this, value, path).recover { alternative.parse(this, value, path) }
}