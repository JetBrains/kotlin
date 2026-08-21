// WITH_STDLIB

import kotlinx.serialization.*

// The plugin does not see @Serializable behind an alias, so `Some` is not serializable at all.
<!SERIALIZABLE_ANNOTATION_TYPEALIAS_UNSUPPORTED!>typealias MySerializable = kotlinx.serialization.Serializable<!>

// An alias of an alias is just as invisible to the plugin.
<!SERIALIZABLE_ANNOTATION_TYPEALIAS_UNSUPPORTED!>typealias MySerializableTwice = MySerializable<!>

@MySerializable
class Some

// The supported alternative.
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MyMetaSerializable

@MyMetaSerializable
class Meta

// Aliases of other serialization annotations are expanded by the backend and stay unreported.
typealias MySerialName = SerialName

typealias MyTransient = Transient

@Serializable
class Regular(@MySerialName("renamed") val a: Int, @MyTransient val b: Int = 0)
