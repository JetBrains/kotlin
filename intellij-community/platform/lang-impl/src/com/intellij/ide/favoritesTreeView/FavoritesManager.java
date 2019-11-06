// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.favoritesTreeView.actions.AddToFavoritesAction;
import com.intellij.ide.projectView.impl.*;
import com.intellij.ide.projectView.impl.nodes.LibraryGroupElement;
import com.intellij.ide.projectView.impl.nodes.NamedLibraryElement;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.TreeItem;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import static com.intellij.ide.favoritesTreeView.FavoritesListProvider.EP_NAME;

public final class FavoritesManager implements ProjectComponent, JDOMExternalizable {
  // fav list name -> list of (root: root url, root class)
  private final Map<String, List<TreeItem<Pair<AbstractUrl, String>>>> myName2FavoritesRoots =
    new TreeMap<>();
  private final List<String> myFavoritesRootsOrder = new ArrayList<>();
  private final Map<String, String> myDescriptions = new HashMap<>();
  private final Project myProject;
  private final List<FavoritesListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final FavoritesViewSettings myViewSettings = new FavoritesViewSettings();
  private Map<String, FavoritesListProvider> myProviders;

  @NotNull
  private Map<String, FavoritesListProvider> getProviders() {
    if (myProviders != null) return myProviders;
    myProviders = new HashMap<>();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      final FavoritesListProvider[] providers = EP_NAME.getExtensions(myProject);
      for (FavoritesListProvider provider : providers) {
        myProviders.put(provider.getListName(myProject), provider);
      }
      final MyRootsChangeAdapter myPsiTreeChangeAdapter = new MyRootsChangeAdapter();

      PsiManager.getInstance(myProject).addPsiTreeChangeListener(myPsiTreeChangeAdapter, myProject);
      if (myName2FavoritesRoots.isEmpty()) {
        myDescriptions.put(myProject.getName(), "auto-added");
        createNewList(myProject.getName());
      }
    }
    return myProviders;
  }

  private void rootsChanged() {
    for (FavoritesListener listener : myListeners) {
      listener.rootsChanged();
    }
  }

  private void listAdded(@NotNull String listName) {
    for (FavoritesListener listener : myListeners) {
      listener.listAdded(listName);
    }
  }

  private void listRemoved(@NotNull String listName) {
    for (FavoritesListener listener : myListeners) {
      listener.listRemoved(listName);
    }
  }

  public void renameList(final Project project, @NotNull String listName) {
    final String newName = Messages
      .showInputDialog(project, IdeBundle.message("prompt.input.favorites.list.new.name", listName), IdeBundle.message("title.rename.favorites.list"),
                       Messages.getInformationIcon(), listName, new InputValidator() {
        @Override
        public boolean checkInput(String inputString) {
          return inputString != null && inputString.trim().length() > 0;
        }

        @Override
        public boolean canClose(String inputString) {
          inputString = inputString.trim();
          if (myName2FavoritesRoots.keySet().contains(inputString) || getProviders().keySet().contains(inputString)) {
            Messages.showErrorDialog(project, IdeBundle.message("error.favorites.list.already.exists", inputString.trim()),
                                     IdeBundle.message("title.unable.to.add.favorites.list"));
            return false;
          }
          return !inputString.isEmpty();
        }
      });

    if (newName != null && renameFavoritesList(listName, newName)) {
      rootsChanged();
    }
  }

  public void addFavoritesListener(final FavoritesListener listener, @NotNull Disposable parent) {
    myListeners.add(listener);
    listener.rootsChanged();
    Disposer.register(parent, new Disposable() {
      @Override
      public void dispose() {
        myListeners.remove(listener);
      }
    });
  }

  List<AbstractTreeNode> createRootNodes() {
    List<AbstractTreeNode> result = new ArrayList<>();
    for (String listName : myFavoritesRootsOrder) {
      result.add(new FavoritesListNode(myProject, listName, myDescriptions.get(listName)));
    }
    ArrayList<FavoritesListProvider> providers = new ArrayList<>(getProviders().values());
    Collections.sort(providers);
    for (FavoritesListProvider provider : providers) {
      result.add(provider.createFavoriteListNode(myProject));
    }
    return result;
  }

  public static FavoritesManager getInstance(Project project) {
    return project.getComponent(FavoritesManager.class);
  }

  public FavoritesManager(Project project) {
    myProject = project;
  }

  @NotNull
  public List<String> getAvailableFavoritesListNames() {
    return new ArrayList<>(myFavoritesRootsOrder);
  }

  public synchronized void createNewList(@NotNull String listName) {
    myName2FavoritesRoots.put(listName, new ArrayList<>());
    myFavoritesRootsOrder.add(listName);
    listAdded(listName);
  }

  public synchronized void fireListeners(@NotNull final String listName) {
    rootsChanged();
  }

  @NotNull
  public FavoritesViewSettings getViewSettings() {
    return myViewSettings;
  }

  public synchronized boolean removeFavoritesList(@NotNull String name) {
    boolean result = myName2FavoritesRoots.remove(name) != null;
    myFavoritesRootsOrder.remove(name);
    myDescriptions.remove(name);
    listRemoved(name);
    return result;
  }

  @NotNull
  public List<TreeItem<Pair<AbstractUrl, String>>> getFavoritesListRootUrls(@NotNull String name) {
    final List<TreeItem<Pair<AbstractUrl, String>>> pairs = myName2FavoritesRoots.get(name);
    return pairs == null ? new ArrayList<>() : pairs;
  }

  public synchronized boolean addRoots(@NotNull String name, Module moduleContext, @NotNull Object elements) {
    Collection<AbstractTreeNode> nodes = AddToFavoritesAction.createNodes(myProject, moduleContext, elements, true, getViewSettings());
    return !nodes.isEmpty() && addRoots(name, nodes);
  }

  public synchronized Comparator<FavoritesTreeNodeDescriptor> getCustomComparator(@NotNull final String name) {
    return getProviders().get(name);
  }

  private Pair<AbstractUrl, String> createPairForNode(AbstractTreeNode node) {
    final String className = node.getClass().getName();
    final Object value = node.getValue();
    final AbstractUrl url = createUrlByElement(value, myProject);
    if (url == null) return null;
    return Pair.create(url, className);
  }

  public boolean addRoots(final String name, final Collection<? extends AbstractTreeNode> nodes) {
    final Collection<TreeItem<Pair<AbstractUrl, String>>> list = getFavoritesListRootUrls(name);

    final HashSet<AbstractUrl> set =
      new HashSet<>(ContainerUtil.map(list, item -> item.getData().getFirst()));
    for (AbstractTreeNode node : nodes) {
      final Pair<AbstractUrl, String> pair = createPairForNode(node);
      if (pair != null) {
        if (set.contains(pair.getFirst())) continue;
        final TreeItem<Pair<AbstractUrl, String>> treeItem = new TreeItem<>(pair);
        list.add(treeItem);
        set.add(pair.getFirst());
        appendChildNodes(node, treeItem);
      }
    }
    rootsChanged();
    return true;
  }

  public boolean canAddRoots(@NotNull String name, @NotNull Collection<? extends AbstractTreeNode> nodes) {
    final Collection<TreeItem<Pair<AbstractUrl, String>>> list = getFavoritesListRootUrls(name);

    final HashSet<AbstractUrl> set =
      new HashSet<>(ContainerUtil.map(list, item -> item.getData().getFirst()));
    for (AbstractTreeNode node : nodes) {
      final Pair<AbstractUrl, String> pair = createPairForNode(node);
      if (pair != null && !set.contains(pair.getFirst())) return true;
    }
    return false;
  }

  private void appendChildNodes(AbstractTreeNode node, TreeItem<Pair<AbstractUrl, String>> treeItem) {
    final Collection<? extends AbstractTreeNode> children = node.getChildren();
    for (AbstractTreeNode child : children) {
      final TreeItem<Pair<AbstractUrl, String>> childTreeItem = new TreeItem<>(createPairForNode(child));
      treeItem.addChild(childTreeItem);
      appendChildNodes(child, childTreeItem);
    }
  }

  private <T> boolean findListToRemoveFrom(@NotNull String name, @NotNull final List<T> elements,
                                           final Function<? super T, ? extends AbstractUrl> convertor) {
    Collection<TreeItem<Pair<AbstractUrl, String>>> list = getFavoritesListRootUrls(name);
    if (elements.size() > 1) {
      final List<T> sublist = elements.subList(0, elements.size() - 1);
      for (T obj : sublist) {
        AbstractUrl objUrl = convertor.apply(obj);
        final TreeItem<Pair<AbstractUrl, String>> item = findNextItem(objUrl, list);
        if (item == null || item.getChildren() == null) return false;
        list = item.getChildren();
      }
    }

    TreeItem<Pair<AbstractUrl, String>> found = null;
    AbstractUrl url = convertor.apply(elements.get(elements.size() - 1));
    if (url == null) return false;
    for (TreeItem<Pair<AbstractUrl, String>> pair : list) {
      if (url.equals(pair.getData().getFirst())) {
        found = pair;
        break;
      }
    }

    if (found != null) {
      list.remove(found);
      rootsChanged();
      return true;
    }
    return false;
  }

  public synchronized boolean removeRoot(@NotNull String name, @NotNull List<? extends AbstractTreeNode> elements) {
    Function<AbstractTreeNode, AbstractUrl> convertor = obj -> createUrlByElement(obj.getValue(), myProject);
    boolean result = true;
    for (AbstractTreeNode element : elements) {
      final List<AbstractTreeNode> path = TaskDefaultFavoriteListProvider.getPathToUsualNode(element);
      result &= findListToRemoveFrom(name, path.subList(1, path.size()), convertor);
    }
    return result;
  }

  private static TreeItem<Pair<AbstractUrl, String>> findNextItem(AbstractUrl url, Collection<? extends TreeItem<Pair<AbstractUrl, String>>> list) {
    for (TreeItem<Pair<AbstractUrl, String>> pair : list) {
      if (url.equals(pair.getData().getFirst())) {
        return pair;
      }
    }
    return null;
  }

  private boolean renameFavoritesList(@NotNull String oldName, @NotNull String newName) {
    List<TreeItem<Pair<AbstractUrl, String>>> list = myName2FavoritesRoots.remove(oldName);
    int index = myFavoritesRootsOrder.indexOf(oldName);
    if (index != -1 && newName.length() > 0) {
      myFavoritesRootsOrder.remove(oldName);
      myFavoritesRootsOrder.remove(newName);
      myFavoritesRootsOrder.add(index, newName);
    }
    if (list != null && newName.length() > 0) {
      myName2FavoritesRoots.put(newName, list);
      String description = myDescriptions.remove(oldName);
      if (description != null) {
        myDescriptions.put(newName, description);
      }
      rootsChanged();
      return true;
    }
    return false;
  }

  public void setOrder(@NotNull String nameToOrder, String anchorName, boolean above) {
    if (!canReorder(nameToOrder, anchorName, above)) return;
    int index = myFavoritesRootsOrder.indexOf(anchorName);
    int toRemove = myFavoritesRootsOrder.indexOf(nameToOrder);
    myFavoritesRootsOrder.add(above? index : index +1, nameToOrder);
    myFavoritesRootsOrder.remove(toRemove > index ? toRemove+1 : toRemove);
    rootsChanged();
  }

  public boolean canReorder(String nameToOrder, String anchorName, boolean above) {
    int index = myFavoritesRootsOrder.indexOf(anchorName);
    int toReorder = myFavoritesRootsOrder.indexOf(nameToOrder);
    if (index ==-1 || toReorder ==-1 || index == toReorder) return false;
    if (toReorder == index -1 && above) return false;
    if (toReorder == index + 1 && !above) return false;
    return true;
  }

  @Override
  @NotNull
  public String getComponentName() {
    return "FavoritesManager";
  }

  @Nullable
  public FavoritesListProvider getListProvider(@Nullable String name) {
    return getProviders().get(name);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    myName2FavoritesRoots.clear();
    for (Element list : element.getChildren(ELEMENT_FAVORITES_LIST)) {
      final String name = list.getAttributeValue(ATTRIBUTE_NAME);
      List<TreeItem<Pair<AbstractUrl, String>>> roots = readRoots(list, myProject);
      myName2FavoritesRoots.put(name, roots);
      myFavoritesRootsOrder.add(name);
    }
    DefaultJDOMExternalizer.readExternal(this, element);
  }

  @NonNls private static final String CLASS_NAME = "klass";
  @NonNls private static final String FAVORITES_ROOT = "favorite_root";
  @NonNls private static final String ELEMENT_FAVORITES_LIST = "favorites_list";
  @NonNls private static final String ATTRIBUTE_NAME = "name";

  private static List<TreeItem<Pair<AbstractUrl, String>>> readRoots(final Element list, Project project) {
    List<TreeItem<Pair<AbstractUrl, String>>> result = new ArrayList<>();
    readFavoritesOneLevel(list, project, result);
    return result;
  }

  private static void readFavoritesOneLevel(Element list, Project project, Collection<? super TreeItem<Pair<AbstractUrl, String>>> result) {
    for (Element favorite : list.getChildren(FAVORITES_ROOT)) {
      final String className = favorite.getAttributeValue(CLASS_NAME);
      final AbstractUrl abstractUrl = readUrlFromElement(favorite, project);
      if (abstractUrl != null) {
        final TreeItem<Pair<AbstractUrl, String>> treeItem = new TreeItem<>(Pair.create(abstractUrl, className));
        result.add(treeItem);
        readFavoritesOneLevel(favorite, project, treeItem.getChildren());
      }
    }
  }

  private static final ArrayList<AbstractUrl> ourAbstractUrlProviders = new ArrayList<>();

  static {
    ourAbstractUrlProviders.add(new ModuleUrl(null, null));
    ourAbstractUrlProviders.add(new DirectoryUrl(null, null));

    ourAbstractUrlProviders.add(new ModuleGroupUrl(null));

    ourAbstractUrlProviders.add(new PsiFileUrl(null));
    ourAbstractUrlProviders.add(new LibraryModuleGroupUrl(null));
    ourAbstractUrlProviders.add(new NamedLibraryUrl(null, null));
  }

  @NonNls private static final String ATTRIBUTE_TYPE = "type";
  @NonNls private static final String ATTRIBUTE_URL = "url";
  @NonNls private static final String ATTRIBUTE_MODULE = "module";

  @Nullable
  private static AbstractUrl readUrlFromElement(Element element, Project project) {
    final String type = element.getAttributeValue(ATTRIBUTE_TYPE);
    final String urlValue = element.getAttributeValue(ATTRIBUTE_URL);
    final String moduleName = element.getAttributeValue(ATTRIBUTE_MODULE);

    for (FavoriteNodeProvider nodeProvider : FavoriteNodeProvider.EP_NAME.getExtensions(project)) {
      if (nodeProvider.getFavoriteTypeId().equals(type)) {
        return new AbstractUrlFavoriteAdapter(urlValue, moduleName, nodeProvider);
      }
    }

    for (AbstractUrl urlProvider : ourAbstractUrlProviders) {
      AbstractUrl url = urlProvider.createUrl(type, moduleName, urlValue);
      if (url != null) return url;
    }
    return null;
  }


  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    for (final String name : myFavoritesRootsOrder) {
      Element list = new Element(ELEMENT_FAVORITES_LIST);
      list.setAttribute(ATTRIBUTE_NAME, name);
      writeRoots(list, myName2FavoritesRoots.get(name));
      element.addContent(list);
    }
    DefaultJDOMExternalizer.writeExternal(this, element);
  }

  @Nullable
  public static AbstractUrl createUrlByElement(Object element, final Project project) {
    if (element instanceof SmartPsiElementPointer) element = ((SmartPsiElementPointer)element).getElement();

    for (FavoriteNodeProvider nodeProvider : FavoriteNodeProvider.EP_NAME.getExtensions(project)) {
      String url = nodeProvider.getElementUrl(element);
      if (url != null) {
        return new AbstractUrlFavoriteAdapter(url, nodeProvider.getElementModuleName(element), nodeProvider);
      }
    }

    for (AbstractUrl urlProvider : ourAbstractUrlProviders) {
      AbstractUrl url = urlProvider.createUrlByElement(element);
      if (url != null) return url;
    }
    return null;
  }

  private static void writeRoots(Element element, Collection<? extends TreeItem<Pair<AbstractUrl, String>>> roots) {
    for (TreeItem<Pair<AbstractUrl, String>> root : roots) {
      final AbstractUrl url = root.getData().getFirst();
      if (url == null) continue;
      final Element list = new Element(FAVORITES_ROOT);
      url.write(list);
      list.setAttribute(CLASS_NAME, root.getData().getSecond());
      element.addContent(list);
      final List<TreeItem<Pair<AbstractUrl, String>>> children = root.getChildren();
      if (children != null && !children.isEmpty()) {
        writeRoots(list, children);
      }
    }
  }

  public String getFavoriteListName(@Nullable final String currentSubId, @NotNull final VirtualFile vFile) {
    if (currentSubId != null && contains(currentSubId, vFile)) {
      return currentSubId;
    }
    for (String listName : myName2FavoritesRoots.keySet()) {
      if (contains(listName, vFile)) {
        return listName;
      }
    }
    return null;
  }

  // currently only one level here..
  public boolean contains(@NotNull String name, @NotNull final VirtualFile vFile) {
    final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    final Set<Boolean> find = new HashSet<>();
    final ContentIterator contentIterator = fileOrDir -> {
      if (fileOrDir != null && fileOrDir.getPath().equals(vFile.getPath())) {
        find.add(Boolean.TRUE);
      }
      return true;
    };

    Collection<TreeItem<Pair<AbstractUrl, String>>> urls = getFavoritesListRootUrls(name);
    for (TreeItem<Pair<AbstractUrl, String>> pair : urls) {
      AbstractUrl abstractUrl = pair.getData().getFirst();
      if (abstractUrl == null) {
        continue;
      }
      final Object[] path = abstractUrl.createPath(myProject);
      if (path == null || path.length < 1 || path[0] == null) {
        continue;
      }
      Object element = path[path.length - 1];
      if (element instanceof SmartPsiElementPointer) {
        final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(((SmartPsiElementPointer)element).getElement());
        if (virtualFile == null) continue;
        if (vFile.getPath().equals(virtualFile.getPath())) {
          return true;
        }
        if (!virtualFile.isDirectory()) {
          continue;
        }
        projectFileIndex.iterateContentUnderDirectory(virtualFile, contentIterator);
      }

      if (element instanceof PsiElement) {
        final VirtualFile virtualFile = PsiUtilCore.getVirtualFile((PsiElement)element);
        if (virtualFile == null) continue;
        if (vFile.getPath().equals(virtualFile.getPath())) {
          return true;
        }
        if (!virtualFile.isDirectory()) {
          continue;
        }
        projectFileIndex.iterateContentUnderDirectory(virtualFile, contentIterator);
      }
      if (element instanceof Module) {
        ModuleRootManager.getInstance((Module)element).getFileIndex().iterateContent(contentIterator);
      }
      if (element instanceof LibraryGroupElement) {
        final boolean inLibrary =
          ModuleRootManager.getInstance(((LibraryGroupElement)element).getModule()).getFileIndex().isInContent(vFile) &&
          projectFileIndex.isInLibraryClasses(vFile);
        if (inLibrary) {
          return true;
        }
      }
      if (element instanceof NamedLibraryElement) {
        NamedLibraryElement namedLibraryElement = (NamedLibraryElement)element;
        final VirtualFile[] files = namedLibraryElement.getOrderEntry().getRootFiles(OrderRootType.CLASSES);
        if (ArrayUtil.find(files, vFile) > -1) {
          return true;
        }
      }
      if (element instanceof ModuleGroup) {
        ModuleGroup group = (ModuleGroup)element;
        final Collection<Module> modules = group.modulesInGroup(myProject, true);
        for (Module module : modules) {
          ModuleRootManager.getInstance(module).getFileIndex().iterateContent(contentIterator);
        }
      }


      for (FavoriteNodeProvider provider : FavoriteNodeProvider.EP_NAME.getExtensions(myProject)) {
        if (provider.elementContainsFile(element, vFile)) {
          return true;
        }
      }

      if (!find.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static void iterateTreeItems(final Collection<? extends TreeItem<Pair<AbstractUrl, String>>> coll,
                                       Consumer<? super TreeItem<Pair<AbstractUrl, String>>> consumer) {
    final ArrayDeque<TreeItem<Pair<AbstractUrl, String>>> queue = new ArrayDeque<>(coll);
    while (!queue.isEmpty()) {
      final TreeItem<Pair<AbstractUrl, String>> item = queue.removeFirst();
      consumer.consume(item);
      final List<TreeItem<Pair<AbstractUrl, String>>> children = item.getChildren();
      if (children != null && !children.isEmpty()) {
        queue.addAll(children);
      }
    }
  }

  protected Collection<VirtualFile> getVirtualFiles(String listName, boolean recursively) {
    if (getListProvider(listName) != null) return Collections.emptyList();
    Collection<VirtualFile> result = new SmartList<>();
    final List<TreeItem<Pair<AbstractUrl, String>>> roots = myName2FavoritesRoots.get(listName);
    if (!recursively) {
      for (TreeItem<Pair<AbstractUrl, String>> item : roots) {
        VirtualFile file = getVirtualFile(item);
        if (file != null) {
          result.add(file);
        }
      }
    } else {
      iterateTreeItems(roots, item -> {
        VirtualFile file = getVirtualFile(item);
        if (file != null) {
          result.add(file);
        }
      });
    }
    return result;
  }

  @Nullable
  private VirtualFile getVirtualFile(TreeItem<Pair<AbstractUrl, String>> item) {
    Pair<AbstractUrl, String> data = item.getData();
    Object[] path = data.first.createPath(myProject);
    if (path != null && path.length == 1) {
      if (path[0] instanceof PsiFile) {
        VirtualFile virtualFile = ((PsiFile)path[0]).getVirtualFile();
        if (virtualFile != null && !virtualFile.isDirectory()) {
          return virtualFile;
        }
      }
      if (path[0] instanceof File) {
        VirtualFile virtualFile = VfsUtil.findFileByIoFile((File)path[0], false);
        if (virtualFile != null && !virtualFile.isDirectory()) {
          return virtualFile;
        }
      }
    }
    return null;
  }

  private class MyRootsChangeAdapter extends PsiTreeChangeAdapter {
    @Override
    public void beforeChildMovement(@NotNull final PsiTreeChangeEvent event) {
      final PsiElement oldParent = event.getOldParent();
      final PsiElement newParent = event.getNewParent();
      final PsiElement child = event.getChild();
      if (newParent instanceof PsiDirectory) {
        final Module module = ModuleUtilCore.findModuleForPsiElement(newParent);
        if (module == null) return;
        AbstractUrl childUrl = null;
        if (child instanceof PsiFile) {
          childUrl = new PsiFileUrl(((PsiDirectory)newParent).getVirtualFile().getUrl() + "/" + ((PsiFile)child).getName());
        }
        else if (child instanceof PsiDirectory) {
          childUrl =
            new DirectoryUrl(((PsiDirectory)newParent).getVirtualFile().getUrl() + "/" + ((PsiDirectory)child).getName(), module.getName());
        }

        for (String listName : myFavoritesRootsOrder) {
          final List<TreeItem<Pair<AbstractUrl, String>>> roots = myName2FavoritesRoots.get(listName);
          final AbstractUrl finalChildUrl = childUrl;
          iterateTreeItems(roots, item -> {
            final Pair<AbstractUrl, String> root = item.getData();
            final Object[] path = root.first.createPath(myProject);
            if (path == null || path.length < 1 || path[0] == null) {
              return;
            }
            final Object element = path[path.length - 1];
            if (element == child && finalChildUrl != null) {
              item.setData(Pair.create(finalChildUrl, root.second));
            }
            else {
              if (element == oldParent) {
                item.setData(Pair.create(root.first.createUrlByElement(newParent), root.second));
              }
            }
          });
        }
      }
    }

    @Override
    public void beforePropertyChange(@NotNull final PsiTreeChangeEvent event) {
      if (event.getPropertyName().equals(PsiTreeChangeEvent.PROP_FILE_NAME) ||
          event.getPropertyName().equals(PsiTreeChangeEvent.PROP_DIRECTORY_NAME)) {
        final PsiElement psiElement = event.getChild();
        if (psiElement instanceof PsiFile || psiElement instanceof PsiDirectory) {
          final Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
          if (module == null) return;
          final String url = ((PsiDirectory)psiElement.getParent()).getVirtualFile().getUrl() + "/" + event.getNewValue();
          final AbstractUrl childUrl = psiElement instanceof PsiFile ? new PsiFileUrl(url) : new DirectoryUrl(url, module.getName());

          for (String listName : myFavoritesRootsOrder) {
            final List<TreeItem<Pair<AbstractUrl, String>>> roots = myName2FavoritesRoots.get(listName);
            iterateTreeItems(roots, item -> {
              final Pair<AbstractUrl, String> root = item.getData();
              final Object[] path = root.first.createPath(myProject);
              if (path == null || path.length < 1 || path[0] == null) {
                return;
              }
              final Object element = path[path.length - 1];
              if (element == psiElement && psiElement instanceof PsiFile) {
                item.setData(Pair.create(childUrl, root.second));
              }
              else {
                item.setData(root);
              }
            });
          }
        }
      }
    }
  }
}
