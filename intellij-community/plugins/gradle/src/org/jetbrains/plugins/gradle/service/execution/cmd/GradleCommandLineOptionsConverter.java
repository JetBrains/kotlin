/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.service.execution.cmd;

import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import groovyjarjarcommonscli.Option;
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

    //noinspection unchecked
    final Collection<Option> supportedOptions = GradleCommandLineOptionsProvider.getSupportedOptions().getOptions();
    for (Option supportedOption : supportedOptions) {


      final List<String> objects = ContainerUtil.newSmartList();
      ContainerUtil.addAllNotNull(objects, supportedOption.getOpt(), supportedOption.getLongOpt());

      if (!objects.isEmpty()) {
        try {
          final CommandLineOption option = parser.option(ArrayUtil.toStringArray(objects));
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
