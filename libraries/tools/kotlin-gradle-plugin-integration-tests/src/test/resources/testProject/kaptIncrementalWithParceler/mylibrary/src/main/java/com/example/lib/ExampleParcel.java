// Sample project obtained from https://youtrack.jetbrains.com/issue/KT-34340,
// originally obtained from https://github.com/johncarl81/parceler.
package com.example.lib;


import com.example.lib2.basemodule.BaseClassParcel;

import org.parceler.Parcel;
import org.parceler.ParcelFactory;

/**
 * Intentionally in a different package to make sure we don't accidentally match it with org.parceler Proguard matchers.
 */
@Parcel
public class ExampleParcel extends BaseClassParcel {

    private final String message;

    @ParcelFactory
    public static ExampleParcel create(String message) {
        return new ExampleParcel(message);
    }

    public ExampleParcel(String message) {
        this.message = message;
    }

    public String getMessage(){
        return message;
    }
}