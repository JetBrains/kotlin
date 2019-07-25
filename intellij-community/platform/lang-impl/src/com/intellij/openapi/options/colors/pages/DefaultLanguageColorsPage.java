// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.options.colors.pages;

import com.intellij.codeHighlighting.RainbowHighlighter;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.RainbowColorSettingsPage;
import com.intellij.psi.codeStyle.DisplayPriority;
import com.intellij.psi.codeStyle.DisplayPrioritySortable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows to set default colors for multiple languages.
 *
 * @author Rustam Vishnyakov
 */
public class DefaultLanguageColorsPage implements RainbowColorSettingsPage, DisplayPrioritySortable {
  @NonNls private static final Map<String, TextAttributesKey> TAG_HIGHLIGHTING_MAP = RainbowHighlighter.createRainbowHLM();

  private final static TextAttributesKey FAKE_BAD_CHAR =
    TextAttributesKey.createTextAttributesKey("FAKE_BAD_CHAR", HighlighterColors.BAD_CHARACTER);

  static {
    TAG_HIGHLIGHTING_MAP.put("bad_char", FAKE_BAD_CHAR);
    TAG_HIGHLIGHTING_MAP.put("template_language", DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR);
    TAG_HIGHLIGHTING_MAP.put("identifier", DefaultLanguageHighlighterColors.IDENTIFIER);
    TAG_HIGHLIGHTING_MAP.put("number", DefaultLanguageHighlighterColors.NUMBER);
    TAG_HIGHLIGHTING_MAP.put("keyword", DefaultLanguageHighlighterColors.KEYWORD);
    TAG_HIGHLIGHTING_MAP.put("string", DefaultLanguageHighlighterColors.STRING);
    TAG_HIGHLIGHTING_MAP.put("line_comment", DefaultLanguageHighlighterColors.LINE_COMMENT);
    TAG_HIGHLIGHTING_MAP.put("block_comment", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    TAG_HIGHLIGHTING_MAP.put("operation_sign", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    TAG_HIGHLIGHTING_MAP.put("braces", DefaultLanguageHighlighterColors.BRACES);
    TAG_HIGHLIGHTING_MAP.put("doc_comment", DefaultLanguageHighlighterColors.DOC_COMMENT);
    TAG_HIGHLIGHTING_MAP.put("dot", DefaultLanguageHighlighterColors.DOT);
    TAG_HIGHLIGHTING_MAP.put("semicolon", DefaultLanguageHighlighterColors.SEMICOLON);
    TAG_HIGHLIGHTING_MAP.put("comma", DefaultLanguageHighlighterColors.COMMA);
    TAG_HIGHLIGHTING_MAP.put("brackets", DefaultLanguageHighlighterColors.BRACKETS);
    TAG_HIGHLIGHTING_MAP.put("parenths", DefaultLanguageHighlighterColors.PARENTHESES);
    TAG_HIGHLIGHTING_MAP.put("func_decl", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
    TAG_HIGHLIGHTING_MAP.put("func_call", DefaultLanguageHighlighterColors.FUNCTION_CALL);
    TAG_HIGHLIGHTING_MAP.put("param", DefaultLanguageHighlighterColors.PARAMETER);
    TAG_HIGHLIGHTING_MAP.put("class_name", DefaultLanguageHighlighterColors.CLASS_NAME);
    TAG_HIGHLIGHTING_MAP.put("class_ref", DefaultLanguageHighlighterColors.CLASS_REFERENCE);
    TAG_HIGHLIGHTING_MAP.put("inst_method", DefaultLanguageHighlighterColors.INSTANCE_METHOD);
    TAG_HIGHLIGHTING_MAP.put("inst_field", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    TAG_HIGHLIGHTING_MAP.put("static_method", DefaultLanguageHighlighterColors.STATIC_METHOD);
    TAG_HIGHLIGHTING_MAP.put("static_field", DefaultLanguageHighlighterColors.STATIC_FIELD);
    TAG_HIGHLIGHTING_MAP.put("label", DefaultLanguageHighlighterColors.LABEL);
    TAG_HIGHLIGHTING_MAP.put("local_var", DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    TAG_HIGHLIGHTING_MAP.put("global_var", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE);
    TAG_HIGHLIGHTING_MAP.put("const", DefaultLanguageHighlighterColors.CONSTANT);
    TAG_HIGHLIGHTING_MAP.put("interface", DefaultLanguageHighlighterColors.INTERFACE_NAME);
    TAG_HIGHLIGHTING_MAP.put("doc_markup", DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP);
    TAG_HIGHLIGHTING_MAP.put("doc_tag", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG);
    TAG_HIGHLIGHTING_MAP.put("doc_tag_value", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE);
    TAG_HIGHLIGHTING_MAP.put("valid_esc_seq", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
    TAG_HIGHLIGHTING_MAP.put("invalid_esc_seq", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
    TAG_HIGHLIGHTING_MAP.put("predefined", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    TAG_HIGHLIGHTING_MAP.put("metadata", DefaultLanguageHighlighterColors.METADATA);
    TAG_HIGHLIGHTING_MAP.put("tag", DefaultLanguageHighlighterColors.MARKUP_TAG);
    TAG_HIGHLIGHTING_MAP.put("attribute", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE);
    TAG_HIGHLIGHTING_MAP.put("entity", DefaultLanguageHighlighterColors.MARKUP_ENTITY);
    TAG_HIGHLIGHTING_MAP.put("reassigned_parameter", DefaultLanguageHighlighterColors.REASSIGNED_PARAMETER);
    TAG_HIGHLIGHTING_MAP.put("reassigned_local", DefaultLanguageHighlighterColors.REASSIGNED_LOCAL_VARIABLE);
  }

  @NonNls private static final Map<String, TextAttributesKey> INLINE_ELEMENTS = new HashMap<>();

  static {
    INLINE_ELEMENTS.put("parameter_hint", DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT);
    INLINE_ELEMENTS.put("parameter_hint_highlighted", DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_HIGHLIGHTED);
    INLINE_ELEMENTS.put("parameter_hint_current", DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_CURRENT);
  }

  private final static AttributesDescriptor[] ATTRIBUTES_DESCRIPTORS = {
    new AttributesDescriptor(
      OptionsBundle.message("options.java.attribute.descriptor.bad.character"), HighlighterColors.BAD_CHARACTER),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.keyword"), DefaultLanguageHighlighterColors.KEYWORD),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.identifier"), DefaultLanguageHighlighterColors.IDENTIFIER),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.string"), DefaultLanguageHighlighterColors.STRING),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.valid.esc.seq"), DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.invalid.esc.seq"), DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.number"), DefaultLanguageHighlighterColors.NUMBER),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.operation"), DefaultLanguageHighlighterColors.OPERATION_SIGN),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.braces"), DefaultLanguageHighlighterColors.BRACES),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.parentheses"), DefaultLanguageHighlighterColors.PARENTHESES),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.brackets"), DefaultLanguageHighlighterColors.BRACKETS),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.dot"), DefaultLanguageHighlighterColors.DOT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.comma"), DefaultLanguageHighlighterColors.COMMA),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.semicolon"), DefaultLanguageHighlighterColors.SEMICOLON),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.line.comment"), DefaultLanguageHighlighterColors.LINE_COMMENT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.block.comment"), DefaultLanguageHighlighterColors.BLOCK_COMMENT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.doc.comment"), DefaultLanguageHighlighterColors.DOC_COMMENT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.doc.markup"), DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.doc.tag"), DefaultLanguageHighlighterColors.DOC_COMMENT_TAG),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.doc.tag.value"), DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.label"), DefaultLanguageHighlighterColors.LABEL),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.constant"), DefaultLanguageHighlighterColors.CONSTANT),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.predefined"), DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.local.variable"), DefaultLanguageHighlighterColors.LOCAL_VARIABLE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.reassigned.local.variable"), DefaultLanguageHighlighterColors.REASSIGNED_LOCAL_VARIABLE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.global.variable"), DefaultLanguageHighlighterColors.GLOBAL_VARIABLE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.function.declaration"), DefaultLanguageHighlighterColors.FUNCTION_DECLARATION),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.function.call"), DefaultLanguageHighlighterColors.FUNCTION_CALL),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.parameter"), DefaultLanguageHighlighterColors.PARAMETER),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.reassigned.parameter"), DefaultLanguageHighlighterColors.REASSIGNED_PARAMETER),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.interface.name"), DefaultLanguageHighlighterColors.INTERFACE_NAME),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.metadata"), DefaultLanguageHighlighterColors.METADATA),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.class.name"), DefaultLanguageHighlighterColors.CLASS_NAME),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.class.reference"), DefaultLanguageHighlighterColors.CLASS_REFERENCE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.instance.method"), DefaultLanguageHighlighterColors.INSTANCE_METHOD),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.instance.field"), DefaultLanguageHighlighterColors.INSTANCE_FIELD),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.static.method"), DefaultLanguageHighlighterColors.STATIC_METHOD),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.static.field"), DefaultLanguageHighlighterColors.STATIC_FIELD),

    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.markup.tag"), DefaultLanguageHighlighterColors.MARKUP_TAG),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.markup.attribute"), DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.markup.entity"), DefaultLanguageHighlighterColors.MARKUP_ENTITY),
    new AttributesDescriptor(
      OptionsBundle.message("options.language.defaults.template.language"), DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR),

    new AttributesDescriptor(
      OptionsBundle.message("options.java.attribute.descriptor.inline.parameter.hint"), 
      DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT),
    new AttributesDescriptor(
      OptionsBundle.message("options.java.attribute.descriptor.inline.parameter.hint.highlighted"), 
      DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_HIGHLIGHTED),
    new AttributesDescriptor(
      OptionsBundle.message("options.java.attribute.descriptor.inline.parameter.hint.current"), 
      DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_CURRENT)
  };

  @Nullable
  @Override
  public Icon getIcon() {
    return FileTypes.PLAIN_TEXT.getIcon();
  }

  @NotNull
  @Override
  public SyntaxHighlighter getHighlighter() {
    return new PlainSyntaxHighlighter();
  }

  @NotNull
  @Override
  public String getDemoText() {
    return
      "Bad characters: <bad_char>????</bad_char>\n" +
      "<keyword>Keyword</keyword>\n" +
      "<identifier>Identifier</identifier>\n" +
      "<string>'String <valid_esc_seq>\\n</valid_esc_seq><invalid_esc_seq>\\?</invalid_esc_seq>'</string>\n" +
      "<number>12345</number>\n" +
      "<operation_sign>Operator</operation_sign>\n" +
      "Dot: <dot>.</dot> comma: <comma>,</comma> semicolon: <semicolon>;</semicolon>\n" +
      "<braces>{</braces> Braces <braces>}</braces>\n" +
      "<parenths>(</parenths> Parentheses <parenths>)</parenths>\n" +
      "<brackets>[</brackets> Brackets <brackets>]</brackets>\n" +
      "<line_comment>// Line comment</line_comment>\n" +
      "<block_comment>/* Block comment */</block_comment>\n" +
      "<label>:Label</label>\n" +
      "<predefined>predefined_symbol()</predefined>\n" +
      "<const>CONSTANT</const>\n" +
      "Global <global_var>variable</global_var>\n" +
      "<doc_comment>/** \n" +
      " * Doc comment\n" +
      " * <doc_tag>@tag</doc_tag> <doc_markup><code></doc_markup>Markup<<doc_markup></code></doc_markup>" +
      RainbowHighlighter.generatePaletteExample("\n * ") + "\n" +
      " * <doc_tag>@param</doc_tag> <doc_tag_value>parameter1</doc_tag_value> documentation\n" +
      " * <doc_tag>@param</doc_tag> <doc_tag_value>parameter2</doc_tag_value> documentation\n" +
      " * <doc_tag>@param</doc_tag> <doc_tag_value>parameter3</doc_tag_value> documentation\n" +
      " * <doc_tag>@param</doc_tag> <doc_tag_value>parameter4</doc_tag_value> documentation\n" +
      " */</doc_comment>\n" +
      "Function <func_decl>declaration</func_decl> (<param>parameter1</param> <param>parameter2</param> <param>parameter3</param> <param>parameter4</param>)\n" +
      "    Local <local_var>variable1</local_var> <local_var>variable2</local_var> <local_var>variable3</local_var> <local_var>variable4</local_var>\n" +
      "    Reassigned local <reassigned_local>variable</reassigned_local>\n" +
      "    Reassigned <reassigned_parameter>parameter</reassigned_parameter>\n" +
      "Function <func_call>call</func_call>(" +
      "<parameter_hint p:>0, <parameter_hint param:>1, <parameter_hint parameterName:>2" +
      ")\n" +
      "Current function <func_call>call</func_call>(<parameter_hint_highlighted param:>0, <parameter_hint_current currentParam:>1)\n" +
      "Interface <interface>Name</interface>\n" +
      "<metadata>@Metadata</metadata>\n" +
      "Class <class_name>Name</class_name>\n" +
      "    instance <inst_method>method</inst_method>\n" +
      "    instance <inst_field>field</inst_field>\n" +
      "    static <static_method>method</static_method>\n" +
      "    static <static_field>field</static_field>\n" +
      "\n" +
      "<tag><keyword>@TAG</keyword> <attribute>attribute</attribute>=<string>Value</string></tag>\n" +
      "    Entity: <entity>&amp;</entity>\n" +
      "    <template_language>{% Template language %}</template_language>";
  }

  @Nullable
  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return TAG_HIGHLIGHTING_MAP;
  }

  @Nullable
  @Override
  public Map<String, TextAttributesKey> getAdditionalInlineElementToDescriptorMap() {
    return INLINE_ELEMENTS;
  }

  @NotNull
  @Override
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRIBUTES_DESCRIPTORS;
  }

  @NotNull
  @Override
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return OptionsBundle.message("options.language.defaults.display.name");
  }

  @Override
  public DisplayPriority getPriority() {
    return DisplayPriority.GENERAL_SETTINGS;
  }

  @Override
  public boolean isRainbowType(TextAttributesKey type) {
    return DefaultLanguageHighlighterColors.LOCAL_VARIABLE.equals(type)
           || DefaultLanguageHighlighterColors.PARAMETER.equals(type)
           || DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE.equals(type);
  }

  @Nullable
  @Override
  public Language getLanguage() {
    return null;
  }
}
