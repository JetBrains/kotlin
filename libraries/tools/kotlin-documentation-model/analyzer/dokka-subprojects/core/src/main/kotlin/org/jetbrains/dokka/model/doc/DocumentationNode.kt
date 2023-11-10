/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model.doc

import org.jetbrains.dokka.model.WithChildren

public data class DocumentationNode(override val children: List<TagWrapper>): WithChildren<TagWrapper>
