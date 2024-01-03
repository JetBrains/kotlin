// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlin.reflect.KClass
import kotlin.test.*

// TODO: for this test to work, runtime dependency should be updated to (yet unreleased) serialization with @MetaSerializable annotation

@MetaSerializable
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class MySerializable

@MetaSerializable
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class MySerializableWithInfo(
    val value: Int,
    val kclass: KClass<*>
)

@MySerializable
class Project1(val name: String, val language: String)

@MySerializableWithInfo(123, String::class)
class Project2(val name: String, val language: String)

@Serializable
class Wrapper(
    @MySerializableWithInfo(234, Int::class) val project: Project2
)

@Serializable
@MySerializableWithInfo(123, String::class)
class Project3(val name: String, val language: String)

@Serializable(with = MySerializer::class)
@MySerializableWithInfo(123, String::class)
class Project4(val name: String, val language: String)

@MySerializableWithInfo(123, String::class)
sealed class TestSealed {
    @MySerializableWithInfo(123, String::class)
    class A(val value1: String) : TestSealed()
    @MySerializableWithInfo(123, String::class)
    class B(val value2: String) : TestSealed()
}

@MySerializable
abstract class TestAbstract {
    @MySerializableWithInfo(123, String::class)
    class A(val value1: String) : TestSealed()

    @MySerializableWithInfo(123, String::class)
    class B(val value2: String) : TestSealed()
}

@MySerializableWithInfo(123, String::class)
enum class TestEnum { Value1, Value2 }

@MySerializableWithInfo(123, String::class)
object TestObject

object MySerializer : KSerializer<Project4> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Project4", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Project4) = encoder.encodeString("${value.name}:${value.language}")

    override fun deserialize(decoder: Decoder): Project4 {
        val params = decoder.decodeString().split(':')
        return Project4(params[0], params[1])
    }
}

fun testMetaSerializable() {
    val string = Json.encodeToString(Project1.serializer(), Project1("name", "lang"))
    assertEquals("""{"name":"name","language":"lang"}""", string)

    val reconstructed = Json.decodeFromString(Project1.serializer(), string)
    assertEquals("name", reconstructed.name)
    assertEquals("lang", reconstructed.language)
}

fun testMetaSerializableWithInfo() {
    val string = Json.encodeToString(Project2.serializer(), Project2("name", "lang"))
    assertEquals("""{"name":"name","language":"lang"}""", string)

    val reconstructed = Json.decodeFromString(Project2.serializer(), string)
    assertEquals("name", reconstructed.name)
    assertEquals("lang", reconstructed.language)

    val info = Project2.serializer().descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
    assertEquals(123, info.value)
    assertEquals(String::class, info.kclass)
}

fun testMetaSerializableOnProperty() {
    val info = Wrapper.serializer().descriptor.getElementAnnotations(0).filterIsInstance<MySerializableWithInfo>().first()
    assertEquals(234, info.value)
    assertEquals(Int::class, info.kclass)
}

fun testSerializableAndMetaAnnotation() {
    val string = Json.encodeToString(Project3.serializer(), Project3("name", "lang"))
    assertEquals("""{"name":"name","language":"lang"}""", string)

    val reconstructed = Json.decodeFromString(Project3.serializer(), string)
    assertEquals("name", reconstructed.name)
    assertEquals("lang", reconstructed.language)

    val info = Project3.serializer().descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
    assertEquals(123, info.value)
    assertEquals(String::class, info.kclass)
}

fun testCustomSerializerAndMetaAnnotation() {
    val string = Json.encodeToString(Project4.serializer(), Project4("name", "lang"))
    assertEquals("""name:lang""", string)

    val reconstructed = Json.decodeFromString(Project4.serializer(), string)
    assertEquals("name", reconstructed.name)
    assertEquals("lang", reconstructed.language)
}

fun testSealed() {
    val serializerA = TestSealed.A.serializer()
    val serializerB = TestSealed.B.serializer()
    assertNotNull(serializerA)
    assertNotNull(serializerB)

    val infoA = serializerA.descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
    val infoB = serializerB.descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
    assertEquals(123, infoA.value)
    assertEquals(String::class, infoA.kclass)
    assertEquals(123, infoB.value)
    assertEquals(String::class, infoB.kclass)
}

fun testAbstract() {
    val serializerA = TestAbstract.A.serializer()
    val serializerB = TestAbstract.B.serializer()
    assertNotNull(serializerA)
    assertNotNull(serializerB)

    val infoA = serializerA.descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
    val infoB = serializerB.descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
    assertEquals(123, infoA.value)
    assertEquals(String::class, infoA.kclass)
    assertEquals(123, infoB.value)
    assertEquals(String::class, infoB.kclass)
}

fun testEnum() {
    val serializer = TestEnum.serializer()
    assertNotNull(serializer)

    val info = serializer.descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
    assertEquals(123, info.value)
    assertEquals(String::class, info.kclass)
}

fun testObject() {
    val serializer = TestObject.serializer()
    assertNotNull(serializer)

    val info = serializer.descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
    assertEquals(123, info.value)
    assertEquals(String::class, info.kclass)
}

fun box(): String {
    testMetaSerializable()
    testMetaSerializableWithInfo()
    testMetaSerializableOnProperty()
    testSealed()
    testAbstract()
    testEnum()
    testObject()
    return "OK"
}
