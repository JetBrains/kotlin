// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.SimpleChannelInboundHandlerAdapter;
import org.jetbrains.jps.api.CmdlineProtoUtil;
import org.jetbrains.jps.api.CmdlineRemoteProto;
import org.jetbrains.jps.api.RequestFuture;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
* @author Eugene Zhuravlev
*/
@ChannelHandler.Sharable
class BuildMessageDispatcher extends SimpleChannelInboundHandlerAdapter<CmdlineRemoteProto.Message> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.server.BuildMessageDispatcher");

  private static final AttributeKey<SessionData> SESSION_DATA = AttributeKey.valueOf("BuildMessageDispatcher.sessionData");

  private final Map<UUID, SessionData> mySessionDescriptors = new ConcurrentHashMap<>(16, 0.75f, 1);
  private final Set<UUID> myCanceledSessions = ContainerUtil.newConcurrentSet();

  public void registerBuildMessageHandler(@NotNull final RequestFuture<? extends BuilderMessageHandler> future, @Nullable CmdlineRemoteProto.Message.ControllerMessage params) {
    final BuilderMessageHandler wrappedHandler = new DelegatingMessageHandler() {
      @Override
      protected BuilderMessageHandler getDelegateHandler() {
        return future.getMessageHandler();
      }

      @Override
      public void sessionTerminated(@NotNull UUID sessionId) {
        try {
          super.sessionTerminated(sessionId);
        }
        finally {
          future.setDone();
        }
      }
    };
    final UUID sessionId = future.getRequestID();
    mySessionDescriptors.put(sessionId, new SessionData(sessionId, wrappedHandler, params));
  }

  @Nullable
  public BuilderMessageHandler unregisterBuildMessageHandler(UUID sessionId) {
    myCanceledSessions.remove(sessionId);
    final SessionData data = mySessionDescriptors.remove(sessionId);
    if (data == null) {
      return null;
    }
    final Channel channel = data.channel;
    if (channel != null) {
      // cleanup the attribute so that session data is not leaked
      channel.attr(SESSION_DATA).set(null);
    }
    return data.handler;
  }

  public void cancelSession(UUID sessionId) {
    if (myCanceledSessions.add(sessionId)) {
      final Channel channel = getConnectedChannel(sessionId);
      if (channel != null) {
        channel.writeAndFlush(CmdlineProtoUtil.toMessage(sessionId, CmdlineProtoUtil.createCancelCommand()));
      }
    }
  }

  @Nullable
  public Channel getConnectedChannel(@NotNull UUID sessionId) {
    final Channel channel = getAssociatedChannel(sessionId);
    return channel != null && channel.isActive()? channel : null;
  }

  @Nullable
  public Channel getAssociatedChannel(@NotNull UUID sessionId) {
    final SessionData data = mySessionDescriptors.get(sessionId);
    return data != null? data.channel : null;
  }

  public boolean sendBuildParameters(@NotNull final UUID preloadedSessionId, @NotNull CmdlineRemoteProto.Message.ControllerMessage params) {
    boolean succeeded = false;
    final SessionData sessionData = mySessionDescriptors.get(preloadedSessionId);
    if (sessionData != null) {
      //noinspection SynchronizationOnLocalVariableOrMethodParameter
      synchronized (sessionData) {
        if (sessionData.state == SessionData.State.WAITING_PARAMS) {
          sessionData.state = SessionData.State.RUNNING;
          final Channel channel = sessionData.channel;
          if (channel != null && channel.isActive()) {
            sessionData.handler.buildStarted(preloadedSessionId);
            channel.writeAndFlush(CmdlineProtoUtil.toMessage(preloadedSessionId, params));
            succeeded = true;
          }
        }
        else {
          if (sessionData.state == SessionData.State.INITIAL) {
            sessionData.params = params;
            succeeded = true;
          }
        }
      }
    }
    return succeeded;
  }

  @Override
  protected void messageReceived(ChannelHandlerContext context, CmdlineRemoteProto.Message message) {
    SessionData sessionData = context.channel().attr(SESSION_DATA).get();
    final boolean isFirstMessage = sessionData == null;
    final UUID sessionId;
    if (isFirstMessage) {
      // this is the first message for this session, so fill session data with missing info
      final CmdlineRemoteProto.Message.UUID id = message.getSessionId();
      sessionId = new UUID(id.getMostSigBits(), id.getLeastSigBits());

      sessionData = mySessionDescriptors.get(sessionId);
      if (sessionData != null) {
        sessionData.channel = context.channel();
        context.channel().attr(SESSION_DATA).set(sessionData);
      }
    }
    else {
      sessionId = sessionData.sessionId;
    }

    final BuilderMessageHandler handler = sessionData != null? sessionData.handler : null;
    try {
      if (handler == null) {
        if (!isBuilderEvent(message)) {
          // do not pollute logs, just silently skip events in case handler is missing
          LOG.info("No message handler registered for session " + sessionId);
        }
        return;
      }

      final CmdlineRemoteProto.Message.Type messageType = message.getType();
      switch (messageType) {
        case FAILURE:
          handler.handleFailure(sessionId, message.getFailure());
          break;

        case BUILDER_MESSAGE:
          final CmdlineRemoteProto.Message.BuilderMessage builderMessage = message.getBuilderMessage();
          final CmdlineRemoteProto.Message.BuilderMessage.Type msgType = builderMessage.getType();
          if (msgType == CmdlineRemoteProto.Message.BuilderMessage.Type.PARAM_REQUEST) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (sessionData) {
              final CmdlineRemoteProto.Message.ControllerMessage params = sessionData.params;
              if (params != null) {
                sessionData.state = SessionData.State.RUNNING;
                handler.buildStarted(sessionId);
                sessionData.params = null;
                context.writeAndFlush(CmdlineProtoUtil.toMessage(sessionId, params));
              }
              else {
                if (sessionData.state == SessionData.State.INITIAL) {
                  sessionData.state = SessionData.State.WAITING_PARAMS;
                }
                else {
                  // this message is expected to be sent only once.
                  // To be on the safe side, cancel the session
                  cancelSession(sessionId);
                }
              }
            }
          }
          else {
            handler.handleBuildMessage(context.channel(), sessionId, builderMessage);
          }
          break;

        default:
          LOG.info("Unsupported message type " + messageType);
          break;
      }
    }
    finally {
      if ((isFirstMessage && myCanceledSessions.contains(sessionId)) || (handler == null && !isBuilderEvent(message))) {
        // handle the case when the session had been cancelled before communication even started  
        // or if message handling is not possible due to missing handler
        context.channel().writeAndFlush(CmdlineProtoUtil.toMessage(sessionId, CmdlineProtoUtil.createCancelCommand()));
      }
    }
  }

  private static boolean isBuilderEvent(CmdlineRemoteProto.Message message) {
    return message.hasBuilderMessage() && message.getBuilderMessage().getType() == CmdlineRemoteProto.Message.BuilderMessage.Type.BUILD_EVENT;
  }

  @Override
  public void channelInactive(ChannelHandlerContext context) throws Exception {
    try {
      super.channelInactive(context);
    }
    finally {
      final SessionData sessionData = context.channel().attr(SESSION_DATA).get();
      if (sessionData != null) {
        final BuilderMessageHandler handler = unregisterBuildMessageHandler(sessionData.sessionId);
        if (handler != null) {
          // notify the handler only if it has not been notified yet
          handler.sessionTerminated(sessionData.sessionId);
        }
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
    if (cause != null) {
      LOG.info(cause);
    }
  }

  private static final class SessionData {
    enum State {
      INITIAL, WAITING_PARAMS, RUNNING
    }

    @NotNull
    final UUID sessionId;
    @NotNull
    final BuilderMessageHandler handler;
    volatile CmdlineRemoteProto.Message.ControllerMessage params;
    volatile Channel channel;
    State state = State.INITIAL;

    private SessionData(@NotNull UUID sessionId, @NotNull BuilderMessageHandler handler, CmdlineRemoteProto.Message.ControllerMessage params) {
      this.sessionId = sessionId;
      this.handler = handler;
      this.params = params;
    }
  }
}
