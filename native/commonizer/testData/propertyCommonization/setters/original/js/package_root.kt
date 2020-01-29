var defaultSetter1 = 42
var defaultSetter2 = 42
    set
var defaultSetter3 = 42
    set

var setterWithoutBackingField1
    get() = 42
    set(value) = Unit
var setterWithoutBackingField2
    get() = 42
    set(value) = Unit

var setterWithDelegation1: Int by mutableMapOf("setterWithDelegation1" to 42)
var setterWithDelegation2: Int by mutableMapOf("setterWithDelegation2" to 42)

var defaultSetteCustomVisibility1 = 42
    public set
var defaultSetteCustomVisibility2 = 42
    internal set
var defaultSetteCustomVisibility3 = 42
    internal set
var defaultSetteCustomVisibility4 = 42
    private set
var defaultSetteCustomVisibility5 = 42
    private set

val propertyWithoutSetter = 42
var propertyMaybeSetter = 42
var propertyWithSetter = 42
