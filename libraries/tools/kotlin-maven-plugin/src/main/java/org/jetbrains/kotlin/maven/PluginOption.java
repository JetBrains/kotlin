/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.maven;

public class PluginOption {
    /** The plugin name in Maven, e.g. "all-open" */
    public final String pluginName;

    /** The compiler plugin identifier, e.g. "org.jetbrains.kotlin.allopen" */
    public final String pluginId;

    public final String key;
    public final String value;

    public PluginOption(String pluginName, String pluginId, String key, String value) {
        this.pluginName = pluginName;
        this.pluginId = pluginId;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "plugin:" + pluginId + ":" + key + "=" + value;
    }
}
