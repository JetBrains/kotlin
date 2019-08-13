// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.InspectionProfileConvertor;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.SeveritiesProvider;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.configurationStore.BundledSchemeEP;
import com.intellij.configurationStore.SchemeDataHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.SchemeManager;
import com.intellij.openapi.options.SchemeManagerFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.profile.codeInspection.*;
import com.intellij.util.ObjectUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@State(
  name = "InspectionProfileManager",
  storages = @Storage("editor.xml"),
  additionalExportFile = InspectionProfileManager.INSPECTION_DIR
)
public class ApplicationInspectionProfileManager extends BaseInspectionProfileManager implements InspectionProfileManager, PersistentStateComponent<Element> {
  private static final ExtensionPointName<BundledSchemeEP> BUNDLED_EP_NAME = ExtensionPointName.create("com.intellij.bundledInspectionProfile");

  private final SchemeManager<InspectionProfileImpl> mySchemeManager;
  private final AtomicBoolean myProfilesAreInitialized = new AtomicBoolean(false);

  public static ApplicationInspectionProfileManager getInstanceImpl() {
    return (ApplicationInspectionProfileManager)ServiceManager.getService(InspectionProfileManager.class);
  }

  public ApplicationInspectionProfileManager() {
    //noinspection TestOnlyProblems
    this(SchemeManagerFactory.getInstance());
  }

  @TestOnly
  public ApplicationInspectionProfileManager(@NotNull SchemeManagerFactory schemeManagerFactory) {
    super(ApplicationManager.getApplication().getMessageBus());

    registerProvidedSeverities();

    mySchemeManager = schemeManagerFactory.create(INSPECTION_DIR, new InspectionProfileProcessor() {
      @NotNull
      @Override
      public String getSchemeKey(@NotNull Function<String, String> attributeProvider, @NotNull String fileNameWithoutExtension) {
        return fileNameWithoutExtension;
      }

      @Override
      @NotNull
      public InspectionProfileImpl createScheme(@NotNull SchemeDataHolder<? super InspectionProfileImpl> dataHolder,
                                                @NotNull String name,
                                                @NotNull Function<? super String, String> attributeProvider,
                                                boolean isBundled) {
        return new InspectionProfileImpl(name, InspectionToolRegistrar.getInstance(), ApplicationInspectionProfileManager.this, dataHolder);
      }

      @Override
      public void onSchemeAdded(@NotNull InspectionProfileImpl scheme) {
        fireProfileChanged(scheme);
      }
    });
  }

  @NotNull
  @Override
  protected SchemeManager<InspectionProfileImpl> getSchemeManager() {
    return mySchemeManager;
  }

  // It should be public to be available from Upsource
  public static void registerProvidedSeverities() {
    for (SeveritiesProvider provider : SeveritiesProvider.EP_NAME.getExtensionList()) {
      for (HighlightInfoType t : provider.getSeveritiesHighlightInfoTypes()) {
        HighlightSeverity highlightSeverity = t.getSeverity(null);
        SeverityRegistrar.registerStandard(t, highlightSeverity);
        TextAttributesKey attributesKey = t.getAttributesKey();
        Icon icon = t instanceof HighlightInfoType.Iconable ? new IconLoader.LazyIcon() {
          @Override
          protected Icon compute() {
            return ((HighlightInfoType.Iconable)t).getIcon();
          }
        } : null;
        HighlightDisplayLevel.registerSeverity(highlightSeverity, attributesKey, icon);
      }
    }
  }

  @Override
  @NotNull
  public Collection<InspectionProfileImpl> getProfiles() {
    initProfiles();
    return Collections.unmodifiableList(mySchemeManager.getAllSchemes());
  }

  private volatile boolean LOAD_PROFILES = !ApplicationManager.getApplication().isUnitTestMode();
  @TestOnly
  public void forceInitProfiles(boolean flag) {
    LOAD_PROFILES = flag;
    myProfilesAreInitialized.set(false);
  }

  public void initProfiles() {
    if (!myProfilesAreInitialized.compareAndSet(false, true) || !LOAD_PROFILES) {
      return;
    }

    loadBundledSchemes();
    mySchemeManager.loadSchemes();

    if (mySchemeManager.isEmpty()) {
      mySchemeManager.addScheme(new InspectionProfileImpl(InspectionProfileKt.DEFAULT_PROFILE_NAME, InspectionToolRegistrar.getInstance(), this));
    }
  }

  private void loadBundledSchemes() {
    if (!isUnitTestOrHeadlessMode()) {
      for (BundledSchemeEP ep : BUNDLED_EP_NAME.getExtensions()) {
        mySchemeManager.loadBundledScheme(ep.getPath() + ".xml", ep);
      }
    }
  }

  private static boolean isUnitTestOrHeadlessMode() {
    return ApplicationManager.getApplication().isUnitTestMode() || ApplicationManager.getApplication().isHeadlessEnvironment();
  }

  public InspectionProfileImpl loadProfile(@NotNull String path) throws IOException, JDOMException {
    final Path file = Paths.get(path);
    if (Files.isRegularFile(file)) {
      try {
        return InspectionProfileLoadUtil.load(file, InspectionToolRegistrar.getInstance(), this);
      }
      catch (IOException | JDOMException e) {
        throw e;
      }
      catch (Exception ignored) {
        ApplicationManager.getApplication().invokeLater(() -> Messages
          .showErrorDialog(InspectionsBundle.message("inspection.error.loading.message", 0, file),
                           InspectionsBundle.message("inspection.errors.occurred.dialog.title")), ModalityState.NON_MODAL);
      }
    }
    return getProfile(path, false);
  }

  @Nullable
  @Override
  public Element getState() {
    Element state = new Element("state");
    getSeverityRegistrar().writeExternal(state);
    return state;
  }

  @Override
  public void loadState(@NotNull Element state) {
    getSeverityRegistrar().readExternal(state);
  }

  public InspectionProfileConvertor getConverter() {
    return new InspectionProfileConvertor(this);
  }

  @Override
  public void setRootProfile(@Nullable String profileName) {
    mySchemeManager.setCurrentSchemeName(profileName);
  }

  @Override
  public InspectionProfileImpl getProfile(@NotNull final String name, boolean returnRootProfileIfNamedIsAbsent) {
    InspectionProfileImpl found = mySchemeManager.findSchemeByName(name);
    if (found != null) {
      return found;
    }
    //profile was deleted
    if (returnRootProfileIfNamedIsAbsent) {
      return getCurrentProfile();
    }
    return null;
  }

  @NotNull
  @Override
  public InspectionProfileImpl getCurrentProfile() {
    initProfiles();

    InspectionProfileImpl current = mySchemeManager.getActiveScheme();
    if (current != null) {
      return current;
    }

    // use default as base, not random custom profile
    InspectionProfileImpl result = mySchemeManager.findSchemeByName(InspectionProfileKt.DEFAULT_PROFILE_NAME);
    if (result == null) {
      InspectionProfileImpl profile = new InspectionProfileImpl(InspectionProfileKt.DEFAULT_PROFILE_NAME);
      addProfile(profile);
      return profile;
    }
    return result;
  }

  @NotNull
  public String getRootProfileName() {
    return ObjectUtils.chooseNotNull(mySchemeManager.getCurrentSchemeName(), InspectionProfileKt.DEFAULT_PROFILE_NAME);
  }

  @Override
  public void fireProfileChanged(@NotNull InspectionProfileImpl profile) {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      ProjectInspectionProfileManager.getInstance(project).fireProfileChanged(profile);
    }
  }
}
