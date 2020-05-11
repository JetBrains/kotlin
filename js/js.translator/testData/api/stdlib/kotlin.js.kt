package kotlin.js

public external val console: kotlin.js.Console
    public fun <get-console>(): kotlin.js.Console
public external val definedExternally: kotlin.Nothing
    public fun <get-definedExternally>(): kotlin.Nothing
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use `definedExternally` instead", replaceWith = kotlin.ReplaceWith(expression = "definedExternally", imports = {})) public external val noImpl: kotlin.Nothing
    public fun <get-noImpl>(): kotlin.Nothing
public external val undefined: kotlin.Nothing?
    public fun <get-undefined>(): kotlin.Nothing?
public val </*0*/ T : kotlin.Any> kotlin.reflect.KClass<T>.js: kotlin.js.JsClass<T>
    public fun kotlin.reflect.KClass<T>.<get-js>(): kotlin.js.JsClass<T>
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use class literal and extension property `js` instead.", replaceWith = kotlin.ReplaceWith(expression = "this::class.js", imports = {})) public val </*0*/ T : kotlin.Any> T.jsClass: kotlin.js.JsClass<T>
    public fun T.<get-jsClass>(): kotlin.js.JsClass<T>
public val </*0*/ T : kotlin.Any> kotlin.js.JsClass<T>.kotlin: kotlin.reflect.KClass<T>
    public fun kotlin.js.JsClass<T>.<get-kotlin>(): kotlin.reflect.KClass<T>
public inline fun dateLocaleOptions(/*0*/ init: kotlin.js.Date.LocaleOptions.() -> kotlin.Unit): kotlin.js.Date.LocaleOptions
public external fun eval(/*0*/ expr: kotlin.String): dynamic
public external fun js(/*0*/ code: kotlin.String): dynamic
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use class literal and extension property `js` instead.", replaceWith = kotlin.ReplaceWith(expression = "T::class.js", imports = {})) public external fun </*0*/ T : kotlin.Any> jsClass(): kotlin.js.JsClass<T>
@kotlin.internal.InlineOnly public inline fun jsTypeOf(/*0*/ a: kotlin.Any?): kotlin.String
public fun json(/*0*/ vararg pairs: kotlin.Pair<kotlin.String, kotlin.Any?> /*kotlin.Array<out kotlin.Pair<kotlin.String, kotlin.Any?>>*/): kotlin.js.Json
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use toDouble() instead.", replaceWith = kotlin.ReplaceWith(expression = "s.toDouble()", imports = {})) public external fun parseFloat(/*0*/ s: kotlin.String, /*1*/ radix: kotlin.Int = ...): kotlin.Double
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use toInt() instead.", replaceWith = kotlin.ReplaceWith(expression = "s.toInt()", imports = {})) public external fun parseInt(/*0*/ s: kotlin.String): kotlin.Int
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use toInt(radix) instead.", replaceWith = kotlin.ReplaceWith(expression = "s.toInt(radix)", imports = {})) public external fun parseInt(/*0*/ s: kotlin.String, /*1*/ radix: kotlin.Int = ...): kotlin.Int
public fun kotlin.js.Json.add(/*0*/ other: kotlin.js.Json): kotlin.js.Json
public inline fun kotlin.js.RegExpMatch.asArray(): kotlin.Array<out kotlin.String?>
@kotlin.internal.InlineOnly public inline fun kotlin.Any?.asDynamic(): dynamic
public inline operator fun kotlin.js.RegExpMatch.get(/*0*/ index: kotlin.Int): kotlin.String?
@kotlin.internal.DynamicExtension public operator fun dynamic.iterator(): kotlin.collections.Iterator<dynamic>
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use maxOf or kotlin.math.max instead", replaceWith = kotlin.ReplaceWith(expression = "maxOf(a, b)", imports = {})) public fun kotlin.js.Math.max(/*0*/ a: kotlin.Long, /*1*/ b: kotlin.Long): kotlin.Long
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use minOf or kotlin.math.min instead", replaceWith = kotlin.ReplaceWith(expression = "minOf(a, b)", imports = {})) public fun kotlin.js.Math.min(/*0*/ a: kotlin.Long, /*1*/ b: kotlin.Long): kotlin.Long
public fun kotlin.js.RegExp.reset(): kotlin.Unit
public inline fun </*0*/ T, /*1*/ S> kotlin.js.Promise<kotlin.js.Promise<T>>.then(/*0*/ noinline onFulfilled: ((T) -> S)?): kotlin.js.Promise<S>
public inline fun </*0*/ T, /*1*/ S> kotlin.js.Promise<kotlin.js.Promise<T>>.then(/*0*/ noinline onFulfilled: ((T) -> S)?, /*1*/ noinline onRejected: ((kotlin.Throwable) -> S)?): kotlin.js.Promise<S>
@kotlin.internal.DynamicExtension @kotlin.js.JsName(name = "unsafeCastDynamic") @kotlin.internal.InlineOnly public inline fun </*0*/ T> dynamic.unsafeCast(): T
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Any?.unsafeCast(): T

