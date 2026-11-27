/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import java.lang.reflect.Modifier;
import javax.persistence.NoArgClass;

public class NoArgJpaStandaloneTest {
    @Test
    public void hasNoArgConstructor() {
        assertEquals(2, NoArgClass.class.getDeclaredConstructors().length);
    }

    @Test
    public void isOpen() {
        assertFalse(Modifier.isFinal(NoArgClass.class.getModifiers()));
    }
}
