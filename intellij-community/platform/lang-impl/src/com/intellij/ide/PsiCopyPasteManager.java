// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide;

import com.intellij.ide.dnd.LinuxDragAndDropSupport;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public final class PsiCopyPasteManager {
  public static PsiCopyPasteManager getInstance() {
    return ServiceManager.getService(PsiCopyPasteManager.class);
  }

  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.PsiCopyPasteManagerImpl");

  private MyData myRecentData;
  private final CopyPasteManagerEx myCopyPasteManager;

  public PsiCopyPasteManager() {
    myCopyPasteManager = CopyPasteManagerEx.getInstanceEx();
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectClosing(@NotNull Project project) {
        if (myRecentData != null && (!myRecentData.isValid() || myRecentData.getProject() == project)) {
          myRecentData = null;
        }

        Transferable[] contents = myCopyPasteManager.getAllContents();
        for (int i = contents.length - 1; i >= 0; i--) {
          Transferable t = contents[i];
          if (t instanceof MyTransferable) {
            MyData myData = ((MyTransferable)t).myDataProxy;
            if (!myData.isValid() || myData.getProject() == project) {
              myCopyPasteManager.removeContent(t);
            }
          }
        }
      }
    });
  }

  @Nullable
  public PsiElement[] getElements(boolean[] isCopied) {
    try {
      Object transferData = myCopyPasteManager.getContents(ourDataFlavor);
      if (!(transferData instanceof MyData)) {
        return null;
      }
      MyData dataProxy = (MyData)transferData;
      if (!Comparing.equal(dataProxy, myRecentData)) {
        return null;
      }
      if (isCopied != null) {
        isCopied[0] = myRecentData.isCopied();
      }
      return myRecentData.getElements();
    }
    catch (Exception e) {
      LOG.debug(e);
      return null;
    }
  }

  @Nullable
  static PsiElement[] getElements(final Transferable content) {
    if (content == null) return null;
    Object transferData;
    try {
      transferData = content.getTransferData(ourDataFlavor);
    }
    catch (UnsupportedFlavorException | InvalidDnDOperationException | IOException e) {
      return null;
    }

    return transferData instanceof MyData ? ((MyData)transferData).getElements() : null;
  }

  public void clear() {
    myRecentData = null;
    myCopyPasteManager.setContents(new StringSelection(""));
  }

  public void setElements(PsiElement[] elements, boolean copied) {
    myRecentData = new MyData(elements, copied);
    myCopyPasteManager.setContents(new MyTransferable(myRecentData));
  }

  public boolean isCutElement(Object element) {
    if (myRecentData == null) return false;
    if (myRecentData.isCopied()) return false;
    PsiElement[] elements = myRecentData.getElements();
    if (elements == null) return false;
    for (PsiElement aElement : elements) {
      if (aElement == element) return true;
    }
    return false;
  }

  private static final DataFlavor ourDataFlavor;

  static {
    try {
      final Class<MyData> flavorClass = MyData.class;
      final Thread currentThread = Thread.currentThread();
      final ClassLoader currentLoader = currentThread.getContextClassLoader();
      try {
        currentThread.setContextClassLoader(flavorClass.getClassLoader());
        ourDataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + flavorClass.getName());
      }
      finally {
        currentThread.setContextClassLoader(currentLoader);
      }
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }


  public static class MyData {
    private PsiElement[] myElements;
    private final boolean myIsCopied;

    public MyData(PsiElement[] elements, boolean copied) {
      myElements = elements;
      myIsCopied = copied;
    }

    public PsiElement[] getElements() {
      if (myElements == null) return PsiElement.EMPTY_ARRAY;

      ReadAction.run(() -> {
        int validElementsCount = 0;
        for (PsiElement element : myElements) {
          if (element.isValid()) {
            validElementsCount++;
          }
        }

        if (validElementsCount != myElements.length) {
          PsiElement[] validElements = new PsiElement[validElementsCount];
          int j = 0;
          for (PsiElement element : myElements) {
            if (element.isValid()) {
              validElements[j++] = element;
            }
          }

          myElements = validElements;
        }
      });

      return myElements;
    }

    public boolean isCopied() {
      return myIsCopied;
    }

    public boolean isValid() {
      return myElements.length > 0 && myElements[0].isValid();
    }

    @Nullable
    public Project getProject() {
      if (myElements == null || myElements.length == 0) {
        return null;
      }
      final PsiElement element = myElements[0];
      return element.isValid() ? element.getProject() : null;
    }
  }

  public static class MyTransferable implements Transferable {
    private static final DataFlavor[] DATA_FLAVORS_COPY = {
      ourDataFlavor, DataFlavor.stringFlavor, DataFlavor.javaFileListFlavor,
      LinuxDragAndDropSupport.uriListFlavor, LinuxDragAndDropSupport.gnomeFileListFlavor
    };
    private static final DataFlavor[] DATA_FLAVORS_CUT = {
      ourDataFlavor, DataFlavor.stringFlavor, DataFlavor.javaFileListFlavor,
      LinuxDragAndDropSupport.uriListFlavor, LinuxDragAndDropSupport.gnomeFileListFlavor, LinuxDragAndDropSupport.kdeCutMarkFlavor
    };

    private final MyData myDataProxy;

    public MyTransferable(MyData data) {
      myDataProxy = data;
    }

    public MyTransferable(PsiElement[] selectedValues) {
      this(new PsiCopyPasteManager.MyData(selectedValues, true));
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
      Object result = getTransferDataOrNull(flavor);
      if (result == null) throw new IOException();
      return result;
    }

    @Nullable
    private Object getTransferDataOrNull(DataFlavor flavor) throws UnsupportedFlavorException {
      if (ourDataFlavor.equals(flavor)) {
        return myDataProxy;
      }
      else if (DataFlavor.stringFlavor.equals(flavor)) {
        return getDataAsText();
      }
      else if (DataFlavor.javaFileListFlavor.equals(flavor)) {
        return getDataAsFileList();
      }
      else if (flavor.equals(LinuxDragAndDropSupport.uriListFlavor)) {
        final List<File> files = getDataAsFileList();
        return files == null ? null : LinuxDragAndDropSupport.toUriList(files);
      }
      else if (flavor.equals(LinuxDragAndDropSupport.gnomeFileListFlavor)) {
        final List<File> files = getDataAsFileList();
        if (files == null) return null;
        final String string = (myDataProxy.isCopied() ? "copy\n" : "cut\n") + LinuxDragAndDropSupport.toUriList(files);
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
      }
      else if (flavor.equals(LinuxDragAndDropSupport.kdeCutMarkFlavor) && !myDataProxy.isCopied()) {
        return new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8));
      }
      throw new UnsupportedFlavorException(flavor);
    }

    @Nullable
    private String getDataAsText() {
      return ReadAction.compute(() -> {
        String names = Stream.of(myDataProxy.getElements())
          .filter(PsiNamedElement.class::isInstance)
          .map(e -> StringUtil.nullize(((PsiNamedElement)e).getName(), true))
          .filter(Objects::nonNull)
          .collect(Collectors.joining("\n"));
        return names.isEmpty() ? null : names;
      });
    }

    @Nullable
    private List<File> getDataAsFileList() {
      return ReadAction.compute(() -> asFileList(myDataProxy.getElements()));
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      DataFlavor[] flavors = myDataProxy.isCopied() ? DATA_FLAVORS_COPY : DATA_FLAVORS_CUT;
      return JBIterable.of(flavors).filter(flavor -> {
        try {
          return getTransferDataOrNull(flavor) != null;
        }
        catch (UnsupportedFlavorException ex) {
          return false;
        }
      }).toList().toArray(new DataFlavor[0]);
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return ArrayUtilRt.find(getTransferDataFlavors(), flavor) != -1;
    }

    public PsiElement[] getElements() {
      return myDataProxy.getElements();
    }
  }

  @Nullable
  public static List<File> asFileList(final PsiElement[] elements) {
    final List<File> result = new ArrayList<>();
    for (PsiElement element : elements) {
      final PsiFileSystemItem psiFile;
      if (element instanceof PsiFileSystemItem) {
        psiFile = (PsiFileSystemItem)element;
      }
      else if (element instanceof PsiDirectoryContainer) {
        final PsiDirectory[] directories = ((PsiDirectoryContainer)element).getDirectories();
        if (directories.length == 0) {
          LOG.error("No directories for " + element + " of " + element.getClass());
          return null;
        }
        psiFile = directories[0];
      }
      else {
        psiFile = element.getContainingFile();
      }
      if (psiFile != null) {
        VirtualFile vFile = psiFile.getVirtualFile();
        if (vFile != null && vFile.getFileSystem() instanceof LocalFileSystem) {
          result.add(new File(vFile.getPath()));
        }
      }
    }
    return result.isEmpty() ? null : result;
  }
}