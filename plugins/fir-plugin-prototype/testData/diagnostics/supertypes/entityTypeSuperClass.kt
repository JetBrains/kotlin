package foo

import org.jetbrains.kotlin.fir.plugin.GeneratedEntityType

open class EntityType<Self>

@GeneratedEntityType
class WithImplicitAny

interface Inter

@GeneratedEntityType
class WithExplicitInterface : Inter

open class SomeClass

@GeneratedEntityType
class WithExplicitClass : SomeClass()
