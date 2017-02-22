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

package org.jetbrains.kotlin.test;

import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class SettingsConfigurator {
    public static final String SET_TRUE_DIRECTIVE = "SET_TRUE:";
    public static final String SET_FALSE_DIRECTIVE = "SET_FALSE:";
    private static final String SET_INT_DIRECTIVE = "SET_INT:";

    private final String[] settingsToTrue;
    private final String[] settingsToFalse;
    private final Pair<String, Integer>[] settingsToIntValue;
    private final Object[] objects;

    public SettingsConfigurator(String fileText, Object... objects) {
        settingsToTrue = InTextDirectivesUtils.findArrayWithPrefixes(fileText, SET_TRUE_DIRECTIVE);
        settingsToFalse = InTextDirectivesUtils.findArrayWithPrefixes(fileText, SET_FALSE_DIRECTIVE);
        //noinspection unchecked
        settingsToIntValue = ContainerUtil.map2Array(
                InTextDirectivesUtils.findArrayWithPrefixes(fileText, SET_INT_DIRECTIVE),
                Pair.class,
                new Function<String, Pair>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public Pair fun(String s) {
                        String[] tokens = s.split("=");
                        return new Pair(tokens[0].trim(), Integer.valueOf(tokens[1].trim()));
                    }
                }
        );
        this.objects = objects;
    }

    private static void setSettingValue(String settingName, Object value, Class<?> valueType, Object... objects) {
        for (Object object : objects) {
            if (setSettingWithField(settingName, object, value) || setSettingWithMethod(settingName, object, value, valueType)) {
                return;
            }
        }

        throw new IllegalArgumentException(String.format(
                "There's no property or method with name '%s' in given objects: %s", settingName, Arrays.toString(objects)));
    }

    private static void setBooleanSetting(String setting, Boolean value, Object... objects) {
        setSettingValue(setting, value, boolean.class, objects);
    }

    private static void setIntSetting(String setting, Integer value, Object... objects) {
        setSettingValue(setting, value, int.class, objects);
    }

    public void configureSettings() {
        for (String trueSetting : settingsToTrue) {
            setBooleanSetting(trueSetting, true, objects);
        }

        for (String falseSetting : settingsToFalse) {
            setBooleanSetting(falseSetting, false, objects);
        }

        for (Pair<String, Integer> setting : settingsToIntValue) {
            setIntSetting(setting.getFirst(), setting.getSecond(), objects);
        }
    }

    public void configureInvertedSettings() {
        for (String trueSetting : settingsToTrue) {
            setBooleanSetting(trueSetting, false, objects);
        }

        for (String falseSetting : settingsToFalse) {
            setBooleanSetting(falseSetting, true, objects);
        }

        for (Pair<String, Integer> setting : settingsToIntValue) {
            setIntSetting(setting.getFirst(), setting.getSecond(), objects);
        }
    }

    private static boolean setSettingWithField(String settingName, Object object, Object value) {
        try {
            Field field = object.getClass().getDeclaredField(settingName);
            field.set(object, value);
            return true;
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Can't set property with the name %s in object %s", settingName, object));
        }
        catch (NoSuchFieldException e) {
            // Do nothing - will try other variants
        }

        return false;
    }

    private static boolean setSettingWithMethod(String setterName, Object object, Object value, Class<?> valueType) {
        try {
            Method method = object.getClass().getMethod(setterName, valueType);
            method.invoke(object, value);
            return true;
        }
        catch (InvocationTargetException e) {
            throw new IllegalArgumentException(String.format("Can't call method with name %s for object %s", setterName, object));
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Can't access to method with name %s for object %s", setterName, object));
        }
        catch (NoSuchMethodException e) {
            // Do nothing - will try other variants
        }

        return false;
    }
}
