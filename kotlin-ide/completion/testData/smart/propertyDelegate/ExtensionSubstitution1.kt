import kotlin.reflect.KProperty

class Property<TOwner, TValue>(owner: TOwner, value: TValue) {
    companion object {
        fun <TOwner, TValue> create(owner: TOwner, value: TValue) = Property<TOwner, TValue>(owner, value)
    }
}

operator fun <TValue, TOwner> Property<TOwner, TValue>.getValue(thisRef: TOwner, property: KProperty<*>): TValue {
    throw Exception()
}

fun<TOwner, TValue> createProperty(owner: TOwner, value: TValue): Property<TOwner, TValue> = Property(owner, value)

class C {
    val v by <caret>
}

// EXIST: { itemText: "createProperty", typeText: "Property<C, TValue>" }
// EXIST: { itemText: "Property", tailText: "(owner: C, value: TValue) (<root>)" }
// EXIST: { itemText:"Property.create", tailText:"(owner: C, value: TValue) (<root>)", typeText:"Property<C, TValue>" }
