/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.colors.pluginExport;

import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;

public class PluginExportData {
  private final static String DESCRIPTION_PROPERTY = "description";
  private final static String VENDOR_NAME_PROPERTY = "vendorName";
  private final static String VENDOR_MAIL_PROPERTY = "vendorMail";
  private final static String VENDOR_URL_PROPERTY = "vendorUrl";
  private final static String VERSION_PROPERTY = "version";

  private String myDescription;
  private String myVendorName;
  private String myVendorMail;
  private String myVendorUrl;

  private String myPluginVersion;
  private String myChangeNotes;


  public PluginExportData(@NotNull Properties info) {
    initData(info);
  }

  private void initData(@NotNull Properties info) {
    myDescription = info.getProperty(DESCRIPTION_PROPERTY);
    myVendorName = info.getProperty(VENDOR_NAME_PROPERTY);
    myVendorMail = info.getProperty(VENDOR_MAIL_PROPERTY);
    myVendorUrl = info.getProperty(VENDOR_URL_PROPERTY);
    myPluginVersion = info.getProperty(VERSION_PROPERTY);
  }

  public String getDescription() {
    return normalize(myDescription, "");
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  public String getVendorName() {
    return normalize(myVendorName, "");
  }

  public void setVendorName(String vendorName) {
    myVendorName = vendorName;
  }

  public String getVendorMail() {
    return normalize(myVendorMail, "");
  }

  public void setVendorMail(String vendorMail) {
    myVendorMail = vendorMail;
  }

  public String getVendorUrl() {
    return normalize(myVendorUrl, "http://");
  }

  public void setVendorUrl(String vendorUrl) {
    myVendorUrl = vendorUrl;
  }

  public String getPluginVersion() {
    return normalize(myPluginVersion, "0.1");
  }

  public void setPluginVersion(String pluginVersion) {
    myPluginVersion = pluginVersion;
  }

  public String getChangeNotes() {
    return normalize(myChangeNotes, "");
  }

  public void setChangeNotes(String changeNotes) {
    myChangeNotes = changeNotes;
  }

  public String getSinceBuild() {
    return AbstractColorsScheme.CURR_VERSION + ".0";
  }

  private static String normalize(@Nullable String value, @NotNull String defaultValue) {
    return value == null || StringUtil.isEmptyOrSpaces(value) ? defaultValue : value.trim();
  }

  public boolean isEmpty() {
    return
      myDescription == null &&
      myPluginVersion == null &&
      myVendorUrl == null &&
      myVendorMail == null &&
      myVendorName == null;
  }

  public void saveToProperties(@NotNull Properties properties) {
    properties.setProperty(DESCRIPTION_PROPERTY, getDescription());
    properties.setProperty(VERSION_PROPERTY, getPluginVersion());
    properties.setProperty(VENDOR_URL_PROPERTY, getVendorUrl());
    properties.setProperty(VENDOR_MAIL_PROPERTY, getVendorMail());
    properties.setProperty(VENDOR_NAME_PROPERTY, getVendorName());
  }
}
