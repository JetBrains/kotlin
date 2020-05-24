/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.ide.highlighter.JavaHighlightingColors;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class KotlinHighlightingColors {
    // default keys (mostly syntax elements)
    public static final TextAttributesKey KEYWORD = createTextAttributesKey("KOTLIN_KEYWORD", JavaHighlightingColors.KEYWORD);
    public static final TextAttributesKey BUILTIN_ANNOTATION = createTextAttributesKey("KOTLIN_BUILTIN_ANNOTATION", KotlinHighlightingColors.KEYWORD);
    public static final TextAttributesKey VAL_KEYWORD = createTextAttributesKey("KOTLIN_KEYWORD_VAL", KotlinHighlightingColors.KEYWORD);
    public static final TextAttributesKey VAR_KEYWORD = createTextAttributesKey("KOTLIN_KEYWORD_VAR", KotlinHighlightingColors.KEYWORD);
    public static final TextAttributesKey NUMBER = createTextAttributesKey("KOTLIN_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey STRING = createTextAttributesKey("KOTLIN_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey STRING_ESCAPE = createTextAttributesKey("KOTLIN_STRING_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
    public static final TextAttributesKey INVALID_STRING_ESCAPE = createTextAttributesKey("KOTLIN_INVALID_STRING_ESCAPE", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
    public static final TextAttributesKey OPERATOR_SIGN = createTextAttributesKey("KOTLIN_OPERATION_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey PARENTHESIS = createTextAttributesKey("KOTLIN_PARENTHESIS", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey BRACES = createTextAttributesKey("KOTLIN_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey BRACKETS = createTextAttributesKey("KOTLIN_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey FUNCTION_LITERAL_BRACES_AND_ARROW = createTextAttributesKey("KOTLIN_FUNCTION_LITERAL_BRACES_AND_ARROW");
    public static final TextAttributesKey COMMA = createTextAttributesKey("KOTLIN_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey SEMICOLON = createTextAttributesKey("KOTLIN_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    public static final TextAttributesKey COLON = createTextAttributesKey("KOTLIN_COLON");
    public static final TextAttributesKey DOUBLE_COLON = createTextAttributesKey("KOTLIN_DOUBLE_COLON");
    public static final TextAttributesKey DOT = createTextAttributesKey("KOTLIN_DOT", DefaultLanguageHighlighterColors.DOT);
    public static final TextAttributesKey SAFE_ACCESS = createTextAttributesKey("KOTLIN_SAFE_ACCESS", DefaultLanguageHighlighterColors.DOT);
    public static final TextAttributesKey QUEST = createTextAttributesKey("KOTLIN_QUEST");
    public static final TextAttributesKey EXCLEXCL = createTextAttributesKey("KOTLIN_EXCLEXCL");
    public static final TextAttributesKey ARROW = createTextAttributesKey("KOTLIN_ARROW", PARENTHESIS);
    public static final TextAttributesKey LINE_COMMENT = createTextAttributesKey("KOTLIN_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BLOCK_COMMENT = createTextAttributesKey("KOTLIN_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey DOC_COMMENT = createTextAttributesKey("KOTLIN_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);
    public static final TextAttributesKey KDOC_TAG = createTextAttributesKey("KDOC_TAG_NAME", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG);
    public static final TextAttributesKey KDOC_LINK = createTextAttributesKey("KDOC_LINK", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE);

    // class kinds
    public static final TextAttributesKey CLASS = createTextAttributesKey("KOTLIN_CLASS", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey TYPE_PARAMETER = createTextAttributesKey("KOTLIN_TYPE_PARAMETER", JavaHighlightingColors.TYPE_PARAMETER_NAME_ATTRIBUTES);
    public static final TextAttributesKey ABSTRACT_CLASS = createTextAttributesKey("KOTLIN_ABSTRACT_CLASS", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey TRAIT = createTextAttributesKey("KOTLIN_TRAIT", DefaultLanguageHighlighterColors.INTERFACE_NAME);
    public static final TextAttributesKey ANNOTATION = createTextAttributesKey("KOTLIN_ANNOTATION", JavaHighlightingColors.ANNOTATION_NAME_ATTRIBUTES);
    public static final TextAttributesKey OBJECT = createTextAttributesKey("KOTLIN_OBJECT", CLASS);
    public static final TextAttributesKey ENUM_ENTRY = createTextAttributesKey("KOTLIN_ENUM_ENTRY", DefaultLanguageHighlighterColors.STATIC_FIELD);
    public static final TextAttributesKey TYPE_ALIAS = createTextAttributesKey("KOTLIN_TYPE_ALIAS", CLASS);

    // variable kinds
    public static final TextAttributesKey MUTABLE_VARIABLE = createTextAttributesKey("KOTLIN_MUTABLE_VARIABLE");
    public static final TextAttributesKey LOCAL_VARIABLE = createTextAttributesKey("KOTLIN_LOCAL_VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    public static final TextAttributesKey PARAMETER = createTextAttributesKey("KOTLIN_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER);
    public static final TextAttributesKey WRAPPED_INTO_REF = createTextAttributesKey("KOTLIN_WRAPPED_INTO_REF", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey INSTANCE_PROPERTY = createTextAttributesKey("KOTLIN_INSTANCE_PROPERTY", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey PACKAGE_PROPERTY = createTextAttributesKey("KOTLIN_PACKAGE_PROPERTY", DefaultLanguageHighlighterColors.STATIC_FIELD);
    public static final TextAttributesKey BACKING_FIELD_VARIABLE = createTextAttributesKey("KOTLIN_BACKING_FIELD_VARIABLE");
    public static final TextAttributesKey EXTENSION_PROPERTY = createTextAttributesKey("KOTLIN_EXTENSION_PROPERTY", DefaultLanguageHighlighterColors.STATIC_FIELD);
    public static final TextAttributesKey SYNTHETIC_EXTENSION_PROPERTY = createTextAttributesKey("KOTLIN_SYNTHETIC_EXTENSION_PROPERTY", EXTENSION_PROPERTY);
    public static final TextAttributesKey DYNAMIC_PROPERTY_CALL = createTextAttributesKey("KOTLIN_DYNAMIC_PROPERTY_CALL");
    public static final TextAttributesKey ANDROID_EXTENSIONS_PROPERTY_CALL = createTextAttributesKey("KOTLIN_ANDROID_EXTENSIONS_PROPERTY_CALL");
    public static final TextAttributesKey INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION = createTextAttributesKey("KOTLIN_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION", INSTANCE_PROPERTY);
    public static final TextAttributesKey PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION = createTextAttributesKey("KOTLIN_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION", PACKAGE_PROPERTY);

    // functions
    public static final TextAttributesKey FUNCTION_LITERAL_DEFAULT_PARAMETER = createTextAttributesKey("KOTLIN_CLOSURE_DEFAULT_PARAMETER", PARAMETER);
    public static final TextAttributesKey FUNCTION_DECLARATION = createTextAttributesKey("KOTLIN_FUNCTION_DECLARATION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
    public static final TextAttributesKey FUNCTION_CALL = createTextAttributesKey("KOTLIN_FUNCTION_CALL", DefaultLanguageHighlighterColors.FUNCTION_CALL);
    public static final TextAttributesKey PACKAGE_FUNCTION_CALL = createTextAttributesKey("KOTLIN_PACKAGE_FUNCTION_CALL", DefaultLanguageHighlighterColors.STATIC_METHOD);
    public static final TextAttributesKey EXTENSION_FUNCTION_CALL = createTextAttributesKey("KOTLIN_EXTENSION_FUNCTION_CALL", DefaultLanguageHighlighterColors.STATIC_METHOD);
    public static final TextAttributesKey CONSTRUCTOR_CALL = createTextAttributesKey("KOTLIN_CONSTRUCTOR", DefaultLanguageHighlighterColors.FUNCTION_CALL);
    public static final TextAttributesKey DYNAMIC_FUNCTION_CALL = createTextAttributesKey("KOTLIN_DYNAMIC_FUNCTION_CALL");
    public static final TextAttributesKey SUSPEND_FUNCTION_CALL = createTextAttributesKey("KOTLIN_SUSPEND_FUNCTION_CALL", KotlinHighlightingColors.FUNCTION_CALL);
    public static final TextAttributesKey VARIABLE_AS_FUNCTION_CALL = createTextAttributesKey("KOTLIN_VARIABLE_AS_FUNCTION");
    public static final TextAttributesKey VARIABLE_AS_FUNCTION_LIKE_CALL = createTextAttributesKey("KOTLIN_VARIABLE_AS_FUNCTION_LIKE");

    // other
    public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey("KOTLIN_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);
    public static final TextAttributesKey SMART_CAST_VALUE = createTextAttributesKey("KOTLIN_SMART_CAST_VALUE");
    public static final TextAttributesKey SMART_CONSTANT = createTextAttributesKey("KOTLIN_SMART_CONSTANT");
    public static final TextAttributesKey SMART_CAST_RECEIVER = createTextAttributesKey("KOTLIN_SMART_CAST_RECEIVER");
    public static final TextAttributesKey LABEL = createTextAttributesKey("KOTLIN_LABEL", DefaultLanguageHighlighterColors.LABEL);
    public static final TextAttributesKey DEBUG_INFO = createTextAttributesKey("KOTLIN_DEBUG_INFO");
    public static final TextAttributesKey RESOLVED_TO_ERROR = createTextAttributesKey("KOTLIN_RESOLVED_TO_ERROR");
    public static final TextAttributesKey NAMED_ARGUMENT = createTextAttributesKey("KOTLIN_NAMED_ARGUMENT");
    public static final TextAttributesKey ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES = createTextAttributesKey("KOTLIN_ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES", JavaHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES);

    private KotlinHighlightingColors() {
    }
}
