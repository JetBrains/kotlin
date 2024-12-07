/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test;

import java.util.ArrayList;
import java.util.List;

public class JavaDeclaration {
    public static List<Object> getList() {
        return new ArrayList<>();
    }

    public static Object getAny() {
        return "";
    }

    public static List getRawList() {
        return new ArrayList<>();
    }
}
