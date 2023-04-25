public external val console: kotlin.js.Console { get; }

public external val definedExternally: kotlin.Nothing { get; }

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use `definedExternally` instead", replaceWith = kotlin.ReplaceWith(expression = "definedExternally", imports = {}))
public external val noImpl: kotlin.Nothing { get; }

public external val undefined: kotlin.Nothing? { get; }

public val <T : kotlin.Any> kotlin.reflect.KClass<T>.js: kotlin.js.JsClass<T> { get; }

public val <T : kotlin.Any> kotlin.js.JsClass<T>.kotlin: kotlin.reflect.KClass<T> { get; }

public inline fun dateLocaleOptions(init: kotlin.js.Date.LocaleOptions.() -> kotlin.Unit): kotlin.js.Date.LocaleOptions

public external fun eval(expr: kotlin.String): dynamic

public external fun js(code: kotlin.String): dynamic

/*∆*/ public external fun jsTypeOf(a: kotlin.Any?): kotlin.String

public fun json(vararg pairs: kotlin.Pair<kotlin.String, kotlin.Any?>): kotlin.js.Json

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use toDouble() instead.", replaceWith = kotlin.ReplaceWith(expression = "s.toDouble()", imports = {}))
public external fun parseFloat(s: kotlin.String, radix: kotlin.Int = ...): kotlin.Double

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use toInt() instead.", replaceWith = kotlin.ReplaceWith(expression = "s.toInt()", imports = {}))
public external fun parseInt(s: kotlin.String): kotlin.Int

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use toInt(radix) instead.", replaceWith = kotlin.ReplaceWith(expression = "s.toInt(radix)", imports = {}))
public external fun parseInt(s: kotlin.String, radix: kotlin.Int = ...): kotlin.Int

public fun kotlin.js.Json.add(other: kotlin.js.Json): kotlin.js.Json

public inline fun kotlin.js.RegExpMatch.asArray(): kotlin.Array<out kotlin.String?>

@kotlin.internal.InlineOnly
public inline fun kotlin.Any?.asDynamic(): dynamic

public inline operator fun kotlin.js.RegExpMatch.get(index: kotlin.Int): kotlin.String?

@kotlin.internal.DynamicExtension
public operator fun dynamic.iterator(): kotlin.collections.Iterator<dynamic>

public fun kotlin.js.RegExp.reset(): kotlin.Unit

public inline fun <T, S> kotlin.js.Promise<kotlin.js.Promise<T>>.then(noinline onFulfilled: ((T) -> S)?): kotlin.js.Promise<S>

public inline fun <T, S> kotlin.js.Promise<kotlin.js.Promise<T>>.then(noinline onFulfilled: ((T) -> S)?, noinline onRejected: ((kotlin.Throwable) -> S)?): kotlin.js.Promise<S>

@kotlin.internal.DynamicExtension
@kotlin.js.JsName(name = "unsafeCastDynamic")
@kotlin.internal.InlineOnly
public inline fun <T> dynamic.unsafeCast(): T

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Any?.unsafeCast(): T

public external interface Console {
    public abstract fun dir(o: kotlin.Any): kotlin.Unit

    public abstract fun error(vararg o: kotlin.Any?): kotlin.Unit

    public abstract fun info(vararg o: kotlin.Any?): kotlin.Unit

    public abstract fun log(vararg o: kotlin.Any?): kotlin.Unit

    public abstract fun warn(vararg o: kotlin.Any?): kotlin.Unit
}