public external interface Console {
    public abstract fun dir(/*0*/ o: kotlin.Any): kotlin.Unit
    public abstract fun error(/*0*/ vararg o: kotlin.Any? /*kotlin.Array<out kotlin.Any?>*/): kotlin.Unit
    public abstract fun info(/*0*/ vararg o: kotlin.Any? /*kotlin.Array<out kotlin.Any?>*/): kotlin.Unit
    public abstract fun log(/*0*/ vararg o: kotlin.Any? /*kotlin.Array<out kotlin.Any?>*/): kotlin.Unit
    public abstract fun warn(/*0*/ vararg o: kotlin.Any? /*kotlin.Array<out kotlin.Any?>*/): kotlin.Unit
}

public final external class Date {
    /*primary*/ public constructor Date()
    public constructor Date(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int)
    public constructor Date(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int)
    public constructor Date(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int, /*3*/ hour: kotlin.Int)
    public constructor Date(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int, /*3*/ hour: kotlin.Int, /*4*/ minute: kotlin.Int)
    public constructor Date(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int, /*3*/ hour: kotlin.Int, /*4*/ minute: kotlin.Int, /*5*/ second: kotlin.Int)
    public constructor Date(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int, /*3*/ hour: kotlin.Int, /*4*/ minute: kotlin.Int, /*5*/ second: kotlin.Int, /*6*/ millisecond: kotlin.Number)
    public constructor Date(/*0*/ milliseconds: kotlin.Number)
    public constructor Date(/*0*/ dateString: kotlin.String)
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
    public final fun toLocaleDateString(/*0*/ locales: kotlin.Array<kotlin.String> = ..., /*1*/ options: kotlin.js.Date.LocaleOptions = ...): kotlin.String
    public final fun toLocaleDateString(/*0*/ locales: kotlin.String, /*1*/ options: kotlin.js.Date.LocaleOptions = ...): kotlin.String
    public final fun toLocaleString(/*0*/ locales: kotlin.Array<kotlin.String> = ..., /*1*/ options: kotlin.js.Date.LocaleOptions = ...): kotlin.String
    public final fun toLocaleString(/*0*/ locales: kotlin.String, /*1*/ options: kotlin.js.Date.LocaleOptions = ...): kotlin.String
    public final fun toLocaleTimeString(/*0*/ locales: kotlin.Array<kotlin.String> = ..., /*1*/ options: kotlin.js.Date.LocaleOptions = ...): kotlin.String
    public final fun toLocaleTimeString(/*0*/ locales: kotlin.String, /*1*/ options: kotlin.js.Date.LocaleOptions = ...): kotlin.String
    public final fun toTimeString(): kotlin.String
    public final fun toUTCString(): kotlin.String

    public companion object Companion {
        public final fun UTC(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int): kotlin.Double
        public final fun UTC(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int): kotlin.Double
        public final fun UTC(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int, /*3*/ hour: kotlin.Int): kotlin.Double
        public final fun UTC(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int, /*3*/ hour: kotlin.Int, /*4*/ minute: kotlin.Int): kotlin.Double
        public final fun UTC(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int, /*3*/ hour: kotlin.Int, /*4*/ minute: kotlin.Int, /*5*/ second: kotlin.Int): kotlin.Double
        public final fun UTC(/*0*/ year: kotlin.Int, /*1*/ month: kotlin.Int, /*2*/ day: kotlin.Int, /*3*/ hour: kotlin.Int, /*4*/ minute: kotlin.Int, /*5*/ second: kotlin.Int, /*6*/ millisecond: kotlin.Number): kotlin.Double
        public final fun now(): kotlin.Double
        public final fun parse(/*0*/ dateString: kotlin.String): kotlin.Double
    }

    public interface LocaleOptions {
        public abstract var day: kotlin.String?
            public abstract fun <get-day>(): kotlin.String?
            public abstract fun <set-day>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var era: kotlin.String?
            public abstract fun <get-era>(): kotlin.String?
            public abstract fun <set-era>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var formatMatcher: kotlin.String?
            public abstract fun <get-formatMatcher>(): kotlin.String?
            public abstract fun <set-formatMatcher>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var hour: kotlin.String?
            public abstract fun <get-hour>(): kotlin.String?
            public abstract fun <set-hour>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var hour12: kotlin.Boolean?
            public abstract fun <get-hour12>(): kotlin.Boolean?
            public abstract fun <set-hour12>(/*0*/ <set-?>: kotlin.Boolean?): kotlin.Unit
        public abstract var localeMatcher: kotlin.String?
            public abstract fun <get-localeMatcher>(): kotlin.String?
            public abstract fun <set-localeMatcher>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var minute: kotlin.String?
            public abstract fun <get-minute>(): kotlin.String?
            public abstract fun <set-minute>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var month: kotlin.String?
            public abstract fun <get-month>(): kotlin.String?
            public abstract fun <set-month>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var second: kotlin.String?
            public abstract fun <get-second>(): kotlin.String?
            public abstract fun <set-second>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var timeZone: kotlin.String?
            public abstract fun <get-timeZone>(): kotlin.String?
            public abstract fun <set-timeZone>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var timeZoneName: kotlin.String?
            public abstract fun <get-timeZoneName>(): kotlin.String?
            public abstract fun <set-timeZoneName>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var weekday: kotlin.String?
            public abstract fun <get-weekday>(): kotlin.String?
            public abstract fun <set-weekday>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
        public abstract var year: kotlin.String?
            public abstract fun <get-year>(): kotlin.String?
            public abstract fun <set-year>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
    }
}

