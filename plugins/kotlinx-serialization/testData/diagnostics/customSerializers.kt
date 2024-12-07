// FIR_IDENTICAL
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/* Tests on Serializable(with) on classes */
object Param0Object: ToDoSerializer<Param0WithObject>()
@Serializable(Param0Object::class)
class Param0WithObject(val i: Int)

object Param1Object: ToDoSerializer<Param1WithObject<*>>()
@Serializable(Param1Object::class)
class Param1WithObject<T>(val i: T)


class Param0Serializer0(): ToDoSerializer<Param0WithCustom0>()
@Serializable(Param0Serializer0::class)
class Param0WithCustom0(val i: Int)

class Param0Serializer1(val serializer: KSerializer<*>): ToDoSerializer<Param0WithCustom1>()
<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(Param0Serializer1::class)<!>
class Param0WithCustom1(val i: Int)

class Param0Serializer1Err(val serializer: Any): ToDoSerializer<Param0WithCustom1Err>()
<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Param0Serializer1Err::class)<!>
class Param0WithCustom1Err(val i: Int)

class Param1Serializer0<T: Any>: ToDoSerializer<Param1WithCustom0<T>>()
@Serializable(Param1Serializer0::class)
class Param1WithCustom0<T>(val i: T)

class Param1Serializer1<T: Any>(val serializer: KSerializer<T>): ToDoSerializer<Param1WithCustom<T>>()
@Serializable(Param1Serializer1::class)
class Param1WithCustom<T>(val i: T)

class Param1Serializer1Err<T: Any>(val serializer: Any): ToDoSerializer<Param1WithCustom1Err<T>>()
<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Param1Serializer1Err::class)<!>
class Param1WithCustom1Err<T>(val i: T)

class Param1Serializer2<T: Any>(val serializer: KSerializer<T>, val serializer2: KSerializer<Any>): ToDoSerializer<Param1WithCustom2<T>>()
<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(Param1Serializer2::class)<!>
class Param1WithCustom2<T>(val i: T)

class Param1Serializer2Err<T: Any>(val serializer1: Any, val serializer2: Any): ToDoSerializer<Param1WithCustom2Err<T>>()
<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Param1Serializer2Err::class)<!>
class Param1WithCustom2Err<T>(val i: T)

class Param2Serializer1<T: Any>(val serializer: KSerializer<T>): ToDoSerializer<Param2WithCustom1<T, T>>()
<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(Param2Serializer1::class)<!>
class Param2WithCustom1<T, K>(val i: T, val k: K)

class Param2Serializer2Err<T: Any>(val serializer1: Any, val serializer2: Any): ToDoSerializer<Param2WithCustom2Err<T, T>>()
<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Param2Serializer2Err::class)<!>
class Param2WithCustom2Err<T, K>(val i: T, val k: K)

/* Tests on Serializable(with) on properties */

class Prop0(val t: Int)

class Prop0Serializer0(): ToDoSerializer<Prop0>()

class Prop0Serializer1(val serializer: KSerializer<*>): ToDoSerializer<Prop0>()

class Prop0Serializer1Err(val serializer: Any): ToDoSerializer<Prop0>()

object Prop0SerializerObject: ToDoSerializer<Prop0>()


class Prop1<T>(val t: T)

class Prop1Serializer0<T: Any>(): ToDoSerializer<Prop1<T>>()
class Prop1Serializer1<T: Any>(val serializer: KSerializer<T>): ToDoSerializer<Prop1<T>>()
class Prop1Serializer2<T: Any>(val serializer1: KSerializer<T>, val serializer2: KSerializer<T>): ToDoSerializer<Prop1<T>>()

class Prop1Serializer1Err<T: Any>(val serializer: Any): ToDoSerializer<Prop1<T>>()
class Prop1Serializer2Err<T: Any>(val serializer1: Any, val serializer2: Any): ToDoSerializer<Prop1<T>>()

object Prop1SerializerObject: ToDoSerializer<Prop1<*>>()


@Serializable
class Holder<T>(
    @Serializable(Prop0SerializerObject::class) val obj0: Prop0,

    @Serializable(Prop0Serializer0::class) val p0_0: Prop0,
    <!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(Prop0Serializer1::class)<!> val p0_1: Prop0,
    <!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop0Serializer1Err::class)<!> val p0_1Err: Prop0,

    @Serializable(Prop1SerializerObject::class) val obj1T: Prop1<T>,
    @Serializable(Prop1SerializerObject::class) val obj1: Prop1<Int>,

    @Serializable(Prop1Serializer0::class) val p1_0T: Prop1<T>,
    @Serializable(Prop1Serializer0::class) val p1_0: Prop1<Int>,
    @Serializable(Prop1Serializer1::class) val p1_1T: Prop1<T>,
    @Serializable(Prop1Serializer1::class) val p1_1: Prop1<Int>,
    <!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(Prop1Serializer2::class)<!> val p1_2T: Prop1<T>,
    <!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(Prop1Serializer2::class)<!> val p1_2: Prop1<Int>,

    <!CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop1Serializer1Err::class)<!> val p1_1TErr: Prop1<T>,
    <!CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop1Serializer1Err::class)<!> val p1_1Err: Prop1<Int>,
    <!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop1Serializer2Err::class)<!> val p1_2TErr: Prop1<T>,
    <!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop1Serializer2Err::class)<!> val p1_2Err: Prop1<Int>,

    // generic params
    val list_obj0: List<@Serializable(Prop0SerializerObject::class) Prop0>,

    val list_p0_0: List<@Serializable(Prop0Serializer0::class) Prop0>,
    val list_p0_1: List<<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(Prop0Serializer1::class)<!> Prop0>,
    val list_p0_1Err: List<<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop0Serializer1Err::class)<!> Prop0>,

    val list_obj1T: List<@Serializable(Prop1SerializerObject::class) Prop1<T>>,
    val list_obj1: List<@Serializable(Prop1SerializerObject::class) Prop1<Int>>,

    val list_p1_0T: List<@Serializable(Prop1Serializer0::class) Prop1<T>>,
    val list_p1_0: List<@Serializable(Prop1Serializer0::class) Prop1<Int>>,
    val list_p1_1T: List<@Serializable(Prop1Serializer1::class) Prop1<T>>,
    val list_p1_1: List<@Serializable(Prop1Serializer1::class) Prop1<Int>>,
    val list_p1_2T: List<<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(Prop1Serializer2::class)<!> Prop1<T>>,
    val list_p1_2: List<<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(Prop1Serializer2::class)<!> Prop1<Int>>,

    val list_p1_1TErr: List<<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop1Serializer1Err::class)<!> Prop1<T>>,
    val list_p1_1Err: List<<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop1Serializer1Err::class)<!> Prop1<Int>>,
    val list_p1_2TErr: List<<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop1Serializer2Err::class)<!> Prop1<T>>,
    val list_p1_2Err: List<<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE, CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE!>@Serializable(Prop1Serializer2Err::class)<!> Prop1<Int>>,
)



abstract class ToDoSerializer<T: Any>: KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TODO SERIALIZER", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): T = TODO()
    override fun serialize(encoder: Encoder, value: T) = TODO()
}
