package foo

import org.jetbrains.kotlin.plugin.sandbox.GeneratedEntityType

open class EntityType<Self>

@GeneratedEntityType
class WithImplicitAny

interface Inter

@GeneratedEntityType
class WithExplicitInterface : Inter

open class SomeClass

<!MANY_CLASSES_IN_SUPERTYPE_LIST!>@GeneratedEntityType
class WithExplicitClass : SomeClass()<!>
