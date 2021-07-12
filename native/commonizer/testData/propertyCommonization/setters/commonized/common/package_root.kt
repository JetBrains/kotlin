expect var defaultSetter1: Int
expect var defaultSetter2: Int
expect var defaultSetter3: Int

expect var setterWithoutBackingField1: Int
expect var setterWithoutBackingField2: Int

expect var setterWithDelegation1: Int
expect var setterWithDelegation2: Int

expect var defaultSetteCustomVisibility1: Int
expect var defaultSetteCustomVisibility2: Int
    private set
expect var defaultSetteCustomVisibility3 = 42
    internal set
expect var defaultSetteCustomVisibility4: Int
    private set
expect var defaultSetteCustomVisibility5: Int
    private set

expect val propertyWithoutSetter: Int
expect var propertyMaybeSetter: Int
    private set
expect var propertyWithSetter: Int