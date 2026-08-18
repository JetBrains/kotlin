/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

public class ExternalSnippets {
    public static void snippetFunction() {

        // @start region="configSetup"
        Config config = new Config();
        config.setEnabled(true);
        // @end region="configSetup"

        // @start region="validation"
        if (value != null && value.length() > 0) {
            return true;
        }
        // @end
    }
}
