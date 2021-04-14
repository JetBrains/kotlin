var defaultSetter1 = 42
var defaultSetter2 = 42 // intentionally commented setter declaration

//    set
var defaultSetter3 = 42
    set

var setterWithoutBackingField1
    get() = 42
    set(value) = Unit
var setterWithoutBackingField2 = 42 // intentionally left as with backing field

var setterWithDelegation1: Int by mutableMapOf("setterWithDelegation1" to 42)
var setterWithDelegation2 = 42 // intentionally left without delegation

var defaultSetteCustomVisibility1 = 42
    public set
var defaultSetteCustomVisibility2 = 42
    //    internal set
    public set // intentionally used public visibility
var defaultSetteCustomVisibility3 = 42
    internal set
var defaultSetteCustomVisibility4 = 42
    //    private set
    internal set // intentionally used internal visibility
var defaultSetteCustomVisibility5 = 42
    private set

val propertyWithoutSetter = 42

//var propertyMaybeSetter = 42
val propertyMaybeSetter = 42 // fixed to be a property without setter
var propertyWithSetter = 42
