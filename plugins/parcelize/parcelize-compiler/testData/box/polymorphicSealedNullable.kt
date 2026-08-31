// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
@PolymorphicSealed
sealed class Foo : Parcelable {
    data class A(val x: Int) : Foo()
    object B : Foo()
    data class C(val x: String) : Foo()
}

@Parcelize
@PolymorphicSealed
sealed interface Node : Parcelable {
    data class Leaf(val value: String) : Node
    object Empty : Node
}

@Parcelize
data class Container(
    val foo: Foo?,
    val specificFoo: Foo.A?,
    val node: Node?,
) : Parcelable

fun box() = parcelTest { parcel ->
    val c1 = Container(Foo.A(42), Foo.A(100), Node.Leaf("leaf"))
    val c2 = Container(Foo.B, null, Node.Empty)
    val c3 = Container(null, null, null)

    c1.writeToParcel(parcel, 0)
    c2.writeToParcel(parcel, 0)
    c3.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val creator = parcelableCreator<Container>()
    val r1 = creator.createFromParcel(parcel)
    val r2 = creator.createFromParcel(parcel)
    val r3 = creator.createFromParcel(parcel)

    assert(c1 == r1)
    assert(c2 == r2)
    assert(c3 == r3)
}
