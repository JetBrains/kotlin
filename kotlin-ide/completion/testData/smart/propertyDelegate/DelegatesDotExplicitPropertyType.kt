import kotlin.properties.Delegates

class C {
    val v: String by Delegates.<caret>
}

// EXIST: { itemText: "notNull", typeText: "ReadWriteProperty<Any?, String>" }
// EXIST: { itemText: "observable", typeText: "ReadWriteProperty<Any?, String>" }
// EXIST: { itemText: "vetoable", typeText: "ReadWriteProperty<Any?, String>" }
// NOTHING_ELSE
