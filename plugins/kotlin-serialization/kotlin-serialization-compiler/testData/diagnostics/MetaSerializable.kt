// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE,-EXPERIMENTAL_API_USAGE
// WITH_RUNTIME
// SKIP_TXT
// FILE: test.kt
import kotlinx.serialization.*
import kotlin.reflect.KClass

// TODO: for this test to work, runtime dependency should be updated to (yet unreleased) serialization with @MetaSerializable annotation

// multiple serializer params

//<-!MULTIPLE_SERIALIZER_PARAMS!->@MetaSerializable<!->
@Target(AnnotationTarget.CLASS)
annotation class MySerializableMultipleParams(
//    @MetaSerializable.Serializer val param1: KClass<out KSerializer<*>>,
//    @MetaSerializable.Serializer val param2: KClass<out KSerializer<*>>,
)

// serializer param wrong type

//@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializableWrongParamTypeWithKClass(
//    <-!SERIALIZER_PARAM_WRONG_TYPE!->@MetaSerializable.Serializer val param: KClass<String><!->,
)

//@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializableWrongParamType(
//    <-!SERIALIZER_PARAM_WRONG_TYPE!->@MetaSerializable.Serializer val param: String<!->,
)

// serializer param default value

//@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializableDefaultValue(
//    <-!SERIALIZER_PARAM_DEFAULT_VALUE!->@MetaSerializable.Serializer val param1: KClass<out KSerializer<*>> = KSerializer::class<!->,
)

// multiple annotations

//@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializable

//@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializableAnother

//<-!SERIALIZABLE_AND_META_ANNOTATION!->@Serializable<!->
@MySerializable
data class MyData1(val data: String)

//<-!MULTIPLE_META_ANNOTATIONS!->@MySerializable<!->
@MySerializableAnother
data class MyData2(val data: String)

// wrong serializer type

//@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializableWithType(
//    @MetaSerializable.Serializer val with: KClass<out KSerializer<*>>,
)
class Bar
@Serializer(forClass = Bar::class)
object BarSerializer: KSerializer<Bar>

//<-!SERIALIZER_TYPE_INCOMPATIBLE!->@MySerializableWithType(with = BarSerializer::class)<!->
class Baz(val i: Int)