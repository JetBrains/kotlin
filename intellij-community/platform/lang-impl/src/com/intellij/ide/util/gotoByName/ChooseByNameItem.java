package com.intellij.ide.util.gotoByName;

import org.jetbrains.annotations.NotNull;

/**
 * An item displayed in ListChooseByNameModel.
 *
 * @author yole
 */
public interface ChooseByNameItem {
  @NotNull
  String getName();
  String getDescription();
}