public final external class Date {
    public constructor Date()

    public constructor Date(year: kotlin.Int, month: kotlin.Int)

    public constructor Date(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int)

    public constructor Date(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int, hour: kotlin.Int)

    public constructor Date(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int, hour: kotlin.Int, minute: kotlin.Int)

    public constructor Date(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int, hour: kotlin.Int, minute: kotlin.Int, second: kotlin.Int)

    public constructor Date(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int, hour: kotlin.Int, minute: kotlin.Int, second: kotlin.Int, millisecond: kotlin.Number)

    public constructor Date(milliseconds: kotlin.Number)

    public constructor Date(dateString: kotlin.String)

    public final fun getDate(): kotlin.Int

    public final fun getDay(): kotlin.Int

    public final fun getFullYear(): kotlin.Int

    public final fun getHours(): kotlin.Int

    public final fun getMilliseconds(): kotlin.Int

    public final fun getMinutes(): kotlin.Int

    public final fun getMonth(): kotlin.Int

    public final fun getSeconds(): kotlin.Int

    public final fun getTime(): kotlin.Double

    public final fun getTimezoneOffset(): kotlin.Int

    public final fun getUTCDate(): kotlin.Int

    public final fun getUTCDay(): kotlin.Int

    public final fun getUTCFullYear(): kotlin.Int

    public final fun getUTCHours(): kotlin.Int

    public final fun getUTCMilliseconds(): kotlin.Int

    public final fun getUTCMinutes(): kotlin.Int

    public final fun getUTCMonth(): kotlin.Int

    public final fun getUTCSeconds(): kotlin.Int

    public final fun toDateString(): kotlin.String

    public final fun toISOString(): kotlin.String

    public final fun toJSON(): kotlin.js.Json

    public final fun toLocaleDateString(locales: kotlin.Array<kotlin.String> = ..., options: kotlin.js.Date.LocaleOptions = ...): kotlin.String

    public final fun toLocaleDateString(locales: kotlin.String, options: kotlin.js.Date.LocaleOptions = ...): kotlin.String

    public final fun toLocaleString(locales: kotlin.Array<kotlin.String> = ..., options: kotlin.js.Date.LocaleOptions = ...): kotlin.String

    public final fun toLocaleString(locales: kotlin.String, options: kotlin.js.Date.LocaleOptions = ...): kotlin.String

    public final fun toLocaleTimeString(locales: kotlin.Array<kotlin.String> = ..., options: kotlin.js.Date.LocaleOptions = ...): kotlin.String

    public final fun toLocaleTimeString(locales: kotlin.String, options: kotlin.js.Date.LocaleOptions = ...): kotlin.String

    public final fun toTimeString(): kotlin.String

    public final fun toUTCString(): kotlin.String

    public companion object of Date {
        public final fun UTC(year: kotlin.Int, month: kotlin.Int): kotlin.Double

        public final fun UTC(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int): kotlin.Double

        public final fun UTC(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int, hour: kotlin.Int): kotlin.Double

        public final fun UTC(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int, hour: kotlin.Int, minute: kotlin.Int): kotlin.Double

        public final fun UTC(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int, hour: kotlin.Int, minute: kotlin.Int, second: kotlin.Int): kotlin.Double

        public final fun UTC(year: kotlin.Int, month: kotlin.Int, day: kotlin.Int, hour: kotlin.Int, minute: kotlin.Int, second: kotlin.Int, millisecond: kotlin.Number): kotlin.Double

        public final fun now(): kotlin.Double

        public final fun parse(dateString: kotlin.String): kotlin.Double
    }

    public interface LocaleOptions {
        public abstract var day: kotlin.String? { get; set; }

        public abstract var era: kotlin.String? { get; set; }

        public abstract var formatMatcher: kotlin.String? { get; set; }

        public abstract var hour: kotlin.String? { get; set; }

        public abstract var hour12: kotlin.Boolean? { get; set; }

        public abstract var localeMatcher: kotlin.String? { get; set; }

        public abstract var minute: kotlin.String? { get; set; }

        public abstract var month: kotlin.String? { get; set; }

        public abstract var second: kotlin.String? { get; set; }

        public abstract var timeZone: kotlin.String? { get; set; }

        public abstract var timeZoneName: kotlin.String? { get; set; }

        public abstract var weekday: kotlin.String? { get; set; }

        public abstract var year: kotlin.String? { get; set; }
    }
}

@kotlin.ExperimentalStdlibApi
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.PROPERTY})
@kotlin.SinceKotlin(version = "1.6")
@kotlin.Deprecated(message = "This annotation is a temporal migration assistance and may be removed in the future releases, please consider filing an issue about the case where it is needed")
public final annotation class EagerInitialization : kotlin.Annotation {
    public constructor EagerInitialization()
}

@kotlin.RequiresOptIn(level = Level.WARNING)
@kotlin.annotation.MustBeDocumented
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.SinceKotlin(version = "1.4")
public final annotation class ExperimentalJsExport : kotlin.Annotation {
    public constructor ExperimentalJsExport()
}

public external object JSON {
    public final fun <T> parse(text: kotlin.String): T

    public final fun <T> parse(text: kotlin.String, reviver: (key: kotlin.String, value: kotlin.Any?) -> kotlin.Any?): T

    public final fun stringify(o: kotlin.Any?): kotlin.String

    public final fun stringify(o: kotlin.Any?, replacer: ((key: kotlin.String, value: kotlin.Any?) -> kotlin.Any?)? = ..., space: kotlin.Int): kotlin.String

    public final fun stringify(o: kotlin.Any?, replacer: ((key: kotlin.String, value: kotlin.Any?) -> kotlin.Any?)? = ..., space: kotlin.String): kotlin.String

    public final fun stringify(o: kotlin.Any?, replacer: (key: kotlin.String, value: kotlin.Any?) -> kotlin.Any?): kotlin.String

    public final fun stringify(o: kotlin.Any?, replacer: kotlin.Array<kotlin.String>): kotlin.String

    public final fun stringify(o: kotlin.Any?, replacer: kotlin.Array<kotlin.String>, space: kotlin.Int): kotlin.String

    public final fun stringify(o: kotlin.Any?, replacer: kotlin.Array<kotlin.String>, space: kotlin.String): kotlin.String
}

