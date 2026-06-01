/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kt86443

class KotlinClassForThreadTest {
    override fun toString(): String = "KotlinClassForThreadTest"
    override fun hashCode(): Int = 42
    override fun equals(other: Any?): Boolean = other is KotlinClassForThreadTest
}
