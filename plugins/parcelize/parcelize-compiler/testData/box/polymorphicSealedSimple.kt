// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
@PolymorphicSealed
sealed class State : Parcelable {
    object Loading : State()
    data class Content(val data: String) : State()
    class Error(val code: Int) : State() {
        override fun equals(other: Any?): Boolean = other is Error && code == other.code
    }
}

@Parcelize
@PolymorphicSealed
sealed interface Event : Parcelable {
    data class Click(val id: String) : Event
    object Swipe : Event
    class Custom(val name: String) : Event {
        override fun equals(other: Any?): Boolean = other is Custom && name == other.name
    }
}

@Parcelize
data class Container(
    val state: State,
    val event: Event,
    val optionalState: State?,
) : Parcelable

fun box() = parcelTest { parcel ->
    val s1: State = State.Loading
    val s2: State = State.Content("OK")
    val s3: State = State.Error(404)
    val e1: Event = Event.Click("btn")
    val e2: Event = Event.Swipe
    val e3: Event = Event.Custom("custom")
    val c = Container(s2, e1, null)

    s1.writeToParcel(parcel, 0)
    s2.writeToParcel(parcel, 0)
    s3.writeToParcel(parcel, 0)
    e1.writeToParcel(parcel, 0)
    e2.writeToParcel(parcel, 0)
    e3.writeToParcel(parcel, 0)
    c.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    assert(s1 == parcelableCreator<State>().createFromParcel(parcel))
    assert(s2 == parcelableCreator<State>().createFromParcel(parcel))
    assert(s3 == parcelableCreator<State>().createFromParcel(parcel))
    assert(e1 == parcelableCreator<Event>().createFromParcel(parcel))
    assert(e2 == parcelableCreator<Event>().createFromParcel(parcel))
    assert(e3 == parcelableCreator<Event>().createFromParcel(parcel))
    assert(c == parcelableCreator<Container>().createFromParcel(parcel))
}
