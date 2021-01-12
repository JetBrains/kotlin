// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.internal.daemon;

import org.gradle.internal.id.IdGenerator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.launcher.daemon.client.DaemonClientConnection;
import org.gradle.launcher.daemon.client.DaemonClientFactory;
import org.gradle.launcher.daemon.client.DaemonConnector;
import org.gradle.launcher.daemon.client.ReportStatusDispatcher;
import org.gradle.launcher.daemon.protocol.ReportStatus;
import org.gradle.launcher.daemon.protocol.Status;
import org.gradle.launcher.daemon.registry.DaemonInfo;
import org.gradle.launcher.daemon.registry.DaemonRegistry;
import org.gradle.launcher.daemon.registry.DaemonStopEvent;
import org.gradle.launcher.daemon.registry.DaemonStopEvents;
import org.gradle.launcher.daemon.server.expiry.DaemonExpirationStatus;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Vladislav.Soroka
 */
public class DaemonStatusAction extends DaemonAction {
  public DaemonStatusAction(String serviceDirectoryPath) {
    super(serviceDirectoryPath);
  }

  public List<DaemonState> run(DaemonClientFactory daemonClientFactory) {
    ServiceRegistry daemonServices = getDaemonServices(daemonClientFactory);
    DaemonConnector daemonConnector = daemonServices.get(DaemonConnector.class);
    DaemonRegistry daemonRegistry = daemonServices.get(DaemonRegistry.class);
    IdGenerator<?> idGenerator = daemonServices.get(IdGenerator.class);
    return new ReportDaemonStatusClient(daemonRegistry, daemonConnector, idGenerator).get();
  }

  static class ReportDaemonStatusClient {
    private final DaemonRegistry daemonRegistry;
    private final DaemonConnector connector;
    private final IdGenerator<?> idGenerator;
    private final ReportStatusDispatcher reportStatusDispatcher;

    ReportDaemonStatusClient(DaemonRegistry daemonRegistry,
                             DaemonConnector connector,
                             IdGenerator<?> idGenerator) {
      this.daemonRegistry = daemonRegistry;
      this.connector = connector;
      this.idGenerator = idGenerator;
      this.reportStatusDispatcher = new ReportStatusDispatcher();
    }

    public List<DaemonState> get() {
      List<DaemonState> daemons = new ArrayList<>();
      for (DaemonInfo daemon : this.daemonRegistry.getAll()) {
        DaemonClientConnection connection = this.connector.maybeConnect(daemon);
        if (connection != null) {
          DaemonInfo connectionDaemon = connection.getDaemon() instanceof DaemonInfo ? (DaemonInfo)connection.getDaemon() : daemon;
          try {
            List<String> daemonOpts = connectionDaemon.getContext().getDaemonOpts();
            File javaHome = connectionDaemon.getContext().getJavaHome();
            Integer idleTimeout = connectionDaemon.getContext().getIdleTimeout();
            File registryDir = connectionDaemon.getContext().getDaemonRegistryDir();

            Object id = this.idGenerator.generateId();
            byte[] token = daemon.getToken();
            ReportStatus statusCommand = createCommand(ReportStatus.class, id, token);
            Status status = this.reportStatusDispatcher.dispatch(connection, statusCommand);
            if (status != null) {
              daemons.add(new DaemonState(connectionDaemon.getPid(),
                                          connectionDaemon.getToken(),
                                          status.getVersion(),
                                          status.getStatus(),
                                          null,
                                          connectionDaemon.getLastBusy().getTime(),
                                          null,
                                          daemonOpts,
                                          javaHome,
                                          idleTimeout,
                                          registryDir));
            }
            else {
              daemons.add(new DaemonState(connectionDaemon.getPid(),
                                          connectionDaemon.getToken(),
                                          "UNKNOWN",
                                          "UNKNOWN",
                                          null,
                                          connectionDaemon.getLastBusy().getTime(),
                                          null,
                                          daemonOpts,
                                          javaHome,
                                          idleTimeout,
                                          registryDir));
            }
          }
          finally {
            connection.stop();
          }
        }
      }

      List<DaemonStopEvent> stopEvents = DaemonStopEvents.uniqueRecentDaemonStopEvents(this.daemonRegistry.getStopEvents());
      for (DaemonStopEvent stopEvent : stopEvents) {
        DaemonExpirationStatus expirationStatus = stopEvent.getStatus();
        String daemonExpirationStatus =
          expirationStatus != null ? expirationStatus.name().replace("_", " ").toLowerCase(Locale.ENGLISH) : "";
        Long stopEventPid;
        if (GradleVersion.current().compareTo(GradleVersion.version("3.0")) <= 0) {
          try {
            Field pidField = stopEvent.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            stopEventPid = pidField.getLong(stopEvent);
          }
          catch (Exception ignore) {
            stopEventPid = -1L;
          }
        }
        else {
          stopEventPid = stopEvent.getPid();
        }
        daemons.add(new DaemonState(stopEventPid,
                                    null,
                                    null,
                                    "Stopped",
                                    stopEvent.getReason(),
                                    stopEvent.getTimestamp().getTime(),
                                    daemonExpirationStatus, null, null, null, null));
      }

      return daemons;
    }
  }
}