@kotlin.Experimental(level = Level.WARNING) @kotlin.RequiresOptIn(level = Level.WARNING) @kotlin.annotation.MustBeDocumented @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.SinceKotlin(version = "1.4") public final annotation class ExperimentalJsExport : kotlin.Annotation {
    /*primary*/ public constructor ExperimentalJsExport()
}

public external object JSON {
    public final fun </*0*/ T> parse(/*0*/ text: kotlin.String): T
    public final fun </*0*/ T> parse(/*0*/ text: kotlin.String, /*1*/ reviver: (key: kotlin.String, value: kotlin.Any?) -> kotlin.Any?): T
    public final fun stringify(/*0*/ o: kotlin.Any?): kotlin.String
    public final fun stringify(/*0*/ o: kotlin.Any?, /*1*/ replacer: ((key: kotlin.String, value: kotlin.Any?) -> kotlin.Any?)? = ..., /*2*/ space: kotlin.Int): kotlin.String
    public final fun stringify(/*0*/ o: kotlin.Any?, /*1*/ replacer: ((key: kotlin.String, value: kotlin.Any?) -> kotlin.Any?)? = ..., /*2*/ space: kotlin.String): kotlin.String
    public final fun stringify(/*0*/ o: kotlin.Any?, /*1*/ replacer: (key: kotlin.String, value: kotlin.Any?) -> kotlin.Any?): kotlin.String
    public final fun stringify(/*0*/ o: kotlin.Any?, /*1*/ replacer: kotlin.Array<kotlin.String>): kotlin.String
    public final fun stringify(/*0*/ o: kotlin.Any?, /*1*/ replacer: kotlin.Array<kotlin.String>, /*2*/ space: kotlin.Int): kotlin.String
    public final fun stringify(/*0*/ o: kotlin.Any?, /*1*/ replacer: kotlin.Array<kotlin.String>, /*2*/ space: kotlin.String): kotlin.String
}

public external interface JsClass</*0*/ T : kotlin.Any> {
    public abstract val name: kotlin.String
        public abstract fun <get-name>(): kotlin.String
}

@kotlin.js.ExperimentalJsExport @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE}) @kotlin.SinceKotlin(version = "1.3") public final annotation class JsExport : kotlin.Annotation {
    /*primary*/ public constructor JsExport()
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE}) public final annotation class JsModule : kotlin.Annotation {
    /*primary*/ public constructor JsModule(/*0*/ import: kotlin.String)
    public final val import: kotlin.String
        public final fun <get-import>(): kotlin.String
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER}) public final annotation class JsName : kotlin.Annotation {
    /*primary*/ public constructor JsName(/*0*/ name: kotlin.String)
    public final val name: kotlin.String
        public final fun <get-name>(): kotlin.String
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE}) public final annotation class JsNonModule : kotlin.Annotation {
    /*primary*/ public constructor JsNonModule()
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FILE}) public final annotation class JsQualifier : kotlin.Annotation {
    /*primary*/ public constructor JsQualifier(/*0*/ value: kotlin.String)
    public final val value: kotlin.String
        public final fun <get-value>(): kotlin.String
}

