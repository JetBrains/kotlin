// https://issuetracker.google.com/590046666
// WITH_STDLIB
// LANGUAGE: +ContextParameters

// MODULE: lib
// FILE: lib.kt

package test

interface ExternalHasExtension {
    val String.x: Int get() = length
}

data class ExternalWithExtension(val x: Int) {
    val String.x: Int get() = length
}

data class ExternalWithInheritedExtension(val x: Int) : ExternalHasExtension

data class ExternalWithContextProperty(val x: Int) {
    context(s: String)
    @Suppress("CONTEXTUAL_OVERLOAD_SHADOWED")
    val x: Int get() = s.length
}

data class ExternalJvmFieldWithExtension(@JvmField val x: Int) {
    val String.x: Int get() = length
}

// MODULE: main(lib)
// FILE: main.kt

@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")

package test

import android.os.Parcelable
import kotlinx.parcelize.*

interface LocalHasExtension {
    val String.x: Int get() = length
}

data class LocalWithExtension(val x: Int) {
    val String.x: Int get() = length
}

data class LocalWithInheritedExtension(val x: Int) : LocalHasExtension

data class LocalWithContextProperty(val x: Int) {
    context(s: String)
    @Suppress("CONTEXTUAL_OVERLOAD_SHADOWED")
    val x: Int get() = s.length
}

data class LocalJvmFieldWithExtension(@JvmField val x: Int) {
    val String.x: Int get() = length
}

@Parcelize
data class Wrapper(
    val localExt: @DataClass LocalWithExtension,
    val localInheritedExt: @DataClass LocalWithInheritedExtension,
    val localCtx: @DataClass LocalWithContextProperty,
    val localJvmFieldExt: @DataClass LocalJvmFieldWithExtension,
    val externalExt: @DataClass ExternalWithExtension,
    val externalInheritedExt: @DataClass ExternalWithInheritedExtension,
    val externalCtx: @DataClass ExternalWithContextProperty,
    val externalJvmFieldExt: @DataClass ExternalJvmFieldWithExtension,
) : Parcelable

fun box() = parcelTest { parcel ->
    val original = Wrapper(
        localExt = LocalWithExtension(1),
        localInheritedExt = LocalWithInheritedExtension(2),
        localCtx = LocalWithContextProperty(3),
        localJvmFieldExt = LocalJvmFieldWithExtension(4),
        externalExt = ExternalWithExtension(5),
        externalInheritedExt = ExternalWithInheritedExtension(6),
        externalCtx = ExternalWithContextProperty(7),
        externalJvmFieldExt = ExternalJvmFieldWithExtension(8),
    )
    original.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val restored = parcelableCreator<Wrapper>().createFromParcel(parcel)
    check(original == restored) { "Expected $original, got $restored" }
    check(parcel.dataAvail() == 0) { "Parcel has ${parcel.dataAvail()} unread bytes" }
}
