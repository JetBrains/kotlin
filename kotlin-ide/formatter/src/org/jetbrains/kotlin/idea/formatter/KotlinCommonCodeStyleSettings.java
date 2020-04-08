/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter;

import com.intellij.lang.Language;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.DifferenceFilter;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementUtil;
import com.intellij.util.xmlb.XmlSerializer;
import kotlin.collections.ArraysKt;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.util.FormatterUtilKt;
import org.jetbrains.kotlin.idea.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnnecessaryFinalOnLocalVariableOrParameter")
public class KotlinCommonCodeStyleSettings extends CommonCodeStyleSettings {
    @ReflectionUtil.SkipInEquals
    public String CODE_STYLE_DEFAULTS = null;

    /**
     * Load settings with previous IDEA defaults to have an ability to restore them.
     */
    @Nullable
    private KotlinCommonCodeStyleSettings settingsAgainstPreviousDefaults = null;

    private final boolean isTempForDeserialize;

    public KotlinCommonCodeStyleSettings() {
        this(false);
    }

    private KotlinCommonCodeStyleSettings(boolean isTempForDeserialize) {
        super(KotlinLanguage.INSTANCE);
        this.isTempForDeserialize = isTempForDeserialize;
    }

    private static KotlinCommonCodeStyleSettings createForTempDeserialize() {
        return new KotlinCommonCodeStyleSettings(true);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        if (isTempForDeserialize) {
            super.readExternal(element);
            return;
        }

        KotlinCommonCodeStyleSettings tempDeserialize = createForTempDeserialize();
        tempDeserialize.readExternal(element);

        String customDefaults = tempDeserialize.CODE_STYLE_DEFAULTS;
        if (KotlinStyleGuideCodeStyle.CODE_STYLE_ID.equals(customDefaults)) {
            KotlinStyleGuideCodeStyle.Companion.applyToCommonSettings(this, true);
        }
        else if (KotlinObsoleteCodeStyle.CODE_STYLE_ID.equals(customDefaults)) {
            KotlinObsoleteCodeStyle.Companion.applyToCommonSettings(this, true);
        }
        else if (customDefaults == null && FormatterUtilKt.isDefaultOfficialCodeStyle()) {
            // Temporary load settings against previous defaults
            settingsAgainstPreviousDefaults = createForTempDeserialize();
            KotlinObsoleteCodeStyle.Companion.applyToCommonSettings(settingsAgainstPreviousDefaults, true);
            settingsAgainstPreviousDefaults.readExternal(element);
        }

        readExternalBase(element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        CommonCodeStyleSettings defaultSettings = getDefaultSettings();

        if (defaultSettings != null) {
            if (KotlinStyleGuideCodeStyle.CODE_STYLE_ID.equals(CODE_STYLE_DEFAULTS)) {
                KotlinStyleGuideCodeStyle.Companion.applyToCommonSettings(defaultSettings, false);
            }
            else if (KotlinObsoleteCodeStyle.CODE_STYLE_ID.equals(CODE_STYLE_DEFAULTS)) {
                KotlinObsoleteCodeStyle.Companion.applyToCommonSettings(defaultSettings, false);
            }
        }

        writeExternalBase(element, defaultSettings);
    }

    //<editor-fold desc="Copied and adapted from CommonCodeStyleSettings ">
    private void readExternalBase(Element element) throws InvalidDataException {
        super.readExternal(element);
    }

    private void writeExternalBase(Element element, CommonCodeStyleSettings defaultSettings) throws WriteExternalException {
        Set<String> supportedFields = getSupportedFields();
        if (supportedFields != null) {
            supportedFields.add("FORCE_REARRANGE_MODE");
            supportedFields.add("CODE_STYLE_DEFAULTS");
        }
        //noinspection deprecation
        DefaultJDOMExternalizer.writeExternal(this, element, new SupportedFieldsDiffFilter(this, supportedFields, defaultSettings));
        List<Integer> softMargins = getSoftMargins();
        serializeInto(softMargins, element);

        IndentOptions myIndentOptions = getIndentOptions();
        if (myIndentOptions != null) {
            IndentOptions defaultIndentOptions = defaultSettings != null ? defaultSettings.getIndentOptions() : null;
            Element indentOptionsElement = new Element(INDENT_OPTIONS_TAG);
            myIndentOptions.serialize(indentOptionsElement, defaultIndentOptions);
            if (!indentOptionsElement.getChildren().isEmpty()) {
                element.addContent(indentOptionsElement);
            }
        }

        ArrangementSettings myArrangementSettings = getArrangementSettings();
        if (myArrangementSettings != null) {
            Element container = new Element(ARRANGEMENT_ELEMENT_NAME);
            ArrangementUtil.writeExternal(container, myArrangementSettings, myLanguage);
            if (!container.getChildren().isEmpty()) {
                element.addContent(container);
            }
        }
    }

    @Override
    public CommonCodeStyleSettings clone(@NotNull CodeStyleSettings rootSettings) {
        KotlinCommonCodeStyleSettings commonSettings = new KotlinCommonCodeStyleSettings();

        commonSettings.settingsAgainstPreviousDefaults = settingsAgainstPreviousDefaults;

        copyPublicFieldsOwn(this, commonSettings);

        try {
            Method setRootSettingsMethod = CommonCodeStyleSettings.class.getDeclaredMethod("setRootSettings", CodeStyleSettings.class);
            setRootSettingsMethod.setAccessible(true);
            setRootSettingsMethod.invoke(commonSettings, rootSettings);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

        commonSettings.setForceArrangeMenuAvailable(isForceArrangeMenuAvailable());

        IndentOptions indentOptions = getIndentOptions();
        if (indentOptions != null) {
            IndentOptions targetIndentOptions = commonSettings.initIndentOptions();
            targetIndentOptions.copyFrom(indentOptions);
        }

        ArrangementSettings arrangementSettings = getArrangementSettings();
        if (arrangementSettings != null) {
            commonSettings.setArrangementSettings(arrangementSettings.clone());
        }

        try {
            Method setRootSettingsMethod = ArraysKt.singleOrNull(
                    CommonCodeStyleSettings.class.getDeclaredMethods(),
                    method -> "setSoftMargins".equals(method.getName()));

            if (setRootSettingsMethod != null) {
                // Method was introduced in 173
                setRootSettingsMethod.setAccessible(true);
                setRootSettingsMethod.invoke(commonSettings, getSoftMargins());
            }
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

        return commonSettings;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KotlinCommonCodeStyleSettings)) {
            return false;
        }

        if (!ReflectionUtil.comparePublicNonFinalFieldsWithSkip(this, obj)) {
            return false;
        }

        CommonCodeStyleSettings other = (CommonCodeStyleSettings) obj;
        if (!getSoftMargins().equals(other.getSoftMargins())) {
            return false;
        }

        IndentOptions options = getIndentOptions();
        if ((options == null && other.getIndentOptions() != null) ||
            (options != null && !options.equals(other.getIndentOptions()))) {
            return false;
        }

        return arrangementSettingsEqual(other);
    }

    // SoftMargins.serializeInfo
    private void serializeInto(@NotNull List<Integer> softMargins, @NotNull Element element) {
        if (softMargins.size() > 0) {
            XmlSerializer.serializeInto(this, element);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Copied from CommonCodeStyleSettings">
    private static final String INDENT_OPTIONS_TAG = "indentOptions";
    private static final String ARRANGEMENT_ELEMENT_NAME = "arrangement";

    private final Language myLanguage = KotlinLanguage.INSTANCE;

    @Nullable
    private CommonCodeStyleSettings getDefaultSettings() {
        return LanguageCodeStyleSettingsProvider.getDefaultCommonSettings(myLanguage);
    }

    @Nullable
    private Set<String> getSupportedFields() {
        final LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(myLanguage);
        return provider == null ? null : provider.getSupportedFields();
    }

    public boolean canRestore() {
        return settingsAgainstPreviousDefaults != null;
    }

    public void restore() {
        if (settingsAgainstPreviousDefaults != null) {
            copyFrom(settingsAgainstPreviousDefaults);
        }
    }

    private static class SupportedFieldsDiffFilter extends DifferenceFilter<CommonCodeStyleSettings> {
        private final Set<String> mySupportedFieldNames;

        public SupportedFieldsDiffFilter(
                final CommonCodeStyleSettings object,
                Set<String> supportedFiledNames,
                final CommonCodeStyleSettings parentObject
        ) {
            super(object, parentObject);
            mySupportedFieldNames = supportedFiledNames;
        }

        @Override
        public boolean isAccept(@NotNull Field field) {
            if (mySupportedFieldNames == null ||
                mySupportedFieldNames.contains(field.getName())) {
                return super.isAccept(field);
            }
            return false;
        }
    }

    // Can't use super.copyPublicFields because the method is internal in 181
    private static void copyPublicFieldsOwn(Object from, Object to) {
        assert from != to;
        com.intellij.util.ReflectionUtil.copyFields(to.getClass().getFields(), from, to);
    }
    //</editor-fold>
}
