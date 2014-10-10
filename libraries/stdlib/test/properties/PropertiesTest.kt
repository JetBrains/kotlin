package test.properties

import kotlin.*
import kotlin.properties.*
import kotlin.test.*
import java.util.*
import org.junit.Test as test

class Customer : ChangeSupport() {
    // TODO the setter code should be generated
    // via KT-1299
    var name: String? = null
    set(value) {
        changeProperty("name", $name, value)
        $name = value
    }

    var city: String? = null
    set(value) {
        changeProperty("city", $city, value)
        $city = value
    }

    override fun toString() = "Customer($name, $city)"
}

class MyChangeListener : ChangeListener {
    val events = ArrayList<ChangeEvent>()

    override fun onPropertyChange(event: ChangeEvent): Unit {
        println("Property changed: $event")
        events.add(event)
    }
}

class PropertiesTest {

    test fun testModel() {
        val c = Customer()
        c.name = "James"
        c.city = "Mells"

        val listener = MyChangeListener()
        c.addChangeListener(listener)
        c.name = "Andrey"
        println("Customer $c and raised change events ${listener.events}")

        assertEquals(1, listener.events.size(), "Should have received a change event ${listener.events}")
    }
}