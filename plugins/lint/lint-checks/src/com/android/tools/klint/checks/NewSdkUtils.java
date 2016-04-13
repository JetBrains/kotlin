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

package com.android.tools.klint.checks;

import com.android.annotations.NonNull;

public class NewSdkUtils {
    /**
     * Get the R field name from a resource name, since
     * AAPT will flatten the namespace, turning dots, dashes and colons into _
     *
     * @param resourceName the name to convert
     * @return the corresponding R field name
     */
    @NonNull
    public static String getResourceFieldName(@NonNull String resourceName) {
        // AAPT will flatten the namespace, turning dots, dashes and colons into _
        for (int i = 0, n = resourceName.length(); i < n; i++) {
            char c = resourceName.charAt(i);
            if (c == '.' || c == ':' || c == '-') {
                return resourceName.replace('.', '_').replace('-', '_').replace(':', '_');
            }
        }

        return resourceName;
    }


}
