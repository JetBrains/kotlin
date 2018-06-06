// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>
// WITH_RUNTIME

//FILE: test/JavaClass.java
package test;

class JavaClass {
    void test() {
        // Here we test access to CREATOR
        Object o = Foo.CREATOR;
    }
}

//FILE: android/os/Parcelable.java
package android.os;

public interface Parcelable {
    public static interface Creator<T> {}
}


//FILE: test.kt
package test

import kotlinx.android.parcel.*
import android.os.Parcelable

@Parcelize
class Foo(val parcelable: Parcelable): Parcelable