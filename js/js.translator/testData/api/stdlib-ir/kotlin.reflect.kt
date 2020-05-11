package kotlin.reflect

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun </*0*/ reified T> typeOf(): kotlin.reflect.KType
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.LowPriorityInOverloadResolution public fun </*0*/ T : kotlin.Any> kotlin.reflect.KClass<T>.cast(/*0*/ value: kotlin.Any?): T
@kotlin.reflect.ExperimentalAssociatedObjects public inline fun </*0*/ reified T : kotlin.Annotation> kotlin.reflect.KClass<*>.findAssociatedObject(): kotlin.Any?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.LowPriorityInOverloadResolution public fun </*0*/ T : kotlin.Any> kotlin.reflect.KClass<T>.safeCast(/*0*/ value: kotlin.Any?): T?

@kotlin.reflect.ExperimentalAssociatedObjects @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) public final annotation class AssociatedObjectKey : kotlin.Annotation {
    /*primary*/ public constructor AssociatedObjectKey()
}

@kotlin.RequiresOptIn(level = Level.ERROR) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) public final annotation class ExperimentalAssociatedObjects : kotlin.Annotation {
    /*primary*/ public constructor ExperimentalAssociatedObjects()
}

public interface KCallable</*0*/ out R> {
    public abstract val name: kotlin.String
        public abstract fun <get-name>(): kotlin.String
}

public interface KClass</*0*/ T : kotlin.Any> : kotlin.reflect.KClassifier {
    public abstract val qualifiedName: kotlin.String?
        public abstract fun <get-qualifiedName>(): kotlin.String?
    public abstract val simpleName: kotlin.String?
        public abstract fun <get-simpleName>(): kotlin.String?
    public abstract override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public abstract override /*1*/ fun hashCode(): kotlin.Int
    @kotlin.SinceKotlin(version = "1.1") public abstract fun isInstance(/*0*/ value: kotlin.Any?): kotlin.Boolean
}

@kotlin.SinceKotlin(version = "1.1") public interface KClassifier {
}

public interface KFunction</*0*/ out R> : kotlin.reflect.KCallable<R>, kotlin.Function<R> {
}

public interface KMutableProperty</*0*/ V> : kotlin.reflect.KProperty<V> {
}

public interface KMutableProperty0</*0*/ V> : kotlin.reflect.KProperty0<V>, kotlin.reflect.KMutableProperty<V> {
    public abstract fun set(/*0*/ value: V): kotlin.Unit
}

public interface KMutableProperty1</*0*/ T, /*1*/ V> : kotlin.reflect.KProperty1<T, V>, kotlin.reflect.KMutableProperty<V> {
    public abstract fun set(/*0*/ receiver: T, /*1*/ value: V): kotlin.Unit
}

public interface KMutableProperty2</*0*/ D, /*1*/ E, /*2*/ V> : kotlin.reflect.KProperty2<D, E, V>, kotlin.reflect.KMutableProperty<V> {
    public abstract fun set(/*0*/ receiver1: D, /*1*/ receiver2: E, /*2*/ value: V): kotlin.Unit
}

public interface KProperty</*0*/ out V> : kotlin.reflect.KCallable<V> {
}

public interface KProperty0</*0*/ out V> : kotlin.reflect.KProperty<V>, () -> V {
    public abstract fun get(): V
}

public interface KProperty1</*0*/ T, /*1*/ out V> : kotlin.reflect.KProperty<V>, (T) -> V {
    public abstract fun get(/*0*/ receiver: T): V
}

public interface KProperty2</*0*/ D, /*1*/ E, /*2*/ out V> : kotlin.reflect.KProperty<V>, (D, E) -> V {
    public abstract fun get(/*0*/ receiver1: D, /*1*/ receiver2: E): V
}

public interface KType {
    @kotlin.SinceKotlin(version = "1.1") public abstract val arguments: kotlin.collections.List<kotlin.reflect.KTypeProjection>
        public abstract fun <get-arguments>(): kotlin.collections.List<kotlin.reflect.KTypeProjection>
    @kotlin.SinceKotlin(version = "1.1") public abstract val classifier: kotlin.reflect.KClassifier?
        public abstract fun <get-classifier>(): kotlin.reflect.KClassifier?
    public abstract val isMarkedNullable: kotlin.Boolean
        public abstract fun <get-isMarkedNullable>(): kotlin.Boolean
}

@kotlin.SinceKotlin(version = "1.1") public interface KTypeParameter : kotlin.reflect.KClassifier {
    public abstract val isReified: kotlin.Boolean
        public abstract fun <get-isReified>(): kotlin.Boolean
    public abstract val name: kotlin.String
        public abstract fun <get-name>(): kotlin.String
    public abstract val upperBounds: kotlin.collections.List<kotlin.reflect.KType>
        public abstract fun <get-upperBounds>(): kotlin.collections.List<kotlin.reflect.KType>
    public abstract val variance: kotlin.reflect.KVariance
        public abstract fun <get-variance>(): kotlin.reflect.KVariance
}

@kotlin.SinceKotlin(version = "1.1") public final data class KTypeProjection {
    /*primary*/ public constructor KTypeProjection(/*0*/ variance: kotlin.reflect.KVariance?, /*1*/ type: kotlin.reflect.KType?)
    public final val type: kotlin.reflect.KType?
        public final fun <get-type>(): kotlin.reflect.KType?
    public final val variance: kotlin.reflect.KVariance?
        public final fun <get-variance>(): kotlin.reflect.KVariance?
    public final operator /*synthesized*/ fun component1(): kotlin.reflect.KVariance?
    public final operator /*synthesized*/ fun component2(): kotlin.reflect.KType?
    public final /*synthesized*/ fun copy(/*0*/ variance: kotlin.reflect.KVariance? = ..., /*1*/ type: kotlin.reflect.KType? = ...): kotlin.reflect.KTypeProjection
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
        public final val STAR: kotlin.reflect.KTypeProjection
            public final fun <get-STAR>(): kotlin.reflect.KTypeProjection
        public final fun contravariant(/*0*/ type: kotlin.reflect.KType): kotlin.reflect.KTypeProjection
        public final fun covariant(/*0*/ type: kotlin.reflect.KType): kotlin.reflect.KTypeProjection
        public final fun invariant(/*0*/ type: kotlin.reflect.KType): kotlin.reflect.KTypeProjection
    }
}

@kotlin.SinceKotlin(version = "1.1") public final enum class KVariance : kotlin.Enum<kotlin.reflect.KVariance> {
    enum entry INVARIANT

    enum entry IN

    enum entry OUT

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.reflect.KVariance
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.reflect.KVariance>
}