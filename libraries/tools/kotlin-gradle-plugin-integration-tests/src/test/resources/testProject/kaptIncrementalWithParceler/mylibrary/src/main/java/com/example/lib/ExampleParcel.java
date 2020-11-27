/**
 * Copyright 2011-2015 John Ericksen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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