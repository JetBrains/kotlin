actual var defaultSetter1 = 42
actual var defaultSetter2 = 42
actual var defaultSetter3 = 42
    set

actual var setterWithoutBackingField1
    get() = 42
    set(value) = Unit
actual var setterWithoutBackingField2 = 42

actual var setterWithDelegation1: Int by mutableMapOf("setterWithDelegation1" to 42)
actual var setterWithDelegation2 = 42

actual var defaultSetteCustomVisibility1 = 42
    public set
var defaultSetteCustomVisibility2 = 42
    public set
actual var defaultSetteCustomVisibility3 = 42
    internal set
var defaultSetteCustomVisibility4 = 42
    internal set
var defaultSetteCustomVisibility5 = 42
    private set

actual val propertyWithoutSetter = 42
val propertyMaybeSetter = 42
actual var propertyWithSetter = 42
