// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlin.reflect.KClass
import kotlin.test.assertEquals

// TODO: for this test to work, runtime dependency should be updated to (yet unreleased) serialization with @MetaSerializable annotation

//@MetaSerializable
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class MySerializable

//@MetaSerializable
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
//    @MySerializableWithInfo(234, Int::class) val project: Project2
)

@Serializable(with = MySerializer::class)
@MySerializableWithInfo(123, String::class)
class Project3(val name: String, val language: String)

object MySerializer : KSerializer<Project3> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Project3", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Project3) = encoder.encodeString("${value.name}:${value.language}")

    override fun deserialize(decoder: Decoder): Project3 {
        val params = decoder.decodeString().split(':')
        return Project3(params[0], params[1])
    }
}

fun testMetaSerializable() {
//    val string = Json.encodeToString(Project1.serializer(), Project1("name", "lang"))
//    assertEquals("""{"name":"name","language":"lang"}""", string)
//
//    val reconstructed = Json.decodeFromString(Project1.serializer(), string)
//    assertEquals("name", reconstructed.name)
//    assertEquals("lang", reconstructed.language)
}

fun testMetaSerializableWithInfo() {
//    val string = Json.encodeToString(Project2.serializer(), Project2("name", "lang"))
//    assertEquals("""{"name":"name","language":"lang"}""", string)
//
//    val reconstructed = Json.decodeFromString(Project2.serializer(), string)
//    assertEquals("name", reconstructed.name)
//    assertEquals("lang", reconstructed.language)
//
//    val info = Project2.serializer().descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
//    assertEquals(123, info.value)
//    assertEquals(String::class, info.kclass)
}

fun testMetaSerializableOnProperty() {
//    val info = Wrapper.serializer().descriptor.getElementAnnotations(0).filterIsInstance<MySerializableWithInfo>().first()
//    assertEquals(234, info.value)
//    assertEquals(Int::class, info.kclass)
}

fun testCustomSerializerAndMetaAnnotation() {
//    val string = Json.encodeToString(Project3.serializer(), Project3("name", "lang"))
//    assertEquals("""name:lang""", string)
//
//    val reconstructed = Json.decodeFromString(Project3.serializer(), string)
//    assertEquals("name", reconstructed.name)
//    assertEquals("lang", reconstructed.language)
//
//    val info = Project3.serializer().descriptor.annotations.filterIsInstance<MySerializableWithInfo>().first()
//    assertEquals(123, info.value)
//    assertEquals(String::class, info.kclass)
}

fun box(): String {
    testMetaSerializable()
    testMetaSerializableWithInfo()
    testMetaSerializableOnProperty()
    return "OK"
}
