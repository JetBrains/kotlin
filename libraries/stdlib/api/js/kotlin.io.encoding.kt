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

    public final fun withPadding(onEncode: kotlin.io.encoding.Base64.EncodePadding = ..., onDecode: kotlin.io.encoding.Base64.DecodePadding = ...): kotlin.io.encoding.Base64

    public final fun withPaddingOptions(encodeOption: kotlin.io.encoding.Base64.EncodePadding = ..., decodeOption: kotlin.io.encoding.Base64.DecodePadding = ...): kotlin.io.encoding.Base64

    public final enum class DecodePadding : kotlin.Enum<kotlin.io.encoding.Base64.DecodePadding> {
        enum entry REQUIRE_PRESENT

        enum entry REQUIRE_ABSENT

        enum entry ALLOW_BOTH
    }

    public companion object of Base64 Default : kotlin.io.encoding.Base64 {
        public final val Mime: kotlin.io.encoding.Base64 { get; }

        public final val UrlSafe: kotlin.io.encoding.Base64 { get; }
    }

    public final enum class EncodePadding : kotlin.Enum<kotlin.io.encoding.Base64.EncodePadding> {
        enum entry WRITE

        enum entry OMIT
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