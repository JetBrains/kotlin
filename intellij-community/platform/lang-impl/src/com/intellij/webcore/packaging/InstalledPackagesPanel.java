// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.webcore.packaging;

import com.google.common.collect.Lists;
import com.intellij.ide.ActivityTracker;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.CatchingConsumer;
import com.intellij.util.IconUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class InstalledPackagesPanel extends JPanel {
  private static final Logger LOG = Logger.getInstance(InstalledPackagesPanel.class);

  private static final String LOADING_PACKAGES_LIST_TITLE = "Loading Packages List";

  private final AnActionButton myUpgradeButton;
  protected final AnActionButton myInstallButton;
  private final AnActionButton myUninstallButton;

  protected final JBTable myPackagesTable;
  private final DefaultTableModel myPackagesTableModel;
  // can be accessed from any thread
  protected volatile PackageManagementService myPackageManagementService;
  protected final Project myProject;
  protected final PackagesNotificationPanel myNotificationArea;
  private final Set<String> myCurrentlyInstalling = new HashSet<>();
  private final Map<InstalledPackage, String> myWaitingToUpgrade = new HashMap<>();

  public InstalledPackagesPanel(@NotNull Project project, @NotNull PackagesNotificationPanel area) {
    super(new BorderLayout());
    myProject = project;
    myNotificationArea = area;

    myPackagesTableModel = new DefaultTableModel(new String[]{"Package", "Version", "Latest version"}, 0) {
      @Override
      public boolean isCellEditable(int i, int i1) {
        return false;
      }
    };
    final TableCellRenderer tableCellRenderer = new MyTableCellRenderer();
    myPackagesTable = new JBTable(myPackagesTableModel) {
      @Override
      public TableCellRenderer getCellRenderer(int row, int column) {
        return tableCellRenderer;
      }
    };
    // Defence from javax.swing.JTable.initializeLocalVars:
    //     setPreferredScrollableViewportSize(new Dimension(450, 400));
    myPackagesTable.setPreferredScrollableViewportSize(null);
    myPackagesTable.setStriped(true);
    myPackagesTable.getTableHeader().setReorderingAllowed(false);
    new TableSpeedSearch(myPackagesTable);

    myUpgradeButton = new DumbAwareActionButton("Upgrade", IconUtil.getMoveUpIcon()) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        upgradeAction();
      }
    };
    myInstallButton = new DumbAwareActionButton("Install", IconUtil.getAddIcon()) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        if (myPackageManagementService != null) {
          ManagePackagesDialog dialog = createManagePackagesDialog();
          dialog.show();
        }
      }
    };
    myInstallButton.setShortcut(CommonShortcuts.getNew());
    myUninstallButton = new DumbAwareActionButton("Uninstall", IconUtil.getRemoveIcon()) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        uninstallAction();
      }
    };
    myUninstallButton.setShortcut(CommonShortcuts.getDelete());
    ToolbarDecorator decorator =
      ToolbarDecorator.createDecorator(myPackagesTable).disableUpDownActions().disableAddAction().disableRemoveAction()
        .addExtraAction(myInstallButton)
        .addExtraAction(myUninstallButton)
        .addExtraAction(myUpgradeButton);

    decorator.addExtraActions(getExtraActions());
    add(decorator.createPanel());
    myInstallButton.setEnabled(false);
    myUninstallButton.setEnabled(false);
    myUpgradeButton.setEnabled(false);

    myPackagesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent event) {
        updateUninstallUpgrade();
      }
    });

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        if (myPackageManagementService != null && myInstallButton.isEnabled()) {
          ManagePackagesDialog dialog = createManagePackagesDialog();
          Point p = e.getPoint();
          int row = myPackagesTable.rowAtPoint(p);
          int column = myPackagesTable.columnAtPoint(p);
          if (row >= 0 && column >= 0) {
            Object pkg = myPackagesTable.getValueAt(row, 0);
            if (pkg instanceof InstalledPackage) {
              dialog.selectPackage((InstalledPackage) pkg);
            }
          }
          dialog.show();
          return true;
        }
        return false;
      }
    }.installOn(myPackagesTable);
  }

  protected AnActionButton[] getExtraActions() {
    return new AnActionButton[0];
  }

  @NotNull
  protected ManagePackagesDialog createManagePackagesDialog() {
    return new ManagePackagesDialog(myProject,
                                    myPackageManagementService,
                                    new PackageManagementService.Listener() {
                                      @Override
                                      public void operationStarted(String packageName) {
                                        myNotificationArea.hide();
                                        myPackagesTable.setPaintBusy(true);
                                      }

                                      @Override
                                      public void operationFinished(String packageName,
                                                                    @Nullable PackageManagementService.ErrorDescription errorDescription) {
                                        myNotificationArea.showResult(packageName, errorDescription);
                                        myPackagesTable.clearSelection();
                                        doUpdatePackages(myPackageManagementService);
                                      }
                                    });
  }

  private void upgradeAction() {
    final int[] rows = myPackagesTable.getSelectedRows();
    if (myPackageManagementService != null) {
      final Set<String> upgradedPackages = new HashSet<>();
      final Set<String> packagesShouldBePostponed = getPackagesToPostpone();
      for (int row : rows) {
        final Object packageObj = myPackagesTableModel.getValueAt(row, 0);
        if (packageObj instanceof InstalledPackage) {
          InstalledPackage pkg = (InstalledPackage)packageObj;
          final String packageName = pkg.getName();
          final String currentVersion = pkg.getVersion();
          final String availableVersion = (String)myPackagesTableModel.getValueAt(row, 2);

          if (packagesShouldBePostponed.contains(packageName)) {
            myWaitingToUpgrade.put(pkg, availableVersion);
          }
          else if (isUpdateAvailable(currentVersion, availableVersion)) {
            upgradePackage(pkg, availableVersion);
            upgradedPackages.add(packageName);
          }
        }
      }

      if (myCurrentlyInstalling.isEmpty() && upgradedPackages.isEmpty() && !myWaitingToUpgrade.isEmpty()) {
        upgradePostponedPackages();
      }
    }
  }

  private void upgradePostponedPackages() {
    final Iterator<Entry<InstalledPackage, String>> iterator = myWaitingToUpgrade.entrySet().iterator();
    final Entry<InstalledPackage, String> toUpgrade = iterator.next();
    iterator.remove();
    upgradePackage(toUpgrade.getKey(), toUpgrade.getValue());
  }

  protected Set<String> getPackagesToPostpone() {
    return Collections.emptySet();
  }

  private void upgradePackage(@NotNull final InstalledPackage pkg, @Nullable final String toVersion) {
    final PackageManagementService selPackageManagementService = myPackageManagementService;
    myPackageManagementService.fetchPackageVersions(pkg.getName(), new CatchingConsumer<List<String>, Exception>() {
      @Override
      public void consume(List<String> releases) {
        if (!releases.isEmpty() && !isUpdateAvailable(pkg.getVersion(), releases.get(0))) {
          return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
          ModalityState modalityState = ModalityState.current();
          final PackageManagementService.Listener listener = new PackageManagementService.Listener() {
            @Override
            public void operationStarted(final String packageName) {
              ApplicationManager.getApplication().invokeLater(() -> {
                myPackagesTable.setPaintBusy(true);
                myCurrentlyInstalling.add(packageName);
              }, modalityState);
            }

            @Override
            public void operationFinished(final String packageName,
                                          @Nullable final PackageManagementService.ErrorDescription errorDescription) {
              ApplicationManager.getApplication().invokeLater(() -> {
                myPackagesTable.clearSelection();
                updatePackages(selPackageManagementService);
                myCurrentlyInstalling.remove(packageName);
                myPackagesTable.setPaintBusy(!myCurrentlyInstalling.isEmpty());
                if (errorDescription == null) {
                  myNotificationArea.showSuccess("Package " + packageName + " successfully upgraded");
                }
                else {
                  myNotificationArea.showError("Upgrade packages failed. <a href=\"xxx\">Details...</a>", "Upgrade Packages Failed",
                                               errorDescription);
                }

                if (myCurrentlyInstalling.isEmpty() && !myWaitingToUpgrade.isEmpty()) {
                  upgradePostponedPackages();
                }
              }, modalityState);
            }
          };
          PackageManagementServiceEx serviceEx = getServiceEx();
          if (serviceEx != null) {
            serviceEx.updatePackage(pkg, toVersion, listener);
          }
          else {
            myPackageManagementService.installPackage(new RepoPackage(pkg.getName(), null /* TODO? */), null, true, null, listener, false);
          }
          myUpgradeButton.setEnabled(false);
        }, ModalityState.any());
      }

      @Override
      public void consume(Exception e) {
        ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog("Error occurred. Please, check your internet connection.",
                                                                                     "Upgrade Package Failed."), ModalityState.any());
      }
    });
  }

  @Nullable
  private PackageManagementServiceEx getServiceEx() {
    return ObjectUtils.tryCast(myPackageManagementService, PackageManagementServiceEx.class);
  }

  protected void updateUninstallUpgrade() {
    final int[] selected = myPackagesTable.getSelectedRows();
    boolean upgradeAvailable = false;
    boolean canUninstall = selected.length != 0;
    boolean canInstall = installEnabled();
    boolean canUpgrade = true;
    if (myPackageManagementService != null && selected.length != 0) {
      for (int i = 0; i != selected.length; ++i) {
        final int index = selected[i];
        if (index >= myPackagesTable.getRowCount()) continue;
        final Object value = myPackagesTable.getValueAt(index, 0);
        if (value instanceof InstalledPackage) {
          final InstalledPackage pkg = (InstalledPackage)value;
          if (!canUninstallPackage(pkg)) {
            canUninstall = false;
          }
          canInstall = canInstallPackage(pkg);
          if (!canUpgradePackage(pkg)) {
            canUpgrade = false;
          }
          final String pyPackageName = pkg.getName();
          final String availableVersion = (String)myPackagesTable.getValueAt(index, 2);
          if (!upgradeAvailable) {
            upgradeAvailable = isUpdateAvailable(pkg.getVersion(), availableVersion) &&
                               !myCurrentlyInstalling.contains(pyPackageName);
          }
          if (!canUninstall && !canUpgrade) break;
        }
      }
    }
    myUninstallButton.setEnabled(canUninstall);
    myInstallButton.setEnabled(canInstall);
    myUpgradeButton.setEnabled(upgradeAvailable && canUpgrade);
  }

  protected boolean canUninstallPackage(InstalledPackage pyPackage) {
    return true;
  }

  protected boolean canInstallPackage(@NotNull final InstalledPackage pyPackage) {
    return true;
  }

  protected boolean installEnabled() {
    return true;
  }

  protected boolean canUpgradePackage(InstalledPackage pyPackage) {
    return true;
  }

  private void uninstallAction() {
    final List<InstalledPackage> packages = getSelectedPackages();
    final PackageManagementService selPackageManagementService = myPackageManagementService;
    if (selPackageManagementService != null) {
      ModalityState modalityState = ModalityState.current();
      PackageManagementService.Listener listener = new PackageManagementService.Listener() {
        @Override
        public void operationStarted(String packageName) {
          ApplicationManager.getApplication().invokeLater(
            () -> myPackagesTable.setPaintBusy(true),
            modalityState
          );
        }

        @Override
        public void operationFinished(final String packageName,
                                      @Nullable final PackageManagementService.ErrorDescription errorDescription) {
          ApplicationManager.getApplication().invokeLater(() -> {
            myPackagesTable.clearSelection();
            updatePackages(selPackageManagementService);
            myPackagesTable.setPaintBusy(!myCurrentlyInstalling.isEmpty());
            if (errorDescription == null) {
              if (packageName != null) {
                myNotificationArea.showSuccess("Package '" + packageName + "' successfully uninstalled");
              }
              else {
                myNotificationArea.showSuccess("Packages successfully uninstalled");
              }
            }
            else {
              myNotificationArea.showError("Uninstall packages failed. <a href=\"xxx\">Details...</a>", "Uninstall Packages Failed",
                                           errorDescription);
            }
          }, modalityState);
        }
      };
      myPackageManagementService.uninstallPackages(packages, listener);
    }
  }

  @NotNull
  private List<InstalledPackage> getSelectedPackages() {
    final List<InstalledPackage> results = new ArrayList<>();
    final int[] rows = myPackagesTable.getSelectedRows();
    for (int row : rows) {
      final Object packageName = myPackagesTableModel.getValueAt(row, 0);
      if (packageName instanceof InstalledPackage) {
        results.add((InstalledPackage)packageName);
      }
    }
    return results;
  }

  public void updatePackages(@Nullable PackageManagementService packageManagementService) {
    myPackageManagementService = packageManagementService;
    myPackagesTable.clearSelection();
    myPackagesTableModel.getDataVector().clear();
    myPackagesTableModel.fireTableDataChanged();
    if (packageManagementService != null) {
      doUpdatePackages(packageManagementService);
    }
  }

  private void onUpdateStarted() {
    myPackagesTable.setPaintBusy(true);
    myPackagesTable.getEmptyText().setText("Loading...");
  }

  private void onUpdateFinished() {
    myPackagesTable.setPaintBusy(!myCurrentlyInstalling.isEmpty());
    myPackagesTable.getEmptyText().setText(StatusText.DEFAULT_EMPTY_TEXT);
    updateUninstallUpgrade();
    // Action button presentations won't be updated if no events occur (e.g. mouse isn't moving, keys aren't being pressed).
    // In that case emulating activity will help:
    ActivityTracker.getInstance().inc();
  }

  public void doUpdatePackages(@NotNull final PackageManagementService packageManagementService) {
    onUpdateStarted();
    ProgressManager progressManager = ProgressManager.getInstance();
    progressManager.run(new Task.Backgroundable(myProject, LOADING_PACKAGES_LIST_TITLE, true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        Collection<InstalledPackage> packages = Lists.newArrayList();
        try {
          packages = packageManagementService.getInstalledPackages();
        }
        catch (IOException e) {
          LOG.warn(e.getMessage()); // do nothing, we already have an empty list
        }
        finally {
          final Collection<InstalledPackage> finalPackages = packages;

          final Map<String, RepoPackage> cache = buildNameToPackageMap(packageManagementService.getAllPackagesCached());
          final boolean shouldFetchLatestVersionsForOnlyInstalledPackages = shouldFetchLatestVersionsForOnlyInstalledPackages();
          if (cache.isEmpty()) {
            if (!shouldFetchLatestVersionsForOnlyInstalledPackages) {
              refreshLatestVersions(packageManagementService);
            }
          }
          UIUtil.invokeLaterIfNeeded(() -> {
            if (packageManagementService == myPackageManagementService) {
              myPackagesTableModel.getDataVector().clear();
              for (InstalledPackage pkg : finalPackages) {
                RepoPackage repoPackage = cache.get(pkg.getName());
                final String version = repoPackage != null ? repoPackage.getLatestVersion() : null;
                myPackagesTableModel
                  .addRow(new Object[]{pkg, pkg.getVersion(), version == null ? "" : version});
              }
              if (!cache.isEmpty()) {
                onUpdateFinished();
              }
              if (shouldFetchLatestVersionsForOnlyInstalledPackages) {
                setLatestVersionsForInstalledPackages();
              }
            }
          });
        }
      }
    });
  }

  private InstalledPackage getInstalledPackageAt(int index) {
    return (InstalledPackage) myPackagesTableModel.getValueAt(index, 0);
  }

  private void setLatestVersionsForInstalledPackages() {
    final PackageManagementServiceEx serviceEx = getServiceEx();
    if (serviceEx == null) {
      return;
    }
    int packageCount = myPackagesTableModel.getRowCount();
    if (packageCount == 0) {
      onUpdateFinished();
    }
    final AtomicInteger inProgressPackageCount = new AtomicInteger(packageCount);
    for (int i = 0; i < packageCount; ++i) {
      final int finalIndex = i;
      final InstalledPackage pkg = getInstalledPackageAt(finalIndex);
      serviceEx.fetchLatestVersion(pkg, new CatchingConsumer<String, Exception>() {

        private void decrement() {
          if (inProgressPackageCount.decrementAndGet() == 0) {
            onUpdateFinished();
          }
        }

        @Override
        public void consume(Exception e) {
          UIUtil.invokeLaterIfNeeded(() -> decrement());
          LOG.warn("Cannot fetch the latest version of the installed package " + pkg, e);
        }

        @Override
        public void consume(@Nullable final String latestVersion) {
          UIUtil.invokeLaterIfNeeded(() -> {
            if (finalIndex < myPackagesTableModel.getRowCount()) {
              InstalledPackage p = getInstalledPackageAt(finalIndex);
              if (pkg == p) {
                myPackagesTableModel.setValueAt(latestVersion, finalIndex, 2);
              }
            }
            decrement();
          });
        }
      });
    }
  }

  private boolean shouldFetchLatestVersionsForOnlyInstalledPackages() {
    PackageManagementServiceEx serviceEx = getServiceEx();
    if (serviceEx != null) {
      return serviceEx.shouldFetchLatestVersionsForOnlyInstalledPackages();
    }
    return false;
  }

  private boolean isUpdateAvailable(@Nullable String currentVersion, @Nullable String availableVersion) {
    if (availableVersion == null) {
      return false;
    }
    if (currentVersion == null) {
      return true;
    }
    PackageManagementService service = myPackageManagementService;
    if (service != null) {
      return service.compareVersions(currentVersion, availableVersion) < 0;
    }
    return PackageVersionComparator.VERSION_COMPARATOR.compare(currentVersion, availableVersion) < 0;
  }

  private void refreshLatestVersions(@NotNull final PackageManagementService packageManagementService) {
    final Application application = ApplicationManager.getApplication();
    application.executeOnPooledThread(() -> {
      if (packageManagementService == myPackageManagementService) {
        try {
          List<RepoPackage> packages = packageManagementService.reloadAllPackages();
          final Map<String, RepoPackage> packageMap = buildNameToPackageMap(packages);
          application.invokeLater(() -> {
            for (int i = 0; i != myPackagesTableModel.getRowCount(); ++i) {
              final InstalledPackage pyPackage = (InstalledPackage)myPackagesTableModel.getValueAt(i, 0);
              final RepoPackage repoPackage = packageMap.get(pyPackage.getName());
              myPackagesTableModel.setValueAt(repoPackage == null ? null : repoPackage.getLatestVersion(), i, 2);
            }
            myPackagesTable.setPaintBusy(!myCurrentlyInstalling.isEmpty());
          }, ModalityState.stateForComponent(myPackagesTable));
        }
        catch (IOException ignored) {
          LOG.warn("Cannot refresh the list of available packages with their latest versions", ignored);
          myPackagesTable.setPaintBusy(false);
        }
      }
    });
  }

  private Map<String, RepoPackage> buildNameToPackageMap(List<? extends RepoPackage> packages) {
    try {
      return doBuildNameToPackageMap(packages);
    }
    catch (Exception e) {
      PackageManagementService service = myPackageManagementService;
      LOG.error("Failure in " + getClass().getName() +
                ", service: " + (service != null ? service.getClass().getName() : null), e);
      return Collections.emptyMap();
    }
  }

  private static Map<String, RepoPackage> doBuildNameToPackageMap(List<? extends RepoPackage> packages) {
    final Map<String, RepoPackage> packageMap = new HashMap<>();
    for (RepoPackage aPackage : packages) {
      packageMap.put(aPackage.getName(), aPackage);
    }
    return packageMap;
  }

  private class MyTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      final JLabel cell = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      final String version = (String)table.getValueAt(row, 1);
      final String availableVersion = (String)table.getValueAt(row, 2);
      boolean update = column == 2 &&
                       StringUtil.isNotEmpty(availableVersion) &&
                       isUpdateAvailable(version, availableVersion);
      cell.setIcon(update ? IconUtil.getMoveUpIcon() : null);
      final Object pyPackage = table.getValueAt(row, 0);
      if (pyPackage instanceof InstalledPackage) {
        cell.setToolTipText(((InstalledPackage) pyPackage).getTooltipText());
      }
      return cell;
    }
  }
}
