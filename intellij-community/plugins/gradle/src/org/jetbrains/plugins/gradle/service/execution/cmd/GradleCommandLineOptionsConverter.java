// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.execution.cmd;

import com.intellij.util.ArrayUtilRt;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.apache.commons.cli.Option;
import org.gradle.cli.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class GradleCommandLineOptionsConverter extends AbstractCommandLineConverter<Map<String, List<String>>> {

  private final SystemPropertiesCommandLineConverter systemPropertiesCommandLineConverter = new SystemPropertiesCommandLineConverter();

  @Override
  public Map<String, List<String>> convert(ParsedCommandLine options, Map<String, List<String>> target)
    throws CommandLineArgumentException {
    //noinspection unchecked
    final Collection<Option> supportedOptions = GradleCommandLineOptionsProvider.getSupportedOptions().getOptions();
    for (Option supportedOption : supportedOptions) {
      final String longOpt = supportedOption.getLongOpt();
      if (longOpt != null && options.hasOption(longOpt)) {
        final ParsedCommandLineOption option = options.option(longOpt);
        target.put(longOpt, option.getValues());
      }
      else {
        final String opt = supportedOption.getOpt();
        if (opt != null && options.hasOption(opt)) {
          final ParsedCommandLineOption option = options.option(opt);
          target.put(opt, option.getValues());
        }
      }
    }

    return target;
  }

  @Override
  public void configure(CommandLineParser parser) {
    systemPropertiesCommandLineConverter.configure(parser);
    parser.allowMixedSubcommandsAndOptions();
    parser.allowUnknownOptions();

    //noinspection unchecked
    final Collection<Option> supportedOptions = GradleCommandLineOptionsProvider.getSupportedOptions().getOptions();
    for (Option supportedOption : supportedOptions) {


      final List<String> objects = new SmartList<>();
      ContainerUtil.addAllNotNull(objects, supportedOption.getOpt(), supportedOption.getLongOpt());

      if (!objects.isEmpty()) {
        try {
          final CommandLineOption option = parser.option(ArrayUtilRt.toStringArray(objects));
          if (supportedOption.hasArg()) {
            option.hasArgument();
          }

          if (supportedOption.hasArgs()) {
            option.hasArguments();
          }
          option.hasDescription(supportedOption.getDescription());
        }
        catch (IllegalArgumentException ignore) {
        }
      }
    }
  }
}
