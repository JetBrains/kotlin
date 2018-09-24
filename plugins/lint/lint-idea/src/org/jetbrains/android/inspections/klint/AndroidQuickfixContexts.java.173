package org.jetbrains.android.inspections.klint;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

public class AndroidQuickfixContexts {
  public static abstract class Context {
    private final ContextType myType;

    private Context(@NotNull ContextType type) {
      myType = type;
    }

    @NotNull
    public ContextType getType() {
      return myType;
    }
  }

  public static class ContextType {
    private ContextType() {
    }
  }

  public static class BatchContext extends Context {
    public static final ContextType TYPE = new ContextType();
    private static final BatchContext INSTANCE = new BatchContext();

    private BatchContext() {
      super(TYPE);
    }

    @NotNull
    public static BatchContext getInstance() {
      return INSTANCE;
    }
  }

  public static class EditorContext extends Context {
    public static final ContextType TYPE = new ContextType();
    private final Editor myEditor;

    private EditorContext(@NotNull Editor editor) {
      super(TYPE);
      myEditor = editor;
    }

    @NotNull
    public Editor getEditor() {
      return myEditor;
    }

    @NotNull
    public static EditorContext getInstance(@NotNull Editor editor) {
      return new EditorContext(editor);
    }
  }

  public static class DesignerContext extends Context {
    public static final ContextType TYPE = new ContextType();
    private static final DesignerContext INSTANCE = new DesignerContext();

    private DesignerContext() {
      super(TYPE);
    }

    @NotNull
    public static DesignerContext getInstance() {
      return INSTANCE;
    }
  }
}
