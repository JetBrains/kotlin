// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.execution.cmd;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("AccessStaticViaInstance")
public class GradleCommandLineOptionsProvider {

  private static final Options ourOptions;

  static {
    Options options = new Options();
    // Debugging options, see https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_debugging
    options.addOption(
      OptionBuilder.withLongOpt("full-stacktrace").withDescription("Print out the full (very verbose) stacktrace for any exceptions.")
        .create('S'));
    options.addOption(
      OptionBuilder.withLongOpt("stacktrace").withDescription("Print out the stacktrace also for user exceptions (e.g. compile error).")
        .create('s'));
    options.addOption(
      OptionBuilder.withLongOpt("scan")
        .withDescription("Create a build scan with fine-grained information about all aspects of your Gradle build.")
        .create());

    // Performance options, see https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_performance
    options.addOption(
      OptionBuilder.withLongOpt("build-cache")
        .withDescription("Toggles the Gradle build cache. Gradle will try to reuse outputs from previous builds. Default is off.")
        .create());
    options.addOption(
      OptionBuilder.withLongOpt("no-build-cache")
        .withDescription("Toggles the Gradle build cache. Gradle will try to reuse outputs from previous builds. Default is off.")
        .create());
    options.addOption(OptionBuilder.withLongOpt("configure-on-demand").withDescription(
      "Toggles Configure-on-demand. Only relevant projects are configured in this build run. Default is off.").create());
    options.addOption(OptionBuilder.withLongOpt("no-configure-on-demand").withDescription(
      "Toggles Configure-on-demand. Only relevant projects are configured in this build run. Default is off.").create());
    options.addOption(OptionBuilder.withLongOpt("max-workers").withDescription(
      "Sets maximum number of workers that Gradle may use. Default is number of processors.").create());
    options.addOption(OptionBuilder.withLongOpt("parallel").withDescription(
      "Build projects in parallel.").create());
    options.addOption(OptionBuilder.withLongOpt("no-parallel").withDescription("Switch off parallel build.").create());
    options.addOption(OptionBuilder.withLongOpt("priority").withDescription(
      "Specifies the scheduling priority for the Gradle daemon and all processes launched by it. Values are normal or low. Default is normal.")
                        .create());
    options.addOption(OptionBuilder.withLongOpt("profile")
                        .withDescription("Profiles build execution time and generates a report in the buildDir/reports/profile directory.")
                        .create());

    // Logging options, https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_logging
    options.addOption(OptionBuilder.withLongOpt("quiet").withDescription("Log errors only.").create('q'));
    options.addOption(OptionBuilder.withLongOpt("warn").withDescription("Set log level to warn.").create('w'));
    options.addOption(OptionBuilder.withLongOpt("info").withDescription("Set log level to info. ").create('i'));
    options.addOption(OptionBuilder.withLongOpt("debug").withDescription("Log in debug mode (includes normal stacktrace).").create('d'));
    options.addOption(OptionBuilder.withLongOpt("warning-mode")
                        .hasArg()
                        .withDescription("Specifies how to log warnings. Default is summary. Available modes (all,fail,none,summary)")
                        .create());

    // Execution options, https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_execution_options
    options.addOption(
      OptionBuilder.withLongOpt("include-build").withDescription("Run the build as a composite, including the specified build.").create());
    options.addOption(
      OptionBuilder.withLongOpt("offline").withDescription("Specifies that the build should operate without accessing network resources.")
        .create());
    options.addOption(OptionBuilder.withLongOpt("refresh-dependencies").withDescription("Refresh the state of dependencies.").create());
    options.addOption(OptionBuilder.withLongOpt("dry-run").withDescription("Runs the build with all task actions disabled. ").create());
    options.addOption(OptionBuilder.withLongOpt("write-locks").withDescription(
      "Indicates that all resolved configurations that are lockable should have their lock state persisted.").create());
    options.addOption(
      OptionBuilder.withLongOpt("update-locks")
        .withDescription(
          "Indicates that versions for the specified modules have to be updated in the lock file. This flag also implies --write-locks. ")
        .hasArg()
        .create());
    options.addOption(OptionBuilder.withLongOpt("no-rebuild").withDescription("Do not rebuild project dependencies.").create());

    // Environment options, https://docs.gradle.org/current/userguide/command_line_interface.html#environment_options
    options.addOption(OptionBuilder.withLongOpt("build-file").withDescription("Specifies the build file.").hasArg().create('b'));
    options.addOption(OptionBuilder.withLongOpt("settings-file").withDescription("Specifies the settings file.").hasArg().create('c'));
    options.addOption(OptionBuilder.withLongOpt("gradle-user-home").withDescription(
      "Specifies the Gradle user home directory. The default is the .gradle directory in the user's home directory.").hasArg().create('g'));
    options.addOption(OptionBuilder
                        .withLongOpt("project-dir")
                        .withDescription("Specifies the start directory for Gradle. Defaults to current directory.")
                        .hasArg().create('p'));
    options.addOption(OptionBuilder.withLongOpt("project-cache-dir").withDescription(
      "Specifies the project-specific cache directory. Default value is .gradle in the root project directory.").hasArg().create());
    options.addOption(
      OptionBuilder.withLongOpt("system-prop").withDescription("Sets a system property of the JVM, for example -Dmyprop=myvalue.")
        .hasArg().create('D'));
    options
      .addOption(OptionBuilder.withLongOpt("init-script").withDescription("Specifies an initialization script. ").hasArg().create('I'));
    options.addOption(OptionBuilder.withLongOpt("project-prop")
                        .withDescription("Sets a project property of the root project, for example -Pmyprop=myvalue.").hasArgs()
                        .create('P'));

    // Executing tasks, https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_executing_tasks
    options.addOption(OptionBuilder
                        .withLongOpt("exclude-task")
                        .withDescription("Specifies a task to be excluded from execution.")
                        .hasArgs()
                        .create('x'));
    options.addOption(OptionBuilder
                        .withLongOpt("rerun-tasks")
                        .withDescription("Specifies that any task optimization is ignored.")
                        .create());
    options.addOption(OptionBuilder
                        .withLongOpt("continue")
                        .withDescription("Continues task execution after a task failure.")
                        .create());

    // Do not uncomment the following options. These options does not supported via tooling API,
    // https://github.com/gradle/gradle/blob/v6.2.0/subprojects/tooling-api/src/main/java/org/gradle/tooling/LongRunningOperation.java#L149-L154
    //
    // options.addOption(OptionBuilder.withLongOpt("help").withDescription("Shows a help message.").create('h'));
    // options.addOption(OptionBuilder.withLongOpt("version").withDescription("Prints version info.").create('v'));

    ourOptions = options;
  }

  public static Options getSupportedOptions() {
    return ourOptions;
  }
}
