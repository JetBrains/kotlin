package test.properties

import org.junit.Test as test
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import kotlin.properties.*
import java.util.ArrayList
import java.beans.PropertyChangeSupport
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class SampleBean() {
    private val pcs = PropertyChangeSupport(this)
    var prop: String by pcs.property("")
}

class SupportedBean() : PropertyChangeFeatures {
    public override val pcs = PropertyChangeSupport(this) // can't be anything other than public, see KT-3029
    var prop: String by pcs.property("")
}

private class RecordingListener : PropertyChangeListener {
    val events = ArrayList<PropertyChangeEvent>()

    override fun propertyChange(evt: PropertyChangeEvent) {
        events.add(evt)
    }
}

class PropertyChangeFeaturesTest() {

    test fun initialValueShouldBeCorrect() {
        val subject = SampleBean()

        assertEquals("", subject.prop)
    }

    test fun changingValueShouldwork() {
        val subject = SampleBean()

        // when: value is changed
        subject.prop = "newValue"

        assertEquals("newValue", subject.prop)
    }
}

class PropertySupportTest {

    test fun eventShouldBeFired() {
        val subject = SupportedBean()
        val listener =  RecordingListener()
        subject.addPropertyChangeListener(listener)

        // when: name is changed
        subject.prop = "new name"

        // then: the correct event is fired
        assertEquals(1, listener.events.size)
        val evt = listener.events[0]
        assertEquals("", evt.oldValue)
        assertEquals("new name", evt.newValue)
        assertTrue(evt.source == subject)
    }
}