public external interface JsClass<T : kotlin.Any> {
    public abstract val name: kotlin.String { get; }
}

@kotlin.js.ExperimentalJsExport
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE})
@kotlin.SinceKotlin(version = "1.3")
public final annotation class JsExport : kotlin.Annotation {
    public constructor JsExport()

    @kotlin.js.ExperimentalJsExport
    @kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
    @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR})
    @kotlin.SinceKotlin(version = "1.8")
    public final annotation class Ignore : kotlin.Annotation {
        public constructor Ignore()
    }
}

@kotlin.ExperimentalStdlibApi
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.VALUE_PARAMETER})
@kotlin.SinceKotlin(version = "1.9")
public final annotation class JsExternalArgument : kotlin.Annotation {
    public constructor JsExternalArgument()
}

@kotlin.ExperimentalStdlibApi
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS})
@kotlin.SinceKotlin(version = "1.9")
public final annotation class JsExternalInheritorsOnly : kotlin.Annotation {
    public constructor JsExternalInheritorsOnly()
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE})
public final annotation class JsModule : kotlin.Annotation {
    public constructor JsModule(import: kotlin.String)

    public final val import: kotlin.String { get; }
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER})
public final annotation class JsName : kotlin.Annotation {
    public constructor JsName(name: kotlin.String)

    public final val name: kotlin.String { get; }
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE})
public final annotation class JsNonModule : kotlin.Annotation {
    public constructor JsNonModule()
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FILE})
public final annotation class JsQualifier : kotlin.Annotation {
    public constructor JsQualifier(value: kotlin.String)

    public final val value: kotlin.String { get; }
}

public external interface Json {
    public abstract operator fun get(propertyName: kotlin.String): kotlin.Any?

    public abstract operator fun set(propertyName: kotlin.String, value: kotlin.Any?): kotlin.Unit
}

public open external class Promise<out T> {
    public constructor Promise<out T>(executor: (resolve: (T) -> kotlin.Unit, reject: (kotlin.Throwable) -> kotlin.Unit) -> kotlin.Unit)

    public open fun <S> catch(onRejected: (kotlin.Throwable) -> S): kotlin.js.Promise<S>

    public open fun finally(onFinally: () -> kotlin.Unit): kotlin.js.Promise<T>

    @kotlin.internal.LowPriorityInOverloadResolution
    public open fun <S> then(onFulfilled: ((T) -> S)?): kotlin.js.Promise<S>

    @kotlin.internal.LowPriorityInOverloadResolution
    public open fun <S> then(onFulfilled: ((T) -> S)?, onRejected: ((kotlin.Throwable) -> S)?): kotlin.js.Promise<S>

    public companion object of Promise {
        public final fun <S> all(promise: kotlin.Array<out kotlin.js.Promise<S>>): kotlin.js.Promise<kotlin.Array<out S>>

        public final fun <S> race(promise: kotlin.Array<out kotlin.js.Promise<S>>): kotlin.js.Promise<S>

        public final fun reject(e: kotlin.Throwable): kotlin.js.Promise<kotlin.Nothing>

        public final fun <S> resolve(e: S): kotlin.js.Promise<S>

        public final fun <S> resolve(e: kotlin.js.Promise<S>): kotlin.js.Promise<S>
    }
}

public final external class RegExp {
    public constructor RegExp(pattern: kotlin.String, flags: kotlin.String? = ...)

    public final val global: kotlin.Boolean { get; }

    public final val ignoreCase: kotlin.Boolean { get; }

    public final var lastIndex: kotlin.Int { get; set; }

    public final val multiline: kotlin.Boolean { get; }

    public final fun exec(str: kotlin.String): kotlin.js.RegExpMatch?

    public final fun test(str: kotlin.String): kotlin.Boolean

    public open override fun toString(): kotlin.String
}

public external interface RegExpMatch {
    public abstract val index: kotlin.Int { get; }

    public abstract val input: kotlin.String { get; }

    public abstract val length: kotlin.Int { get; }
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION})
@kotlin.Deprecated(message = "Use inline extension function with body using dynamic")
public final annotation class nativeGetter : kotlin.Annotation {
    public constructor nativeGetter()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION})
@kotlin.Deprecated(message = "Use inline extension function with body using dynamic")
public final annotation class nativeInvoke : kotlin.Annotation {
    public constructor nativeInvoke()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION})
@kotlin.Deprecated(message = "Use inline extension function with body using dynamic")
public final annotation class nativeSetter : kotlin.Annotation {
    public constructor nativeSetter()
}