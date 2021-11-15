// CURIOUS_ABOUT: writeToParcel, createFromParcel, <clinit>
// WITH_STDLIB

//FILE: test/JavaClass.java
package test;

class JavaClass {
    void test() {
        // Here we test access to CREATOR
        Object o = Foo.CREATOR;
    }
}

//FILE: android/os/Parcel.java
package android.os;

public class Parcel {}

//FILE: android/os/Parcelable.java
package android.os;

public interface Parcelable {
    public static interface Creator<T> {
        T createFromParcel(Parcel source);
        T[] newArray(int size);
    }
}


//FILE: test.kt
package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class Foo(val parcelable: Parcelable): Parcelable
