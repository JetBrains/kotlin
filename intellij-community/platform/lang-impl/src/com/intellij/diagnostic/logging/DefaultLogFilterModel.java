/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.diagnostic.logging;

import com.intellij.diagnostic.DiagnosticBundle;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class DefaultLogFilterModel extends LogFilterModel {
  private final Project myProject;
  private boolean myCheckStandardFilters = true;
  private String myPrevType = null;

  public DefaultLogFilterModel(Project project) {
    myProject = project;
  }

  protected LogConsolePreferences getPreferences() {
    return LogConsolePreferences.getInstance(myProject);
  }

  public boolean isCheckStandartFilters() {
    return myCheckStandardFilters;
  }

  public void setCheckStandartFilters(boolean checkStandardFilters) {
    myCheckStandardFilters = checkStandardFilters;
  }

  @Override
  public void updateCustomFilter(String filter) {
    super.updateCustomFilter(filter);
    getPreferences().updateCustomFilter(filter);
  }

  @Override
  public String getCustomFilter() {
    return getPreferences().CUSTOM_FILTER;
  }

  @Override
  public void addFilterListener(LogFilterListener listener) {
    getPreferences().addFilterListener(listener);
  }

  @Override
  public boolean isApplicable(String line) {
    if (!super.isApplicable(line)) return false;
    return getPreferences().isApplicable(line, myPrevType, myCheckStandardFilters);
  }

  @Override
  public void removeFilterListener(LogFilterListener listener) {
    getPreferences().removeFilterListener(listener);
  }

  @Override
  public List<LogFilter> getLogFilters() {
    LogConsolePreferences preferences = getPreferences();
    final ArrayList<LogFilter> filters = new ArrayList<>();
    if (myCheckStandardFilters) {
      addStandardFilters(filters, preferences);
    }
    filters.addAll(preferences.getRegisteredLogFilters());
    return filters;
  }

  private void addStandardFilters(ArrayList<? super LogFilter> filters, final LogConsolePreferences preferences) {
    filters.add(new MyFilter(DiagnosticBundle.message("log.console.filter.show.all"), preferences) {
      @Override
      public void selectFilter() {
        preferences.FILTER_ERRORS = false;
        preferences.FILTER_INFO = false;
        preferences.FILTER_WARNINGS = false;
        preferences.FILTER_DEBUG = false;
      }

      @Override
      public boolean isSelected() {
        return !preferences.FILTER_ERRORS && !preferences.FILTER_INFO && !preferences.FILTER_WARNINGS && !preferences.FILTER_DEBUG;
      }
    });
    filters.add(new MyFilter(DiagnosticBundle.message("log.console.filter.show.errors.warnings.and.infos"), preferences) {
      @Override
      public void selectFilter() {
        preferences.FILTER_ERRORS = false;
        preferences.FILTER_INFO = false;
        preferences.FILTER_WARNINGS = false;
        preferences.FILTER_DEBUG = true;
      }

      @Override
      public boolean isSelected() {
        return !preferences.FILTER_ERRORS && !preferences.FILTER_INFO && !preferences.FILTER_WARNINGS && preferences.FILTER_DEBUG;
      }
    });
    filters.add(new MyFilter(DiagnosticBundle.message("log.console.filter.show.errors.and.warnings"), preferences) {
      @Override
      public void selectFilter() {
        preferences.FILTER_ERRORS = false;
        preferences.FILTER_INFO = true;
        preferences.FILTER_WARNINGS = false;
        preferences.FILTER_DEBUG = true;
      }

      @Override
      public boolean isSelected() {
        return !preferences.FILTER_ERRORS && preferences.FILTER_INFO && !preferences.FILTER_WARNINGS && preferences.FILTER_DEBUG;
      }
    });
    filters.add(new MyFilter(DiagnosticBundle.message("log.console.filter.show.errors"), preferences) {
      @Override
      public void selectFilter() {
        preferences.FILTER_ERRORS = false;
        preferences.FILTER_INFO = true;
        preferences.FILTER_WARNINGS = true;
        preferences.FILTER_DEBUG = true;
      }

      @Override
      public boolean isSelected() {
        return !preferences.FILTER_ERRORS && preferences.FILTER_INFO && preferences.FILTER_WARNINGS && preferences.FILTER_DEBUG;
      }
    });
  }

  @Override
  public boolean isFilterSelected(LogFilter filter) {
    return getPreferences().isFilterSelected(filter);
  }

  @Override
  public void selectFilter(LogFilter filter) {
    getPreferences().selectOnlyFilter(filter);
  }

  @Override
  @NotNull
  public MyProcessingResult processLine(String line) {
    final String type = LogConsolePreferences.getType(line);
    Key contentType = type != null
                      ? LogConsolePreferences.getProcessOutputTypes(type)
                      : (LogConsolePreferences.ERROR.equals(myPrevType) ? ProcessOutputTypes.STDERR : ProcessOutputTypes.STDOUT);
    if (type != null) {
      myPrevType = type;
    }
    final boolean applicable = isApplicable(line);
    return new MyProcessingResult(contentType, applicable, null);
  }

  private abstract class MyFilter extends IndependentLogFilter {
    private final LogConsolePreferences myPreferences;

    protected MyFilter(String name, LogConsolePreferences preferences) {
      super(name);
      myPreferences = preferences;
    }

    @Override
    public boolean isAcceptable(String line) {
      return myPreferences.isApplicable(line, myPrevType, myCheckStandardFilters);
    }
  }
}