public external interface Json {
    public abstract operator fun get(/*0*/ propertyName: kotlin.String): kotlin.Any?
    public abstract operator fun set(/*0*/ propertyName: kotlin.String, /*1*/ value: kotlin.Any?): kotlin.Unit
}

@kotlin.Deprecated(level = DeprecationLevel.WARNING, message = "Use top-level functions from kotlin.math package instead.") public external object Math {
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.PI instead.", replaceWith = kotlin.ReplaceWith(expression = "PI", imports = {"kotlin.math.PI"})) public final val PI: kotlin.Double
        public final fun <get-PI>(): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.abs instead.", replaceWith = kotlin.ReplaceWith(expression = "abs(value)", imports = {"kotlin.math.abs"})) public final fun abs(/*0*/ value: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.acos instead.", replaceWith = kotlin.ReplaceWith(expression = "acos(value)", imports = {"kotlin.math.acos"})) public final fun acos(/*0*/ value: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.asin instead.", replaceWith = kotlin.ReplaceWith(expression = "asin(value)", imports = {"kotlin.math.asin"})) public final fun asin(/*0*/ value: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.atan instead.", replaceWith = kotlin.ReplaceWith(expression = "atan(value)", imports = {"kotlin.math.atan"})) public final fun atan(/*0*/ value: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.atan2 instead.", replaceWith = kotlin.ReplaceWith(expression = "atan2(y, x)", imports = {"kotlin.math.atan2"})) public final fun atan2(/*0*/ y: kotlin.Double, /*1*/ x: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.ceil instead.", replaceWith = kotlin.ReplaceWith(expression = "ceil(value)", imports = {"kotlin.math.ceil"})) public final fun ceil(/*0*/ value: kotlin.Number): kotlin.Int
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.cos instead.", replaceWith = kotlin.ReplaceWith(expression = "cos(value)", imports = {"kotlin.math.cos"})) public final fun cos(/*0*/ value: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.exp instead.", replaceWith = kotlin.ReplaceWith(expression = "exp(value)", imports = {"kotlin.math.exp"})) public final fun exp(/*0*/ value: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.floor instead.", replaceWith = kotlin.ReplaceWith(expression = "floor(value)", imports = {"kotlin.math.floor"})) public final fun floor(/*0*/ value: kotlin.Number): kotlin.Int
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.ln instead.", replaceWith = kotlin.ReplaceWith(expression = "ln(value)", imports = {"kotlin.math.ln"})) public final fun log(/*0*/ value: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use maxOf or kotlin.math.max instead") public final fun max(/*0*/ vararg values: kotlin.Double /*kotlin.DoubleArray*/): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use maxOf or kotlin.math.max instead") public final fun max(/*0*/ vararg values: kotlin.Float /*kotlin.FloatArray*/): kotlin.Float
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use maxOf or kotlin.math.max instead") public final fun max(/*0*/ vararg values: kotlin.Int /*kotlin.IntArray*/): kotlin.Int
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use minOf or kotlin.math.min instead") public final fun min(/*0*/ vararg values: kotlin.Double /*kotlin.DoubleArray*/): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use minOf or kotlin.math.min instead") public final fun min(/*0*/ vararg values: kotlin.Float /*kotlin.FloatArray*/): kotlin.Float
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use minOf or kotlin.math.min instead") public final fun min(/*0*/ vararg values: kotlin.Int /*kotlin.IntArray*/): kotlin.Int
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.pow instead.", replaceWith = kotlin.ReplaceWith(expression = "base.pow(exp)", imports = {"kotlin.math.pow"})) public final fun pow(/*0*/ base: kotlin.Double, /*1*/ exp: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.WARNING, message = "Use Random.nextDouble instead", replaceWith = kotlin.ReplaceWith(expression = "kotlin.random.Random.nextDouble()", imports = {"kotlin.random.Random"})) public final fun random(): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.round instead.", replaceWith = kotlin.ReplaceWith(expression = "round(value)", imports = {"kotlin.math.round"})) public final fun round(/*0*/ value: kotlin.Number): kotlin.Int
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.sin instead.", replaceWith = kotlin.ReplaceWith(expression = "sin(value)", imports = {"kotlin.math.sin"})) public final fun sin(/*0*/ value: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.sqrt instead.", replaceWith = kotlin.ReplaceWith(expression = "sqrt(value)", imports = {"kotlin.math.sqrt"})) public final fun sqrt(/*0*/ value: kotlin.Double): kotlin.Double
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use kotlin.math.tan instead.", replaceWith = kotlin.ReplaceWith(expression = "tan(value)", imports = {"kotlin.math.tan"})) public final fun tan(/*0*/ value: kotlin.Double): kotlin.Double
}

