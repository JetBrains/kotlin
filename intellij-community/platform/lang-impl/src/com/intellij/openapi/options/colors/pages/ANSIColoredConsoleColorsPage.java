package com.intellij.openapi.options.colors.pages;

import com.intellij.execution.process.ConsoleHighlighter;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.psi.codeStyle.DisplayPriority;
import com.intellij.psi.codeStyle.DisplayPrioritySortable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author oleg, Roman.Chernyatchik
 */
public class ANSIColoredConsoleColorsPage implements ColorSettingsPage, DisplayPrioritySortable {

  private static final String DEMO_TEXT =
    "<stdsys>C:\\command.com</stdsys>\n" +
    "-<stdout> C:></stdout>\n" +
    "-<stdin> help</stdin>\n" +
    "<stderr>Bad command or file name</stderr>\n" +
    "\n" +
    "<logError>Log error</logError>\n" +
    "<logWarning>Log warning</logWarning>\n" +
    "<logInfo>Log info</logInfo>\n" +
    "<logVerbose>Log verbose</logVerbose>\n" +
    "<logDebug>Log debug</logDebug>\n" +
    "<logExpired>An expired log entry</logExpired>\n" +
    "\n" +
    "# Process output highlighted using ANSI colors codes\n" +
    "<black>ANSI: black</black>\n" +
    "<red>ANSI: red</red>\n" +
    "<green>ANSI: green</green>\n" +
    "<yellow>ANSI: yellow</yellow>\n" +
    "<blue>ANSI: blue</blue>\n" +
    "<magenta>ANSI: magenta</magenta>\n" +
    "<cyan>ANSI: cyan</cyan>\n" +
    "<gray>ANSI: gray</gray>\n" +
    "<darkGray>ANSI: dark gray</darkGray>\n" +
    "<redBright>ANSI: bright red</redBright>\n" +
    "<greenBright>ANSI: bright green</greenBright>\n" +
    "<yellowBright>ANSI: bright yellow</yellowBright>\n" +
    "<blueBright>ANSI: bright blue</blueBright>\n" +
    "<magentaBright>ANSI: bright magenta</magentaBright>\n" +
    "<cyanBright>ANSI: bright cyan</cyanBright>\n" +
    "<white>ANSI: white</white>\n" +
    "\n" +
    "<stdsys>Process finished with exit code 1</stdsys>\n";

  private static final AttributesDescriptor[] ATTRS = {
    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.stdout"), ConsoleViewContentType.NORMAL_OUTPUT_KEY),
    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.stderr"), ConsoleViewContentType.ERROR_OUTPUT_KEY),
    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.stdin"), ConsoleViewContentType.USER_INPUT_KEY),
    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.system.output"), ConsoleViewContentType.SYSTEM_OUTPUT_KEY),

    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.logError"), ConsoleViewContentType.LOG_ERROR_OUTPUT_KEY),
    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.warning"), ConsoleViewContentType.LOG_WARNING_OUTPUT_KEY),
    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.info"), ConsoleViewContentType.LOG_INFO_OUTPUT_KEY),
    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.verbose"), ConsoleViewContentType.LOG_VERBOSE_OUTPUT_KEY),
    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.debug"), ConsoleViewContentType.LOG_DEBUG_OUTPUT_KEY),
    new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.console.expired"), ConsoleViewContentType.LOG_EXPIRED_ENTRY),

    new AttributesDescriptor(OptionsBundle.message("color.settings.console.black"), ConsoleHighlighter.BLACK),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.red"), ConsoleHighlighter.RED),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.green"), ConsoleHighlighter.GREEN),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.yellow"), ConsoleHighlighter.YELLOW),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.blue"), ConsoleHighlighter.BLUE),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.magenta"), ConsoleHighlighter.MAGENTA),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.cyan"), ConsoleHighlighter.CYAN),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.gray"), ConsoleHighlighter.GRAY),

    new AttributesDescriptor(OptionsBundle.message("color.settings.console.darkGray"), ConsoleHighlighter.DARKGRAY),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.redBright"), ConsoleHighlighter.RED_BRIGHT),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.greenBright"), ConsoleHighlighter.GREEN_BRIGHT),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.yellowBright"), ConsoleHighlighter.YELLOW_BRIGHT),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.blueBright"), ConsoleHighlighter.BLUE_BRIGHT),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.magentaBright"), ConsoleHighlighter.MAGENTA_BRIGHT),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.cyanBright"), ConsoleHighlighter.CYAN_BRIGHT),
    new AttributesDescriptor(OptionsBundle.message("color.settings.console.white"), ConsoleHighlighter.WHITE),
  };

  private static final Map<String, TextAttributesKey> ADDITIONAL_HIGHLIGHT_DESCRIPTORS = new HashMap<>();
  static{
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("stdsys", ConsoleViewContentType.SYSTEM_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("stdout", ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("stdin", ConsoleViewContentType.USER_INPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("stderr", ConsoleViewContentType.ERROR_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("logError", ConsoleViewContentType.LOG_ERROR_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("logWarning", ConsoleViewContentType.LOG_WARNING_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("logInfo", ConsoleViewContentType.LOG_INFO_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("logVerbose", ConsoleViewContentType.LOG_VERBOSE_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("logDebug", ConsoleViewContentType.LOG_DEBUG_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("logExpired", ConsoleViewContentType.LOG_EXPIRED_ENTRY);

    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("black", ConsoleHighlighter.BLACK);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("red", ConsoleHighlighter.RED);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("green", ConsoleHighlighter.GREEN);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("yellow", ConsoleHighlighter.YELLOW);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("blue", ConsoleHighlighter.BLUE);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("magenta", ConsoleHighlighter.MAGENTA);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("cyan", ConsoleHighlighter.CYAN);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("gray", ConsoleHighlighter.GRAY);

    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("darkGray", ConsoleHighlighter.DARKGRAY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("redBright", ConsoleHighlighter.RED_BRIGHT);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("greenBright", ConsoleHighlighter.GREEN_BRIGHT);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("yellowBright", ConsoleHighlighter.YELLOW_BRIGHT);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("blueBright", ConsoleHighlighter.BLUE_BRIGHT);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("magentaBright", ConsoleHighlighter.MAGENTA_BRIGHT);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("cyanBright", ConsoleHighlighter.CYAN_BRIGHT);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("white", ConsoleHighlighter.WHITE);
  }

  private static final ColorDescriptor[] COLORS = {
    new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.console.background"), ConsoleViewContentType.CONSOLE_BACKGROUND_KEY, ColorDescriptor.Kind.BACKGROUND),
  };

  @Override
  @Nullable
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return ADDITIONAL_HIGHLIGHT_DESCRIPTORS;
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return OptionsBundle.message("color.settings.console.name");
  }

  @Override
  @NotNull
  public Icon getIcon() {
    return FileTypes.PLAIN_TEXT.getIcon();
  }

  @Override
  @NotNull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRS;
  }

  @Override
  @NotNull
  public ColorDescriptor[] getColorDescriptors() {
    return COLORS;
  }

  @Override
  @NotNull
  public SyntaxHighlighter getHighlighter() {
     return new PlainSyntaxHighlighter();
  }

  @Override
  @NotNull
  public String getDemoText() {
    return DEMO_TEXT;
  }

  @Override
  public DisplayPriority getPriority() {
    return DisplayPriority.COMMON_SETTINGS;
  }
}
