// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.internal.daemon;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class DaemonState implements Serializable {
  private final Long myPid;
  private final byte[] myToken;
  private final String myVersion;
  private final String myStatus;
  private final String myReason;
  private final long myTimestamp;
  private final String myDaemonExpirationStatus;
  private final List<String> myDaemonOpts;
  private final File myJavaHome;
  private final Integer myIdleTimeout;
  private final File myRegistryDir;

  public DaemonState(Long pid,
                     byte[] token,
                     String version,
                     String status,
                     String reason,
                     long timestamp,
                     String daemonExpirationStatus,
                     List<String> daemonOpts,
                     File javaHome,
                     Integer idleTimeout,
                     File registryDir) {
    myPid = pid;
    myToken = token;
    myVersion = version;
    myStatus = status;
    myReason = reason;
    myTimestamp = timestamp;
    myDaemonExpirationStatus = daemonExpirationStatus;
    myDaemonOpts = daemonOpts;
    myJavaHome = javaHome;
    myIdleTimeout = idleTimeout;
    myRegistryDir = registryDir;
  }

  public Long getPid() {
    return myPid;
  }

  public byte[] getToken() {
    return myToken;
  }

  public String getVersion() {
    return myVersion;
  }

  public String getStatus() {
    return myStatus;
  }

  public String getReason() {
    return myReason;
  }

  public long getTimestamp() {
    return myTimestamp;
  }

  public String getDaemonExpirationStatus() {
    return myDaemonExpirationStatus;
  }

  public List<String> getDaemonOpts() {
    return myDaemonOpts;
  }

  public File getJavaHome() {
    return myJavaHome;
  }

  public Integer getIdleTimeout() {
    return myIdleTimeout;
  }

  public File getRegistryDir() {
    return myRegistryDir;
  }

  public String getDescription() {
    StringBuilder info = new StringBuilder();
    info.append(myPid).append(" ")
      .append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT ).format(new Date(myTimestamp))).append(" ")
      .append(myStatus).append(" ");
    if(myVersion != null && !myVersion.isEmpty()) {
      info.append("Gradle version: ").append(myVersion);
    }
    if(myDaemonExpirationStatus != null && !myDaemonExpirationStatus.isEmpty()) {
      info.append("\nExpiration status: ").append(myDaemonExpirationStatus);
    }
    if(myReason != null && !myReason.isEmpty()) {
      info.append("\nStop reason: ").append(myReason);
    }
    if(myRegistryDir != null) {
      info.append("\nDaemons dir: ").append(myRegistryDir);
    }
    if(myJavaHome != null) {
      info.append("\nJava home: ").append(myJavaHome.getPath());
    }
    if(myDaemonOpts != null && !myDaemonOpts.isEmpty()) {
      info.append("\nDaemon options: ").append(myDaemonOpts);
    }
    if(myIdleTimeout != null) {
      info.append("\nIdle timeout: ").append(myIdleTimeout);
    }

    return info.toString();
  }
}
