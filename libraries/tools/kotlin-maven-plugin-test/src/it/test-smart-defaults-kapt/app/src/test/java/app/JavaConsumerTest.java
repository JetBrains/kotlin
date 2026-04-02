/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package app;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static app.KotlinServiceExtensionsKt.generatedExtension;

public class JavaConsumerTest {
    @Test
    public void testJavaConsumerUsesGeneratedCode() {
        JavaConsumer consumer = new JavaConsumer();
        String result = consumer.consume();
        assertTrue("Generated message should mention KotlinService", result.contains("KotlinService"));
    }

    @Test
    public void testDirectAccessToGeneratedJavaClass() {
        String message = KotlinServiceGenerated.getGeneratedMessage();
        assertTrue("Generated message should mention KotlinService", message.contains("KotlinService"));
    }

    @Test
    public void testDirectAccessToGeneratedExtension() {
        KotlinService service = new KotlinService();
        String message = generatedExtension(service);
        assertTrue("Generated message should mention KotlinService", message.contains("KotlinService"));
    }
}
