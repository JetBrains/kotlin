package com.intellij.codeInspection;

/**
 * @author Roman.Chernyatchik
 */
public interface InspectionToolCmdlineOptions extends InspectionToolCmdlineOptionHelpProvider {
  /**
   * @param app Inspection Application
   * @return  true if was successfully initialized
   */
  void initApplication(InspectionApplication app);

  /**
   * @return 0 if turned off
   */
  int getVerboseLevelProperty();

  /**
   * @return If true help message wont be outputted
   */
  boolean suppressHelp();

  void validate() throws CmdlineArgsValidationException;

  /**
   * Application components have been already initialized at this moment.
   * E.g. you can save smth in application component or service
   */
  void beforeStartup();

  class CmdlineArgsValidationException extends Exception {
    public CmdlineArgsValidationException(String message) {
      super(message);
    }
  }
}
