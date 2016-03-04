// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintParcelCreatorInspection

@file:Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
import android.os.Parcel
import android.os.Parcelable

class <error descr="This class implements `Parcelable` but does not provide a `CREATOR` field">MyParcelable1</error> : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(arg0: Parcel, arg1: Int) {}
}

internal class MyParcelable2 : Parcelable {
    override fun describeContents() = 0

    override fun writeToParcel(arg0: Parcel, arg1: Int) {}

    companion object {
        val CREATOR: Parcelable.Creator<String> = object : Parcelable.Creator<String> {
            override fun newArray(size: Int) = null!!
            override fun createFromParcel(source: Parcel?) = null!!
        }
    }
}

internal class MyParcelable3 : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(arg0: Parcel, arg1: Int) {}

    companion object {
        val CREATOR = 0 // Wrong type
    }
}

internal abstract class MyParcelable4 : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(arg0: Parcel, arg1: Int) {}
}
