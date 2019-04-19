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
package com.intellij.openapi.externalSystem.model.internal;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;

/**
 * @author Vladislav.Soroka
 */
public class InternalExternalProjectInfo implements ExternalProjectInfo, Serializable {

  private static final long serialVersionUID = 1L;

  @NotNull
  private ProjectSystemId myProjectSystemId;
  @NotNull
  private String myExternalProjectPath;
  @Nullable
  private DataNode<ProjectData> myExternalProjectStructure;
  private long lastSuccessfulImportTimestamp = -1;
  private long lastImportTimestamp = -1;
  private String myBuildNumber;

  public InternalExternalProjectInfo(@NotNull ProjectSystemId projectSystemId,
                                     @NotNull String externalProjectPath,
                                     @Nullable DataNode<ProjectData> externalProjectStructure) {
    myProjectSystemId = projectSystemId;
    myExternalProjectPath = externalProjectPath;
    myExternalProjectStructure = externalProjectStructure;
    myBuildNumber = ApplicationInfo.getInstance().getBuild().asString();
  }

  @Override
  @NotNull
  public ProjectSystemId getProjectSystemId() {
    return myProjectSystemId;
  }

  @Override
  @NotNull
  public String getExternalProjectPath() {
    return myExternalProjectPath;
  }

  @Nullable
  public String getNullSafeExternalProjectPath() {
    return myExternalProjectPath;
  }

  @Override
  @Nullable
  public DataNode<ProjectData> getExternalProjectStructure() {
    return myExternalProjectStructure;
  }

  @Override
  public long getLastSuccessfulImportTimestamp() {
    return lastSuccessfulImportTimestamp;
  }

  @Override
  public long getLastImportTimestamp() {
    return lastImportTimestamp;
  }

  public void setLastSuccessfulImportTimestamp(long lastSuccessfulImportTimestamp) {
    this.lastSuccessfulImportTimestamp = lastSuccessfulImportTimestamp;
  }

  public void setLastImportTimestamp(long lastImportTimestamp) {
    this.lastImportTimestamp = lastImportTimestamp;
  }

  @Override
  public String getBuildNumber() {
    return myBuildNumber;
  }

  @Override
  public ExternalProjectInfo copy() {
    InternalExternalProjectInfo copy = new InternalExternalProjectInfo(
      myProjectSystemId,
      myExternalProjectPath,
      myExternalProjectStructure != null ? myExternalProjectStructure.graphCopy() : null
    );
    copy.setLastImportTimestamp(lastImportTimestamp);
    copy.setLastSuccessfulImportTimestamp(lastSuccessfulImportTimestamp);
    return copy;
  }

  @Override
  public String toString() {
    return "InternalExternalProjectInfo{" +
           "myProjectSystemId=" + myProjectSystemId +
           ", myExternalProjectPath='" + myExternalProjectPath + '\'' +
           ", myExternalProjectStructure=" + myExternalProjectStructure +
           ", lastSuccessfulImportTimestamp=" + lastSuccessfulImportTimestamp +
           ", lastImportTimestamp=" + lastImportTimestamp +
           '}';
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.writeObject(myProjectSystemId);
    out.writeObject(myExternalProjectPath);
    out.writeObject(myExternalProjectStructure);
    out.writeLong(lastSuccessfulImportTimestamp);
    out.writeLong(lastImportTimestamp);
    out.writeObject(myBuildNumber);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    myProjectSystemId = (ProjectSystemId)in.readObject();
    myExternalProjectPath = (String)in.readObject();
    //noinspection unchecked
    myExternalProjectStructure = (DataNode<ProjectData>)in.readObject();
    lastSuccessfulImportTimestamp = in.readLong();
    lastImportTimestamp = in.readLong();
    try {
      myBuildNumber = (String)in.readObject();
    }
    catch (OptionalDataException ignore) {
    }
  }
}
