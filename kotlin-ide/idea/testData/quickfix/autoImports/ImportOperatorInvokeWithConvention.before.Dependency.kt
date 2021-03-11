package another

interface SomeType

operator fun SomeType.invoke() {}
val topVal = object : SomeType {}