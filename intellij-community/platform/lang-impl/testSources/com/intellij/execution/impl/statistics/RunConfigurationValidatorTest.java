// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl.statistics;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.impl.statistics.RunConfigurationTypeUsagesCollector.RunConfigurationUtilValidator;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomWhiteListRule;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightPlatformTestCase;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

public class RunConfigurationValidatorTest extends LightPlatformTestCase {

  private static void doTest(@NotNull ValidationResultType expected, @NotNull CustomWhiteListRule validator,
                             @NotNull String data, @NotNull EventContext context) {
    final Disposable disposable = Disposer.newDisposable();
    try {
      final ExtensionPoint<ConfigurationType> ep = Extensions.getRootArea().getExtensionPoint(ConfigurationType.CONFIGURATION_TYPE_EP);
      TestCase.assertNotNull(ep);

      ep.registerExtension(new BaseTestConfigurationType.FirstTestRunConfigurationType(), disposable);
      ep.registerExtension(new BaseTestConfigurationType.SecondTestRunConfigurationType(), disposable);
      ep.registerExtension(new BaseTestConfigurationType.MultiFactoryTestRunConfigurationType(), disposable);

      TestCase.assertEquals(expected, validator.validate(data, context));
    }
    finally {
      Disposer.dispose(disposable);
    }
  }

  private static void doValidateEventId(@NotNull String eventId, @NotNull FeatureUsageData eventData) {
    final RunConfigurationUtilValidator validator = new RunConfigurationUtilValidator();
    final EventContext context = EventContext.create(eventId, eventData.build());
    doTest(ValidationResultType.ACCEPTED, validator, eventId, context);
  }

  @SuppressWarnings("SameParameterValue")
  private static void doValidateFactoryData(@NotNull String eventId, @NotNull FeatureUsageData eventData) {
    final RunConfigurationUtilValidator validator = new RunConfigurationUtilValidator();
    final EventContext context = EventContext.create(eventId, eventData.build());
    final Object data = eventData.build().get("factory");
    assertTrue(data instanceof String);
    doTest(ValidationResultType.ACCEPTED, validator, (String)data, context);
  }

  private static void doRejectEventId(@NotNull String eventId, @NotNull FeatureUsageData eventData) {
    final RunConfigurationUtilValidator validator = new RunConfigurationUtilValidator();
    final EventContext context = EventContext.create(eventId, eventData.build());
    doTest(ValidationResultType.REJECTED, validator, eventId, context);
  }

  @SuppressWarnings("SameParameterValue")
  private static void doRejectFactoryData(@NotNull String eventId, @NotNull FeatureUsageData eventData) {
    final RunConfigurationUtilValidator validator = new RunConfigurationUtilValidator();
    final EventContext context = EventContext.create(eventId, eventData.build());
    final Object data = eventData.build().get("factory");
    assertTrue(data instanceof String);
    doTest(ValidationResultType.REJECTED, validator, (String)data, context);
  }

  public void testRunConfigurationWithOneFactory() {
    doValidateEventId("FirstTestRunConfigurationType", newFeatureUsageData());
  }

  public void testAnotherRunConfigurationWithOneFactory() {
    doValidateEventId("SecondTestRunConfigurationType", newFeatureUsageData());
  }

  public void testAnotherRunConfigurationWithEmptyName() {
    doValidateEventId("SecondTestRunConfigurationType", newFeatureUsageData());
  }

  public void testThirdPartyRunConfigurationWithOneFactory() {
    doValidateEventId("third.party", newFeatureUsageData());
  }

  public void testRejectUnknownRunConfiguration() {
    doRejectEventId("UnknownTestRunConfigurationType", newFeatureUsageData());
  }

  public void testRejectEmptyRunConfiguration() {
    doRejectEventId("", newFeatureUsageData());
  }

  public void testRunConfigurationWithIncorrectFactory() {
    doRejectEventId("FirstTestRunConfigurationType/Local", newFeatureUsageData());
  }

  public void testRunConfigurationWithLocalFactory() {
    final FeatureUsageData data = newFeatureUsageData().addData("factory", "Local");
    doValidateEventId("MultiFactoryTestRunConfigurationType", data);
    doValidateFactoryData("MultiFactoryTestRunConfigurationType", data);
  }

  public void testRunConfigurationWithRemoteFactory() {
    final FeatureUsageData data = newFeatureUsageData().addData("factory", "Remote");
    doValidateEventId("MultiFactoryTestRunConfigurationType", data);
    doValidateFactoryData("MultiFactoryTestRunConfigurationType", data);
  }

  public void testRunConfigurationWithEmptyFactory() {
    doRejectEventId("MultiFactoryTestRunConfigurationType/", newFeatureUsageData());
  }

  public void testRejectRunConfigurationWithUnknownFactory() {
    final FeatureUsageData data = newFeatureUsageData().addData("factory", "Unknown");
    doRejectEventId("MultiFactoryTestRunConfigurationType", data);
    doRejectFactoryData("MultiFactoryTestRunConfigurationType", data);
  }

  public void testRejectRunConfigurationWithEmptyName() {
    doRejectEventId("/Remote", newFeatureUsageData());
  }

  public void testRejectRunConfigurationWithEmptyNameAndFactory() {
    doRejectEventId("/", newFeatureUsageData());
  }

  public void testRejectRunConfigurationWithTwoMuchValues() {
    doRejectEventId("RunConfigName/Factory/AnotherValue", newFeatureUsageData());
  }

  @NotNull
  private static FeatureUsageData newFeatureUsageData() {
    return new FeatureUsageData().
      addData("plugin_type", "PLATFORM").
      addData("edit_before_run", true).
      addData("activate_before_run", false).
      addData("shared", false).
      addData("parallel", true);
  }
}
