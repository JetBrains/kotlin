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

package org.jetbrains.kotlin.idea.findUsages.dialogs;

import com.intellij.ui.SimpleColoredComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesSupport;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;

import javax.swing.*;
import java.awt.*;

class Utils {
    private Utils() {
    }

    public static void configureLabelComponent(
            @NotNull SimpleColoredComponent coloredComponent,
            @NotNull KtNamedDeclaration declaration
    ) {
        String renderedDeclaration = KotlinFindUsagesSupport.Companion.tryRenderDeclarationCompactStyle(declaration);
        if (renderedDeclaration != null) {
            coloredComponent.append(renderedDeclaration);
        }
    }

    static boolean renameCheckbox(@NotNull JPanel panel, @NotNull String srcText, @NotNull String destText) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if (checkBox.getText().equals(srcText)) {
                    checkBox.setText(destText);
                    return true;
                }
            }
        }

        return false;
    }

    static void removeCheckbox(@NotNull JPanel panel, @NotNull String srcText) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if (checkBox.getText().equals(srcText)) {
                    panel.remove(checkBox);
                    return;
                }
            }
        }
    }
}
