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

import groovyjarjarcommonscli.OptionBuilder;
import groovyjarjarcommonscli.Options;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("AccessStaticViaInstance")
public class GradleCommandLineOptionsProvider {

  private static final Options ourOptions;

  static {
    Options options = new Options();
    options.addOption(OptionBuilder.withLongOpt("no-rebuild").withDescription("Do not rebuild project dependencies.").create('a'));
    options.addOption(OptionBuilder.withLongOpt("settings-file").withDescription("Specifies the settings file.").hasArg().create('c'));
    options.addOption(OptionBuilder.withLongOpt("continue").withDescription("Continues task execution after a task failure.").create());
    options.addOption(OptionBuilder.withLongOpt("configure-on-demand").withDescription(
      "Only relevant projects are configured in this build run. This means faster builds for large multi-projects.").create());
    options.addOption(
      OptionBuilder.withLongOpt("system-prop").withDescription("Sets a system property of the JVM, for example -Dmyprop=myvalue.")
        .hasArg().create('D'));
    options.addOption(OptionBuilder.withLongOpt("debug").withDescription("Log in debug mode (includes normal stacktrace).").create('d'));
    options.addOption(OptionBuilder.withLongOpt("gradle-user-home").withDescription(
      "Specifies the Gradle user home directory. The default is the .gradle directory in the user's home directory.").hasArg().create('g'));
    options
      .addOption(OptionBuilder.withLongOpt("init-script").withDescription("Specifies an initialization script. ").hasArg().create('I'));
    options.addOption(OptionBuilder.withLongOpt("info").withDescription("Set log level to info. ").create('i'));
    options.addOption(OptionBuilder.withLongOpt("dry-run").withDescription("Runs the build with all task actions disabled. ").create('m'));
    options.addOption(
      OptionBuilder.withLongOpt("offline").withDescription("Specifies that the build should operate without accessing network resources.")
        .create());
    options.addOption(OptionBuilder.withLongOpt("project-prop")
                        .withDescription("Sets a project property of the root project, for example -Pmyprop=myvalue.").hasArgs()
                        .create('P'));
    options.addOption(
      OptionBuilder.withLongOpt("project-dir").withDescription("Specifies the start directory for Gradle. Defaults to current directory.")
        .hasArg().create('p'));
    options.addOption(OptionBuilder.withLongOpt("parallel").withDescription(
      "Build projects in parallel. Gradle will attempt to determine the optimal number of executor threads to use. This option should only be used with decoupled projects")
                        .create());
    options.addOption(OptionBuilder.withLongOpt("parallel-threads").withDescription(
      "Build projects in parallel, using the specified number of executor threads. For example--parallel-threads=3. This option should only be used with decoupled projects")
                        .hasArg().create());
    options.addOption(OptionBuilder.withLongOpt("profile")
                        .withDescription("Profiles build execution time and generates a report in the buildDir/reports/profile directory.")
                        .create());
    options.addOption(OptionBuilder.withLongOpt("project-cache-dir").withDescription(
      "Specifies the project-specific cache directory. Default value is .gradle in the root project directory.").hasArg().create());
    options.addOption(OptionBuilder.withLongOpt("quiet").withDescription("Log errors only.").create('q'));
    options.addOption(OptionBuilder.withLongOpt("refresh-dependencies").withDescription("Refresh the state of dependencies.").create());
    options
      .addOption(OptionBuilder.withLongOpt("rerun-tasks").withDescription("Specifies that any task optimization is ignored.").create());
    options.addOption(
      OptionBuilder.withLongOpt("full-stacktrace").withDescription("Print out the full (very verbose) stacktrace for any exceptions.")
        .create('S'));
    options.addOption(
      OptionBuilder.withLongOpt("stacktrace").withDescription("Print out the stacktrace also for user exceptions (e.g. compile error).")
        .create('s'));
    options.addOption(
      OptionBuilder.withLongOpt("no-search-upwards").withDescription("Don't search in parent directories for a settings.gradle file.")
        .create('u'));
    options
      .addOption(
        OptionBuilder.withLongOpt("exclude-task").withDescription("Specifies a task to be excluded from execution.").hasArgs().create('x'));
    options.addOption(OptionBuilder.withLongOpt("args").withDescription("Command line arguments passed to the main class.").hasArg().create());
    // Do not uncomment the following options. These options does not supported via tooling API.
    //options.addOption(OptionBuilder.withLongOpt("build-file").withDescription("Specifies the build file.").hasArg().create('b'));
    //options.addOption(OptionBuilder.withLongOpt("help").withDescription("Shows a help message.").create('h'));
    //options.addOption(OptionBuilder.withLongOpt("version").withDescription("Prints version info.").create('v'));
    //options.addOption(OptionBuilder.withLongOpt("all").withDescription("Shows additional detail in the task listing. See Section 11.6.2, "Listing tasks".").create());

    ourOptions = options;
  }

  public static Options getSupportedOptions() {
    return ourOptions;
  }
}
