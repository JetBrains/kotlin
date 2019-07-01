/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections;

import java.util.Arrays;
import java.util.List;

class ArraysUtilJVM {
    static <T> List<T> asList(T[] array) {
        return Arrays.asList(array);
    }
}


