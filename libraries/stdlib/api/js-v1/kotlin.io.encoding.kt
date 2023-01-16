@kotlin.SinceKotlin(version = "1.8")
@kotlin.io.encoding.ExperimentalEncodingApi
public open class Base64 {
    public final fun decode(source: kotlin.ByteArray, startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.ByteArray

    public final fun decode(source: kotlin.CharSequence, startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.ByteArray

    public final fun decodeIntoByteArray(source: kotlin.ByteArray, destination: kotlin.ByteArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.Int

    public final fun decodeIntoByteArray(source: kotlin.CharSequence, destination: kotlin.ByteArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.Int

    public final fun encode(source: kotlin.ByteArray, startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.String

    public final fun encodeIntoByteArray(source: kotlin.ByteArray, destination: kotlin.ByteArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.Int

    public final fun <A : kotlin.text.Appendable> encodeToAppendable(source: kotlin.ByteArray, destination: A, startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): A

    public final fun encodeToByteArray(source: kotlin.ByteArray, startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.ByteArray

    public companion object of Base64 Default : kotlin.io.encoding.Base64 {
        public final val Mime: kotlin.io.encoding.Base64 { get; }

        public final val UrlSafe: kotlin.io.encoding.Base64 { get; }
    }
}

@kotlin.RequiresOptIn(level = Level.ERROR)
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.MustBeDocumented
@kotlin.SinceKotlin(version = "1.8")
public final annotation class ExperimentalEncodingApi : kotlin.Annotation {
    public constructor ExperimentalEncodingApi()
}