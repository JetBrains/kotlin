/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.kdoc;

import org.jetbrains.kotlin.idea.highlighter.AbstractHighlightingTest;
import org.jetbrains.kotlin.idea.inspections.kdoc.KDocMissingDocumentationInspection;

public abstract class AbstractKDocHighlightingTest extends AbstractHighlightingTest {
    @Override
    protected void setUp() {
        super.setUp();
        myFixture.enableInspections(KDocUnresolvedReferenceInspection.class);
        myFixture.enableInspections(KDocMissingDocumentationInspection.class);
    }
}
