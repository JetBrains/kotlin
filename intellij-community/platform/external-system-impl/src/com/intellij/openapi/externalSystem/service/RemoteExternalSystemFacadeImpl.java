// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Denis Zhdanov
 */
public class RemoteExternalSystemFacadeImpl<S extends ExternalSystemExecutionSettings> extends AbstractExternalSystemFacadeImpl<S> {

  private static final long DEFAULT_REMOTE_PROCESS_TTL_IN_MS = TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES);

  private final AtomicInteger myCallsInProgressNumber = new AtomicInteger();
  private Future<?> myShutdownFuture = CompletableFuture.completedFuture(null);
  private final AtomicLong    myTtlMs                 = new AtomicLong(DEFAULT_REMOTE_PROCESS_TTL_IN_MS);

  private volatile boolean myStdOutputConfigured;

  public RemoteExternalSystemFacadeImpl(@NotNull Class<ExternalSystemProjectResolver<S>> projectResolverClass,
                                        @NotNull Class<ExternalSystemTaskManager<S>> buildManagerClass)
    throws IllegalAccessException, InstantiationException
  {
    super(projectResolverClass, buildManagerClass);
    updateAutoShutdownTime();
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      throw new IllegalArgumentException(
        "Can't create external system facade. Reason: given arguments don't contain information about external system resolver to use");
    }
    final Class<ExternalSystemProjectResolver<?>> resolverClass = (Class<ExternalSystemProjectResolver<?>>)Class.forName(args[0]);
    if (!ExternalSystemProjectResolver.class.isAssignableFrom(resolverClass)) {
      throw new IllegalArgumentException(String.format(
        "Can't create external system facade. Reason: given external system resolver class (%s) must be IS-A '%s'",
        resolverClass,
        ExternalSystemProjectResolver.class));
    }

    if (args.length < 2) {
      throw new IllegalArgumentException(
        "Can't create external system facade. Reason: given arguments don't contain information about external system build manager to use"
      );
    }
    final Class<ExternalSystemTaskManager<?>> buildManagerClass = (Class<ExternalSystemTaskManager<?>>)Class.forName(args[1]);
    if (!ExternalSystemProjectResolver.class.isAssignableFrom(resolverClass)) {
      throw new IllegalArgumentException(String.format(
        "Can't create external system facade. Reason: given external system build manager (%s) must be IS-A '%s'",
        buildManagerClass, ExternalSystemTaskManager.class
      ));
    }

    // running the code indicates remote communication mode with external system
    Registry.get(
      System.getProperty(ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY) +
      ExternalSystemConstants.USE_IN_PROCESS_COMMUNICATION_REGISTRY_KEY_SUFFIX).setValue(false);

    RemoteExternalSystemFacadeImpl facade = new RemoteExternalSystemFacadeImpl(resolverClass, buildManagerClass);
    facade.init();
    start(facade);
  }

  @SuppressWarnings({"unchecked", "UseOfSystemOutOrSystemErr"})
  @Override
  protected <I extends RemoteExternalSystemService<S>, C extends I> I createService(@NotNull Class<I> interfaceClass, @NotNull final C impl)
    throws RemoteException
  {
    if (!myStdOutputConfigured) {
      myStdOutputConfigured = true;
      System.setOut(new LineAwarePrintStream(System.out));
      System.setErr(new LineAwarePrintStream(System.err));
    }

    I proxy = (I)Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{interfaceClass}, new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        myCallsInProgressNumber.incrementAndGet();
        try {
          return method.invoke(impl, args);
        }
        catch (InvocationTargetException e) {
          throw e.getCause();
        }
        finally {
          myCallsInProgressNumber.decrementAndGet();
          updateAutoShutdownTime();
        }
      }
    });
    return  (I)UnicastRemoteObject.exportObject(proxy, 0);
  }

  @Override
  public void applySettings(@NotNull S settings) throws RemoteException {
    super.applySettings(settings);
    long ttl = settings.getRemoteProcessIdleTtlInMs();
    if (ttl > 0) {
      myTtlMs.set(ttl);
    }
  }

  /**
   * Schedules automatic process termination in {@code #REMOTE_GRADLE_PROCESS_TTL_IN_MS} milliseconds.
   * <p/>
   * Rationale: it's possible that IJ user performs gradle related activity (e.g. import from gradle) when the works purely
   * at IJ. We don't want to keep remote process that communicates with the gradle api then.
   */
  private void updateAutoShutdownTime() {
    myShutdownFuture.cancel(false);
    myShutdownFuture = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
      if (myCallsInProgressNumber.get() > 0) {
        updateAutoShutdownTime();
        return;
      }
      System.exit(0);
    }, (int)myTtlMs.get(), TimeUnit.MILLISECONDS);
  }

  private static class LineAwarePrintStream extends PrintStream {
    private LineAwarePrintStream(@NotNull final PrintStream delegate) {
      super(new OutputStream() {

        @NotNull private final StringBuilder myBuffer = new StringBuilder();

        @Override
        public void write(int b) {
          char c = (char)b;
          myBuffer.append(c);
          if (c == '\n') {
            doFlush();
          }
        }

        @Override
        public void write(byte[] b, int off, int len) {
          int start = off;
          int maxOffset = off + len;
          for (int i = off; i < maxOffset; i++) {
            if (b[i] == '\n') {
              myBuffer.append(new String(b, start, i - start + 1, StandardCharsets.UTF_8));
              doFlush();
              start = i + 1;
            }
          }

          if (start < maxOffset) {
            myBuffer.append(new String(b, start, maxOffset - start, StandardCharsets.UTF_8));
          }
        }

        private void doFlush() {
          delegate.print(myBuffer.toString());
          delegate.flush();
          myBuffer.setLength(0);
        }
      });
    }
  }

}