public open external class Promise</*0*/ out T> {
    /*primary*/ public constructor Promise</*0*/ out T>(/*0*/ executor: (resolve: (T) -> kotlin.Unit, reject: (kotlin.Throwable) -> kotlin.Unit) -> kotlin.Unit)
    public open fun </*0*/ S> catch(/*0*/ onRejected: (kotlin.Throwable) -> S): kotlin.js.Promise<S>
    @kotlin.internal.LowPriorityInOverloadResolution public open fun </*0*/ S> then(/*0*/ onFulfilled: ((T) -> S)?): kotlin.js.Promise<S>
    @kotlin.internal.LowPriorityInOverloadResolution public open fun </*0*/ S> then(/*0*/ onFulfilled: ((T) -> S)?, /*1*/ onRejected: ((kotlin.Throwable) -> S)?): kotlin.js.Promise<S>

    public companion object Companion {
        public final fun </*0*/ S> all(/*0*/ promise: kotlin.Array<out kotlin.js.Promise<S>>): kotlin.js.Promise<kotlin.Array<out S>>
        public final fun </*0*/ S> race(/*0*/ promise: kotlin.Array<out kotlin.js.Promise<S>>): kotlin.js.Promise<S>
        public final fun reject(/*0*/ e: kotlin.Throwable): kotlin.js.Promise<kotlin.Nothing>
        public final fun </*0*/ S> resolve(/*0*/ e: S): kotlin.js.Promise<S>
        public final fun </*0*/ S> resolve(/*0*/ e: kotlin.js.Promise<S>): kotlin.js.Promise<S>
    }
}

public final external class RegExp {
    /*primary*/ public constructor RegExp(/*0*/ pattern: kotlin.String, /*1*/ flags: kotlin.String? = ...)
    public final val global: kotlin.Boolean
        public final fun <get-global>(): kotlin.Boolean
    public final val ignoreCase: kotlin.Boolean
        public final fun <get-ignoreCase>(): kotlin.Boolean
    public final var lastIndex: kotlin.Int
        public final fun <get-lastIndex>(): kotlin.Int
        public final fun <set-lastIndex>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public final val multiline: kotlin.Boolean
        public final fun <get-multiline>(): kotlin.Boolean
    public final fun exec(/*0*/ str: kotlin.String): kotlin.js.RegExpMatch?
    public final fun test(/*0*/ str: kotlin.String): kotlin.Boolean
    public open override /*1*/ fun toString(): kotlin.String
}

public external interface RegExpMatch {
    public abstract val index: kotlin.Int
        public abstract fun <get-index>(): kotlin.Int
    public abstract val input: kotlin.String
        public abstract fun <get-input>(): kotlin.String
    public abstract val length: kotlin.Int
        public abstract fun <get-length>(): kotlin.Int
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER}) @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use `external` modifier instead") public final annotation class native : kotlin.Annotation {
    /*primary*/ public constructor native(/*0*/ name: kotlin.String = ...)
    public final val name: kotlin.String
        public final fun <get-name>(): kotlin.String
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION}) @kotlin.Deprecated(message = "Use inline extension function with body using dynamic") public final annotation class nativeGetter : kotlin.Annotation {
    /*primary*/ public constructor nativeGetter()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION}) @kotlin.Deprecated(message = "Use inline extension function with body using dynamic") public final annotation class nativeInvoke : kotlin.Annotation {
    /*primary*/ public constructor nativeInvoke()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION}) @kotlin.Deprecated(message = "Use inline extension function with body using dynamic") public final annotation class nativeSetter : kotlin.Annotation {
    /*primary*/ public constructor nativeSetter()
}