/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger.stepping;


import com.intellij.openapi.options.ConfigurableUi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerSettings;

import javax.swing.*;

public class KotlinSteppingConfigurableUi implements ConfigurableUi<KotlinDebuggerSettings> {
    private JCheckBox ignoreKotlinMethods;
    private JPanel myPanel;

    @Override
    public void reset(@NotNull KotlinDebuggerSettings settings) {
        boolean flag = settings.getDEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES();
        ignoreKotlinMethods.setSelected(flag);
    }

    @Override
    public boolean isModified(@NotNull KotlinDebuggerSettings settings) {
        return settings.getDEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES() != ignoreKotlinMethods.isSelected();
    }

    @Override
    public void apply(@NotNull KotlinDebuggerSettings settings) {
        settings.setDEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES(ignoreKotlinMethods.isSelected());
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return myPanel;
    }
}
