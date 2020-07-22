package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import java.nio.file.Paths
import kotlin.reflect.KClass

data class ParsingState(
    val idToTemplate: Map<String, Template>,
    val settingValues: Map<SettingReference<*, *>, Any>
) : ComputeContextState {
    companion object {
        val EMPTY = ParsingState(emptyMap(), emptyMap())
    }
}

fun ParsingState.withSettings(newSettings: List<Pair<SettingReference<*, *>, Any>>) =
    copy(settingValues = settingValues + newSettings)

typealias ParsingContext = ComputeContext<ParsingState>

abstract class Parser<out T : Any> {
    abstract fun ParsingContext.parse(value: Any?, @NonNls path: String): TaskResult<T>
}

fun <T : Any> alwaysFailingParser(errorMessage: String) = object : Parser<T>() {
    override fun ParsingContext.parse(value: Any?, path: String): TaskResult<T> =
        Failure(object : Error() {
            override val message: String get() = errorMessage
        })
}


fun <T : Any> Parser<T>.parse(context: ParsingContext, value: Any?, @NonNls path: String) =
    with(context) { parse(value, path) }

inline fun <reified E> enumParser(): Parser<E> where E : Enum<E>, E : DisplayableSettingItem = object : Parser<E>() {
    override fun ParsingContext.parse(value: Any?, path: String): TaskResult<E> = computeM {
        val (name) = value.parseAs<String>(path)
        enumValues<E>().firstOrNull { enumValue ->
            enumValue.name.equals(name, ignoreCase = true) || enumValue.text.equals(name, ignoreCase = true)
        }.toResult {
            ParseError(
                "For setting `$path` one of [${enumValues<E>().joinToString { it.name }}] was expected but `$name` was found"
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

fun Any?.classMismatchError(@NonNls path: String, expected: KClass<*>): ParseError {
    val classpath = this?.let { it::class.simpleName } ?: "null"
    return ParseError("Expected ${expected.simpleName!!} for `$path` but $classpath was found")
}

inline fun <reified V : Any> Any?.parseAs(@NonNls path: String) =
    safeAs<V>().toResult { classMismatchError(path, V::class) }


inline fun <reified V : Any> Map<*, *>.parseValue(@NonNls path: String, @NonNls name: String) =
    get(name).parseAs<V>("$path.$name")

inline fun <reified V : Any> Map<*, *>.parseValue(@NonNls path: String, @NonNls name: String, defaultValue: (() -> V)) =
    get(name)?.parseAs<V>("$path.$name") ?: defaultValue().asSuccess()


inline fun <reified V : Any, R : Any> Map<*, *>.parseValue(
    context: ParsingContext,
    @NonNls path: String,
    @NonNls name: String,
    crossinline parser: suspend ParsingContext.(V) -> TaskResult<R>
) = with(context) {
    computeM {
        val (result) = get(path).parseAs<V>("$path.$name")
        parser(result)
    }
}

fun <R : Any> Map<*, *>.parseValue(
    context: ParsingContext,
    @NonNls path: String,
    @NonNls name: String,
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
    @NonNls path: String,
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

infix fun <V : Any> Parser<V>.or(alternative: Parser<V>): Parser<V> = object : Parser<V>() {
    override fun ParsingContext.parse(value: Any?, path: String): TaskResult<V> =
        this@or.parse(this, value, path).recover { alternative.parse(this, value, path) }
}