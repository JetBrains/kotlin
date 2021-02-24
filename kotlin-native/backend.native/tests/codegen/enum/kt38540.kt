/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.kt38540

import kotlin.test.*

public enum class Node(
        public val external: Boolean,
        public val dependsOn: Set<Node>,
        public val required: Boolean
) {
    A(
            external = false,
            dependsOn = emptySet(),
            required = true
    ),
    B(
            external = false,
            dependsOn = emptySet(),
            required = true
    ),
    C(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    D(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    E(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    F(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    G(
            external = true,
            dependsOn = emptySet(),
            required = false
    ),
    H(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    I(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    J(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    K(
            external = true,
            dependsOn = setOf(I),
            required = true
    ),
    L(
            external = true,
            dependsOn = setOf(I),
            required = true
    ),
    M(
            external = true,
            dependsOn = setOf(I),
            required = true
    ),
    N(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    O(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    AG(
            external = true,
            dependsOn = emptySet(),
            required = true
    ),
    FIELD_REPORT(
            external = true,
            dependsOn = setOf(AG, O, J),
            required = true
    )
}

@Test
fun runTest() {
    assertTrue(Node.FIELD_REPORT.dependsOn.contains(Node.AG))
    assertTrue(Node.FIELD_REPORT.dependsOn.contains(Node.O))
    assertTrue(Node.FIELD_REPORT.dependsOn.contains(Node.J))
}