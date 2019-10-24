// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.output;

import com.intellij.build.FilePosition;
import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.MessageEvent;
import com.intellij.build.events.impl.FileMessageEventImpl;
import com.intellij.build.events.impl.MessageEventImpl;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Parses javac's output.
 */
public class JavacOutputParser implements BuildOutputParser {
  private static final String COMPILER_MESSAGES_GROUP = "Compiler";

  private static final char COLON = ':';
  private static final String WARNING_PREFIX = "warning:"; // default value
  private static final String ERROR_PREFIX = "error:";
  private final String[] myFileExtensions;

  public JavacOutputParser() {
    this("java");
  }

  public JavacOutputParser(@NotNull String... fileExtensions) {
    myFileExtensions = fileExtensions;
  }

  @Override
  public boolean parse(@NotNull String line,
                       @NotNull BuildOutputInstantReader reader,
                       @NotNull Consumer<? super BuildEvent> messageConsumer) {
    int colonIndex1 = line.indexOf(COLON);
    if (colonIndex1 == 1) { // drive letter
      colonIndex1 = line.indexOf(COLON, colonIndex1 + 1);
    }

    if (colonIndex1 >= 0) { // looks like found something like a file path.
      String part1 = line.substring(0, colonIndex1).trim();
      if (part1.equalsIgnoreCase("error") /* jikes */ || part1.equalsIgnoreCase("Caused by")) {
        // +1 so we don't include the colon
        String text = line.substring(colonIndex1 + 1).trim();
        messageConsumer.accept(new MessageEventImpl(reader.getParentEventId(), MessageEvent.Kind.ERROR, COMPILER_MESSAGES_GROUP, text, line));
        return true;
      }
      if (part1.equalsIgnoreCase("warning")) {
        // +1 so we don't include the colon
        String text = line.substring(colonIndex1 + 1).trim();
        messageConsumer.accept(new MessageEventImpl(reader.getParentEventId(), MessageEvent.Kind.WARNING, COMPILER_MESSAGES_GROUP, text, line));
        return true;
      }
      if (part1.equalsIgnoreCase("javac")) {
        messageConsumer.accept(new MessageEventImpl(reader.getParentEventId(), MessageEvent.Kind.ERROR, COMPILER_MESSAGES_GROUP, line, line));
        return true;
      }
      if (part1.equalsIgnoreCase("Note")) {
        String message = line.substring(colonIndex1 + 1).trim();
        int javaFileExtensionIndex = message.indexOf(".java");
        if (javaFileExtensionIndex > 0) {
          File file = new File(message.substring(0, javaFileExtensionIndex + ".java".length()));
          if (file.isFile()) {
            message = message.substring(javaFileExtensionIndex + ".java".length() + 1);
            String detailedMessage = amendNextInfoLinesIfNeeded(file.getPath() + ":\n" + message, reader);
            messageConsumer.accept(new FileMessageEventImpl(reader.getParentEventId(), MessageEvent.Kind.INFO, COMPILER_MESSAGES_GROUP,
                                                            message, detailedMessage,
                                                            new FilePosition(file, 0, 0)));
            return true;
          }
        }
      }

      int colonIndex2 = line.indexOf(COLON, colonIndex1 + 1);
      if (colonIndex2 >= 0) {
        File file = new File(part1);
        if (!file.isFile()) {
          // the part one is not a file path.
          return false;
        }
        try {
          int lineNumber = Integer.parseInt(line.substring(colonIndex1 + 1, colonIndex2).trim()); // 1-based.
          String text = line.substring(colonIndex2 + 1).trim();
          MessageEvent.Kind kind = MessageEvent.Kind.ERROR;

          if (text.startsWith(WARNING_PREFIX)) {
            text = text.substring(WARNING_PREFIX.length()).trim();
            kind = MessageEvent.Kind.WARNING;
          }
          else if (text.startsWith(ERROR_PREFIX)) {
            text = text.substring(ERROR_PREFIX.length()).trim();
            kind = MessageEvent.Kind.ERROR;
          }

          // Only slurp up line pointer (^) information if this is required source files
          if (!isRelatedFile(file)) {
            return false;
          }

          BuildOutputCollector outputCollector = new BuildOutputCollector(reader);
          List<String> messageList = new ArrayList<>();
          messageList.add(text);
          int column; // 0-based.
          String prevLine = null;
          do {
            String nextLine = outputCollector.readLine();
            if (nextLine == null) {
              return false;
            }
            if (nextLine.trim().equals("^")) {
              column = nextLine.indexOf('^');
              String messageEnd = outputCollector.readLine();

              while (isMessageEnd(messageEnd)) {
                messageList.add(messageEnd.trim());
                messageEnd = outputCollector.readLine();
              }

              if (messageEnd != null) {
                outputCollector.pushBack();
              }
              break;
            }
            if (prevLine != null) {
              messageList.add(prevLine);
            }
            prevLine = nextLine;
          }
          while (true);

          if (column >= 0) {
            String message = StringUtil.join(convertMessages(messageList), "\n");
            String detailedMessage = line + "\n" + outputCollector.getOutput();
            messageConsumer.accept(new FileMessageEventImpl(reader.getParentEventId(), kind, COMPILER_MESSAGES_GROUP,
                                                            message, detailedMessage, new FilePosition(file, lineNumber - 1, column)));
            return true;
          }
        }
        catch (NumberFormatException ignored) {
        }
      }
    }

    if (line.endsWith("java.lang.OutOfMemoryError")) {
      messageConsumer.accept(new MessageEventImpl(reader.getParentEventId(), MessageEvent.Kind.ERROR, COMPILER_MESSAGES_GROUP,
                                                  "Out of memory.", line));
      return true;
    }

    return false;
  }

  private boolean isRelatedFile(File file) {
    String filePath = file.getPath();
    return Arrays.stream(myFileExtensions).anyMatch(extension -> FileUtilRt.extensionEquals(filePath, extension));
  }

  private static String amendNextInfoLinesIfNeeded(String str, BuildOutputInstantReader reader) {
    StringBuilder builder = new StringBuilder(str);
    String nextLine = reader.readLine();
    while (nextLine != null) {
      if (nextLine.startsWith("Note: ")) {
        int index = nextLine.indexOf(".java");
        if (index < 0) {
          builder.append("\n").append(nextLine.substring("Note: ".length()));
          nextLine = reader.readLine();
          continue;
        }
      }
      reader.pushBack();
      break;
    }
    return builder.toString();
  }

  @Contract("null -> false")
  private static boolean isMessageEnd(@Nullable String line) {
    return line != null && line.length() > 0 && Character.isWhitespace(line.charAt(0));
  }

  @NotNull
  private static List<String> convertMessages(@NotNull List<String> messages) {
    if (messages.size() <= 1) {
      return messages;
    }
    final String line0 = messages.get(0);
    final String line1 = messages.get(1);
    final int colonIndex = line1.indexOf(':');
    if (colonIndex > 0) {
      @NonNls String part1 = line1.substring(0, colonIndex).trim();
      // jikes
      if ("symbol".equals(part1)) {
        String symbol = line1.substring(colonIndex + 1).trim();
        messages.remove(1);
        if (messages.size() >= 2) {
          messages.remove(1);
        }
        messages.set(0, line0 + " " + symbol);
      }
    }
    return messages;
  }
}
