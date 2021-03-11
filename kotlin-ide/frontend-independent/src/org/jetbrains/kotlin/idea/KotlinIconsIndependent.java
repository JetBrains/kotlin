/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.PlatformIcons;

import javax.swing.*;

public interface KotlinIconsIndependent {
    Icon SMALL_LOGO = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin.svg");

    Icon CLASS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/classKotlin.svg");
    Icon ABSTRACT_CLASS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/abstractClassKotlin.svg");
    Icon ENUM = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/enumKotlin.svg");
    Icon FILE = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_file.svg");
    Icon SCRIPT = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_script.svg");
    Icon GRADLE_SCRIPT = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_gradle_script.svg");
    Icon OBJECT = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/objectKotlin.svg");
    Icon INTERFACE = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/interfaceKotlin.svg");
    Icon ANNOTATION = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/annotationKotlin.svg");
    Icon FUNCTION = AllIcons.Nodes.Function;
    Icon EXTENSION_FUNCTION = PlatformIcons.FUNCTION_ICON;
    Icon ABSTRACT_EXTENSION_FUNCTION = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/abstract_extension_function.svg");
    Icon LAMBDA = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/lambda.svg");
    Icon PARAMETER = PlatformIcons.PARAMETER_ICON;
    Icon FIELD_VAL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/field_value.svg");
    Icon FIELD_VAR = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/field_variable.svg");
    Icon CLASS_INITIALIZER = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/classInitializerKotlin.svg");
    Icon TYPE_ALIAS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/typeAlias.svg");

    Icon ACTUAL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/actual.svg");
    Icon EXPECT = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/expect.svg");

    Icon LAUNCH = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_launch_configuration.svg");
    Icon LOAD_SCRIPT_CONFIGURATION = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/loadScriptConfiguration.svg");

    Icon JS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_js.svg");
    Icon MPP = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_multiplatform_project.svg");
    Icon NATIVE = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_native.svg");
}
