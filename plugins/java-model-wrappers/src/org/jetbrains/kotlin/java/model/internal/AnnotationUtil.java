/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.java.model.internal;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import sun.reflect.annotation.EnumConstantNotPresentExceptionProxy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AnnotationUtil {
    public static final Map<PsiType, Class<?>> ARRAY_TYPES_MAP; 
    
    static {
        Map<PsiType, Class<?>> classes = new HashMap<PsiType, Class<?>>();
        
        classes.put(PsiType.BYTE, byte.class);
        classes.put(PsiType.SHORT, short.class);
        classes.put(PsiType.INT, int.class);
        classes.put(PsiType.CHAR, char.class);
        classes.put(PsiType.BOOLEAN, boolean.class);
        classes.put(PsiType.LONG, long.class);
        classes.put(PsiType.FLOAT, float.class);
        classes.put(PsiType.DOUBLE, double.class);
        
        ARRAY_TYPES_MAP = Collections.unmodifiableMap(classes);
    }
    
    public static Object createEnumValue(Class<?> enumClass, String name) {
        try {
            return Enum.valueOf((Class)enumClass, name);
        } catch (IllegalArgumentException ex) {
            //noinspection unchecked
            return new EnumConstantNotPresentExceptionProxy((Class<Enum<?>>) enumClass, name);
        }

    }
}
