/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.inspections.klint;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.*;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converter which takes a PSI hierarchy for an XML file or document, and
 * creates a corresponding W3C DOM tree. It attempts to delegate as much
 * as possible to the original PSI tree. Note also that the {@link #getTextRange(Node)}}
 * method allows us to look up source offsets for the DOM nodes (which plain XML
 * DOM parsers do not).
 * <p>
 * NOTE: The tree may not be semantically equivalent to the XML PSI structure; this
 * converter only attempts to make the DOM correct as far as Lint cares (meaning that it
 * only worries about the details Lint cares about; currently this means it only wraps elements,
 * text and comment nodes.)
 */
class DomPsiConverter {
  private DomPsiConverter() {
  }

  /**
   * Convert the given {@link XmlFile} to a DOM tree
   *
   * @param xmlFile the file to be converted
   * @return a corresponding W3C DOM tree
   */
  @Nullable
  public static Document convert(@NotNull XmlFile xmlFile) {
    try {
      XmlDocument xmlDocument = xmlFile.getDocument();
      if (xmlDocument == null) {
        return null;
      }

      return convert(xmlDocument);
    }
    catch (ProcessCanceledException e) {
      // Ignore: common occurrence, e.g. we're running lint as part of an editor background
      // and while lint is running the user switches files: the inspections framework will
      // then cancel the process from within the PSI machinery (which asks the progress manager
      // periodically whether the operation is cancelled) and we find ourselves here
      return null;
    }
    catch (Exception e) {
      String path = xmlFile.getName();
      VirtualFile virtualFile = xmlFile.getVirtualFile();
      if (virtualFile != null) {
        path = virtualFile.getPath();
      }
      throw new RuntimeException("Could not convert file " + path, e);
    }
  }

  /**
   * Convert the given {@link XmlDocument} to a DOM tree
   *
   * @param document the document to be converted
   * @return a corresponding W3C DOM tree
   */
  @Nullable
  private static Document convert(@NotNull XmlDocument document)  {
    return new DomDocument(document);
  }

  /** Gets the {@link TextRange} for a {@link Node} created with this converter */
  @NotNull
  public static TextRange getTextRange(@NotNull Node node) {
    assert node instanceof DomNode;
    DomNode domNode = (DomNode)node;
    XmlElement element = domNode.myElement;

    // For elements, don't highlight the entire element range; instead, just
    // highlight the element name
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      return getTextNameRange(node);
    }

    return element.getTextRange();
  }

  /** Gets the {@link TextRange} for a {@link Node} created with this converter */
  @NotNull
  public static TextRange getTextNameRange(@NotNull Node node) {
    assert node instanceof DomNode;
    DomNode domNode = (DomNode)node;
    XmlElement element = domNode.myElement;

    // For elements and attributes, don't highlight the entire element range; instead, just
    // highlight the element name
    if (node.getNodeType() == Node.ELEMENT_NODE && element instanceof XmlTag) {
      String tag = node.getNodeName();
      int index = element.getText().indexOf(tag);
      if (index != -1) {
        TextRange textRange = element.getTextRange();
        int start = textRange.getStartOffset() + index;
        return new TextRange(start, start + tag.length());
      }
    } else if (node.getNodeType() == Node.ATTRIBUTE_NODE && element instanceof XmlAttribute) {
      XmlElement nameElement = ((XmlAttribute)element).getNameElement();
      if (nameElement != null) {
        return nameElement.getTextRange();
      }
    }

    return element.getTextRange();
  }

  /** Gets the {@link TextRange} for the value region of a {@link Node} created with this converter */
  @NotNull
  public static TextRange getTextValueRange(@NotNull Node node) {
    assert node instanceof DomNode;
    DomNode domNode = (DomNode)node;
    XmlElement element = domNode.myElement;
    TextRange textRange = element.getTextRange();

    // For attributes, don't highlight the entire element range; instead, just
    // highlight the value range
    if (node.getNodeType() == Node.ATTRIBUTE_NODE && element instanceof XmlAttribute) {
      XmlAttributeValue valueElement = ((XmlAttribute)element).getValueElement();
      if (valueElement != null) {
        return valueElement.getValueTextRange();
      }
    }

    return textRange;
  }

  private static final NodeList EMPTY = new NodeList() {
    @NotNull
    @Override
    public Node item(int i) {
      throw new IllegalArgumentException();
    }

    @Override
    public int getLength() {
      return 0;
    }
  };

  @Nullable
  private static final NamedNodeMap EMPTY_ATTRIBUTES = new NamedNodeMap() {
    @Override
    public int getLength() {
      return 0;
    }

    @Nullable
    @Override
    public Node getNamedItem(String s) {
      return null;
    }

    @Nullable
    @Override
    public Node getNamedItemNS(String s, String s2) throws DOMException {
      return null;
    }

    @NotNull
    @Override
    public Node setNamedItem(Node node) throws DOMException {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public Node removeNamedItem(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public Node item(int i) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Nullable
    @Override
    public Node setNamedItemNS(Node node) throws DOMException {
      return null;
    }

    @NotNull
    @Override
    public Node removeNamedItemNS(String s, String s2) throws DOMException {
      throw new UnsupportedOperationException(); // Not supported
    }
  };

  private static class DomNodeList implements NodeList {
    protected final List<DomNode> myChildren = new ArrayList<DomNode>();

    @NotNull
    @Override
    public Node item(int i) {
      return myChildren.get(i);
    }

    @Override
    public int getLength() {
      return myChildren.size();
    }

    void add(@NotNull DomNode node) {
      int size = myChildren.size();
      if (size > 0) {
        DomNode last = myChildren.get(size - 1);
        node.myPrevious = last;
        last.myNext = node;
      }
      myChildren.add(node);
    }
  }

  private static class DomNamedNodeMap implements NamedNodeMap {
    @NotNull protected final Map<String, DomNode> myMap;
    @NotNull protected final Map<String, Map<String, DomNode>> myNsMap;
    @NotNull protected final List<DomNode> mItems;

    private DomNamedNodeMap(@NotNull DomElement element, @NotNull XmlAttribute[] attributes) {
      int count = attributes.length;
      int namespaceCount = 0;
      for (XmlAttribute attribute : attributes) {
        if (!attribute.getNamespace().isEmpty()) {
          namespaceCount++;
        }
      }
      myMap = new HashMap<String, DomNode>(count - namespaceCount);
      myNsMap = new HashMap<String, Map<String, DomNode>>(namespaceCount);
      mItems = new ArrayList<DomNode>(count);

      assert element.myOwner != null; // True for elements, not true for non-Element nodes
      for (XmlAttribute attribute : attributes) {
        DomAttr attr = new DomAttr(element.myOwner, element, attribute);
        mItems.add(attr);
        String namespace = attribute.getNamespace();
        if (!namespace.isEmpty()) {
          Map<String, DomNode> map = myNsMap.get(namespace);
          if (map == null) {
            map = new HashMap<String, DomNode>();
            myNsMap.put(namespace, map);
          }
          map.put(attribute.getLocalName(), attr);
        } else {
          myMap.put(attribute.getName(), attr);
        }
      }
    }

    @Override
    public Node item(int i) {
      return mItems.get(i);
    }

    @Override
    public int getLength() {
      return mItems.size();
    }

    @Override
    public Node getNamedItem(@NotNull String s) {
      return myMap.get(s);
    }

    @Nullable
    @Override
    public Node getNamedItemNS(@NotNull String namespace, @NotNull String name) throws DOMException {
      Map<String, DomNode> map = myNsMap.get(namespace);
      if (map != null) {
        return map.get(name);
      }
      return null;
    }

    @NotNull
    @Override
    public Node setNamedItem(Node node) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Node removeNamedItem(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Node setNamedItemNS(Node node) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Node removeNamedItemNS(String s, String s2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }
  }

  @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"}) // Specifies methods shared by children
  private static abstract class DomNode implements Node {
    @Nullable protected final Document myOwner;
    @Nullable protected final DomNode myParent;
    @NotNull protected final XmlElement myElement;
    @Nullable protected NodeList myChildren;
    @Nullable protected DomNode myNext;
    @Nullable protected DomNode myPrevious;

    protected DomNode(@Nullable Document owner, @Nullable DomNode parent, @NotNull XmlElement element) {
      myOwner = owner;
      myParent = parent;
      myElement = element;
    }

    @Nullable
    @Override
    public Node getParentNode() {
      return myParent;
    }

    @NotNull
    @Override
    public NodeList getChildNodes() {
      if (myChildren == null) {
        PsiElement[] children = myElement.getChildren();
        if (children.length > 0) {
          DomNodeList list = new DomNodeList();
          myChildren = list;
          // True except for in DomDocument, which has custom getChildNodes
          assert myOwner != null;

          for (PsiElement child : children) {
            if (child instanceof XmlTag) {
              list.add(new DomElement(myOwner, this, (XmlTag) child));
            } else if (child instanceof XmlText) {
              list.add(new DomText(myOwner, this, (XmlText) child));
            } else if (child instanceof XmlComment) {
              list.add(new DomComment(myOwner, this, (XmlComment) child));
            } else {
              // Skipping other types for now; lint doesn't care about them.
              // TODO: Consider whether we need CDATA.
            }
          }
        } else {
          myChildren = EMPTY;
        }
      }
      return myChildren;
    }

    @Nullable
    @Override
    public Node getFirstChild() {
      NodeList childNodes = getChildNodes();
      if (childNodes.getLength() > 0) {
        return childNodes.item(0);
      }
      return null;
    }

    @Nullable
    @Override
    public Node getLastChild() {
      NodeList childNodes = getChildNodes();
      if (childNodes.getLength() > 0) {
        return childNodes.item(0);
      }
      return null;
    }

    @Nullable
    @Override
    public Node getPreviousSibling() {
      return myPrevious;
    }

    @Nullable
    @Override
    public Node getNextSibling() {
      return myNext;
    }

    @Nullable
    @Override
    public NamedNodeMap getAttributes() {
      throw new UnsupportedOperationException(); // Only supported on elements
    }

    @Nullable
    @Override
    public Document getOwnerDocument() {
      return myOwner;
    }

    @Override
    public void setNodeValue(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Node insertBefore(Node node, Node node2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Node replaceChild(Node node, Node node2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Node removeChild(Node node) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Node appendChild(Node node) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public boolean hasChildNodes() {
      return getChildNodes().getLength() > 0;
    }

    @NotNull
    @Override
    public Node cloneNode(boolean b) {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public void normalize() {
    }

    @Override
    public boolean isSupported(String s, String s2) {
      return false;
    }

    @NotNull
    @Override
    public String getNamespaceURI() {
      throw new UnsupportedOperationException(); // Only supported on elements in lint
    }

    @NotNull
    @Override
    public String getPrefix() {
      throw new UnsupportedOperationException(); // Only supported on elements in lint
    }

    @Override
    public void setPrefix(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Nullable
    @Override
    public String getLocalName() {
      return null;
    }

    @Override
    public boolean hasAttributes() {
      return false;
    }

    @Nullable
    @Override
    public String getBaseURI() {
      return null;
    }

    @Override
    public short compareDocumentPosition(Node node) throws DOMException {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public String getTextContent() throws DOMException {
      return myElement.getText();
    }

    @Override
    public void setTextContent(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public boolean isSameNode(Node node) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public String lookupPrefix(String s) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public boolean isDefaultNamespace(String s) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public String lookupNamespaceURI(String s) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public boolean isEqualNode(Node node) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public Object getFeature(String s, String s2) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public Object setUserData(String s, Object o, UserDataHandler userDataHandler) {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Object getUserData(String s) {
      throw new UnsupportedOperationException(); // Not supported
    }

    // From CharacterData

    @NotNull
    public String getData() throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    public void setData(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    public int getLength() {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    public String substringData(int i, int i2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    public void appendData(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    public void insertData(int i, String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    public void deleteData(int i, int i2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    public void replaceData(int i, int i2, String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }
  }

  private static class DomDocument extends DomNode implements Document {
    @NotNull private final XmlDocument myPsiDocument;
    @Nullable private DomElement myRoot;

    private DomDocument(@NotNull XmlDocument document) {
      super(null, null, document);
      myPsiDocument = document;
    }

    // From org.w3c.dom.Node:

    @Nullable
    @Override
    public String getNodeName() {
      return null;
    }

    @Nullable
    @Override
    public String getNodeValue() throws DOMException {
      return null;
    }

    @Override
    public short getNodeType() {
      return Node.DOCUMENT_NODE;
    }

    @NotNull
    @Override
    public NodeList getChildNodes() {
      if (myChildren == null) {
        DomNodeList list = new DomNodeList();
        myChildren = list;
        DomNode documentElement = (DomNode)getDocumentElement();
        if (documentElement != null) {
          list.add(documentElement);
        }
      }

      return myChildren;
    }

    // From org.w3c.dom.Document:

    @NotNull
    @Override
    public DocumentType getDoctype() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public DOMImplementation getImplementation() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Nullable
    @Override
    public Element getDocumentElement() {
      if (myRoot == null) {
        XmlTag rootTag = myPsiDocument.getRootTag();
        if (rootTag == null) {
          return null;
        }
        myRoot = new DomElement(this, this, rootTag);
      }

      return myRoot;
    }

    @NotNull
    @Override
    public NodeList getElementsByTagName(String s) {
      Element root = getDocumentElement();
      if (root != null) {
        return root.getElementsByTagName(s);
      }
      return EMPTY;
    }

    @NotNull
    @Override
    public NodeList getElementsByTagNameNS(String s, String s2) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public Element createElement(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public DocumentFragment createDocumentFragment() {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Text createTextNode(String s) {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Comment createComment(String s) {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public CDATASection createCDATASection(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public ProcessingInstruction createProcessingInstruction(String s, String s2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Attr createAttribute(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public EntityReference createEntityReference(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Node importNode(Node node, boolean b) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Element createElementNS(String s, String s2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Attr createAttributeNS(String s, String s2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Element getElementById(String s) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public String getInputEncoding() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public String getXmlEncoding() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public boolean getXmlStandalone() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public void setXmlStandalone(boolean b) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public String getXmlVersion() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public void setXmlVersion(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public boolean getStrictErrorChecking() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public void setStrictErrorChecking(boolean b) {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public String getDocumentURI() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public void setDocumentURI(String s) {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public Node adoptNode(Node node) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public DOMConfiguration getDomConfig() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public void normalizeDocument() {
    }

    @NotNull
    @Override
    public Node renameNode(Node node, String s, String s2) throws DOMException {
      throw new UnsupportedOperationException(); // Not supported
    }
  }

  private static class DomElement extends DomNode implements Element {
    private final XmlTag myTag;
    @Nullable private NamedNodeMap myAttributes;

    private DomElement(@NotNull Document owner, @NotNull DomNode parent, @NotNull XmlTag tag) {
      super(owner, parent, tag);
      myTag = tag;
    }

    // From org.w3c.dom.Node:

    @NotNull
    @Override
    public String getNodeName() {
      return getTagName();
    }

    @Nullable
    @Override
    public String getNodeValue() throws DOMException {
      return null;
    }

    @Override
    public short getNodeType() {
      return Node.ELEMENT_NODE;
    }

    @NotNull
    @Override
    public NamedNodeMap getAttributes() {
      Application application = ApplicationManager.getApplication();
      if (!application.isReadAccessAllowed()) {
        return application.runReadAction(new Computable<NamedNodeMap>() {
          @Override
          public NamedNodeMap compute() {
            return getAttributes();
          }
        });
      }

      if (myAttributes == null) {
        XmlAttribute[] attributes = myTag.getAttributes();
        if (attributes.length == 0) {
         myAttributes = EMPTY_ATTRIBUTES;
        } else {
          myAttributes = new DomNamedNodeMap(this, attributes);
        }
      }

      return myAttributes;
    }

    // From org.w3c.dom.Element:

    @NotNull
    @Override
    public String getTagName() {
      Application application = ApplicationManager.getApplication();
      if (!application.isReadAccessAllowed()) {
        return application.runReadAction(new Computable<String>() {
          @Override
          public String compute() {
            return getTagName();
          }
        });
      }

      return myTag.getName();
    }

    @NotNull
    @Override
    public String getAttribute(@NotNull String name) {
      Node node = getAttributes().getNamedItem(name);
      if (node != null) {
        return node.getNodeValue();
      }
      return "";
    }

    @NotNull
    @Override
    public String getAttributeNS(@NotNull String namespace, @NotNull String name) throws DOMException {
      Node node = getAttributes().getNamedItemNS(namespace, name);
      if (node != null) {
        return node.getNodeValue();
      }
      return "";
    }

    @Nullable
    @Override
    public Attr getAttributeNodeNS(@NotNull String namespace, @NotNull String name) throws DOMException {
      Node node = getAttributes().getNamedItemNS(namespace, name);
      if (node != null) {
        return (Attr)node;
      }
      return  null;
    }

    @Nullable
    @Override
    public Attr getAttributeNode(@NotNull String name) {
      Node node = getAttributes().getNamedItem(name);
      if (node != null) {
        return (Attr)node;
      }
      return  null;
    }

    @Override
    public boolean hasAttribute(@NotNull String name) {
      return getAttributes().getNamedItem(name) != null;
    }

    @Override
    public boolean hasAttributeNS(@NotNull String namespace, @NotNull String name) throws DOMException {
      return getAttributes().getNamedItemNS(namespace, name) != null;
    }

    @NotNull
    @Override
    public NodeList getElementsByTagName(@NotNull String s) {
      NodeList childNodes = getChildNodes();
      if (childNodes == EMPTY) {
        return EMPTY;
      }
      DomNodeList matches = new DomNodeList();
      for (int i = 0, n = childNodes.getLength(); i < n; i++) {
        Node node = childNodes.item(i);
        if (s.equals(node.getNodeName())) {
          matches.add((DomNode)node);
        }
      }

      return matches;
    }

    @NotNull
    @Override
    public NodeList getElementsByTagNameNS(String s, String s2) throws DOMException {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public Attr setAttributeNode(Attr attr) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Attr removeAttributeNode(Attr attr) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public void setAttributeNS(String s, String s2, String s3) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public void removeAttributeNS(String s, String s2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public void setAttribute(String s, String s2) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public void removeAttribute(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Attr setAttributeNodeNS(Attr attr) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public TypeInfo getSchemaTypeInfo() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public void setIdAttribute(String s, boolean b) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public void setIdAttributeNS(String s, String s2, boolean b) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public void setIdAttributeNode(Attr attr, boolean b) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }
  }

  private static class DomText extends DomNode implements Text {
    @NotNull private final XmlText myText;

    private DomText(@NotNull Document owner, @NotNull DomNode parent, @NotNull XmlText text) {
      super(owner, parent, text);
      myText = text;
    }

    // From org.w3c.dom.Node:

    @Nullable
    @Override
    public String getNodeName() {
      return null;
    }

    @NotNull
    @Override
    public String getNodeValue() throws DOMException {
      Application application = ApplicationManager.getApplication();
      if (!application.isReadAccessAllowed()) {
        return application.runReadAction(new Computable<String>() {
          @Override
          public String compute() {
            return getNodeValue();
          }
        });
      }

      return myText.getText();
    }

    @Override
    public short getNodeType() {
      return Node.TEXT_NODE;
    }

    // From org.w3c.dom.Text:

    @NotNull
    @Override
    public Text splitText(int i) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @Override
    public boolean isElementContentWhitespace() {
      String s = myText.getText();
      for (int i = 0, n = s.length(); i < n; i++) {
        if (!Character.isWhitespace(s.charAt(i))) {
          return false;
        }
      }

      return true;
    }

    @NotNull
    @Override
    public String getWholeText() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public Text replaceWholeText(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }
  }

  private static class DomComment extends DomNode implements Comment {
    @NotNull private final XmlComment myComment;

    private DomComment(@NotNull Document owner, @NotNull DomNode parent, @NotNull XmlComment comment) {
      super(owner, parent, comment);
      myComment = comment;
    }

    // From org.w3c.dom.Node:

    @Nullable
    @Override
    public String getNodeName() {
      return null;
    }

    @NotNull
    @Override
    public String getNodeValue() throws DOMException {
      Application application = ApplicationManager.getApplication();
      if (!application.isReadAccessAllowed()) {
        return application.runReadAction(new Computable<String>() {
          @Override
          public String compute() {
            return getNodeValue();
          }
        });
      }

      return myComment.getText();
    }

    @Override
    public short getNodeType() {
      return Node.COMMENT_NODE;
    }

    @NotNull
    @Override
    public String getTextContent() throws DOMException {
      return getNodeValue();
    }
  }

  private static class DomAttr extends DomNode implements Attr {
    @NotNull private final DomElement myOwner;
    @NotNull private final XmlAttribute myAttribute;

    private DomAttr(@NotNull Document document, @NotNull DomElement owner, @NotNull XmlAttribute attribute) {
      super(document, null, attribute);
      myOwner = owner;
      myAttribute = attribute;
    }

    // From org.w3c.dom.Node:

    @NotNull
    @Override
    public String getNodeName() {
      return getName();
    }

    @NotNull
    @Override
    public String getNodeValue() throws DOMException {
      return getValue();
    }

    @Override
    public short getNodeType() {
      return Node.ATTRIBUTE_NODE;
    }

    // From org.w3c.dom.Attr:

    @NotNull
    @Override
    public String getName() {
      Application application = ApplicationManager.getApplication();
      if (!application.isReadAccessAllowed()) {
        return application.runReadAction(new Computable<String>() {
          @Override
          public String compute() {
            return getName();
          }
        });
      }
      return myAttribute.getName();
    }

    @Override
    public boolean getSpecified() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @NotNull
    @Override
    public String getValue() {
      Application application = ApplicationManager.getApplication();
      if (!application.isReadAccessAllowed()) {
        return application.runReadAction(new Computable<String>() {
          @Override
          public String compute() {
            return getValue();
          }
        });
      }

      String value = myAttribute.getValue();
      if (value == null) {
        value = "";
      }
      return value;
    }

    @NotNull
    @Override
    public String getLocalName() {
      Application application = ApplicationManager.getApplication();
      if (!application.isReadAccessAllowed()) {
        return application.runReadAction(new Computable<String>() {
          @Override
          public String compute() {
            return getLocalName();
          }
        });
      }

      return myAttribute.getLocalName();
    }

    @NotNull
    @Override
    public String getPrefix() {
      Application application = ApplicationManager.getApplication();
      if (!application.isReadAccessAllowed()) {
        return application.runReadAction(new Computable<String>() {
          @Override
          public String compute() {
            return getPrefix();
          }
        });
      }

      return myAttribute.getNamespacePrefix();
    }

    @NotNull
    @Override
    public String getNamespaceURI() {
      Application application = ApplicationManager.getApplication();
      if (!application.isReadAccessAllowed()) {
        return application.runReadAction(new Computable<String>() {
          @Override
          public String compute() {
            return getNamespaceURI();
          }
        });
      }

      return myAttribute.getNamespace();
    }

    @Override
    public void setValue(String s) throws DOMException {
      throw new UnsupportedOperationException(); // Read-only bridge
    }

    @NotNull
    @Override
    public Element getOwnerElement() {
      return myOwner;
    }

    @NotNull
    @Override
    public TypeInfo getSchemaTypeInfo() {
      throw new UnsupportedOperationException(); // Not supported
    }

    @Override
    public boolean isId() {
      throw new UnsupportedOperationException(); // Not supported
    }
  }
}
