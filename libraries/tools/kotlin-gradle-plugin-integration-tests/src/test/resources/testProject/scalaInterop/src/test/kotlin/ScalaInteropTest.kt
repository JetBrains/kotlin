/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import org.junit.Assert
import kotlin.reflect.full.valueParameters

class ScalaInteropTest {

    @org.junit.Test
    fun parametersInInnerClassConstructor() {
        val inner = Outer().Inner("123")
        Assert.assertEquals("123", inner.name())

        val valueParameters = inner::class.constructors.single().valueParameters
        Assert.assertEquals(1, valueParameters.size)
        val annotations = valueParameters[0].annotations
        Assert.assertEquals(1, annotations.size)
        Assert.assertEquals("Foo", annotations[0].annotationClass.simpleName)
    }
}
