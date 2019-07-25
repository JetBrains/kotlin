/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.uiDesigner.lw;

import com.intellij.uiDesigner.UIFormXmlConstants;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jdom.Element;

import java.awt.*;

/**
 * @author yole
 */
public class GridLayoutSerializer extends LayoutSerializer {
  protected GridLayoutSerializer() {
  }

  public static GridLayoutSerializer INSTANCE = new GridLayoutSerializer();

  @Override
  void readLayout(Element element, LwContainer container) {
    final int rowCount = LwXmlReader.getRequiredInt(element, UIFormXmlConstants.ATTRIBUTE_ROW_COUNT);
    final int columnCount = LwXmlReader.getRequiredInt(element, UIFormXmlConstants.ATTRIBUTE_COLUMN_COUNT);

    final int hGap = LwXmlReader.getOptionalInt(element, UIFormXmlConstants.ATTRIBUTE_HGAP, -1);
    final int vGap = LwXmlReader.getOptionalInt(element, UIFormXmlConstants.ATTRIBUTE_VGAP, -1);

    // attribute is optional for compatibility with IDEA 4.0 forms
    final boolean sameSizeHorizontally = LwXmlReader.getOptionalBoolean(element, UIFormXmlConstants.ATTRIBUTE_SAME_SIZE_HORIZONTALLY, false);
    final boolean sameSizeVertically = LwXmlReader.getOptionalBoolean(element, UIFormXmlConstants.ATTRIBUTE_SAME_SIZE_VERTICALLY, false);

    final Element marginElement = LwXmlReader.getRequiredChild(element, "margin");
    final Insets margin = new Insets(
      LwXmlReader.getRequiredInt(marginElement,"top"),
      LwXmlReader.getRequiredInt(marginElement,"left"),
      LwXmlReader.getRequiredInt(marginElement,"bottom"),
      LwXmlReader.getRequiredInt(marginElement,"right")
    );

    final GridLayoutManager layout = new GridLayoutManager(rowCount, columnCount);
    layout.setMargin(margin);
    layout.setVGap(vGap);
    layout.setHGap(hGap);
    layout.setSameSizeHorizontally(sameSizeHorizontally);
    layout.setSameSizeVertically(sameSizeVertically);
    container.setLayout(layout);
  }

  @Override
  void readChildConstraints(final Element constraintsElement, final LwComponent component) {
    // Read Grid constraints
    final Element gridElement = LwXmlReader.getChild(constraintsElement, "grid");
    if(gridElement != null){
      final GridConstraints constraints=new GridConstraints();

      constraints.setRow(LwXmlReader.getRequiredInt(gridElement, "row"));
      constraints.setColumn(LwXmlReader.getRequiredInt(gridElement, "column"));
      constraints.setRowSpan(LwXmlReader.getRequiredInt(gridElement, "row-span"));
      constraints.setColSpan(LwXmlReader.getRequiredInt(gridElement, "col-span"));
      constraints.setVSizePolicy(LwXmlReader.getRequiredInt(gridElement, "vsize-policy"));
      constraints.setHSizePolicy(LwXmlReader.getRequiredInt(gridElement, "hsize-policy"));
      constraints.setAnchor(LwXmlReader.getRequiredInt(gridElement, "anchor"));
      constraints.setFill(LwXmlReader.getRequiredInt(gridElement, "fill"));
      constraints.setIndent(LwXmlReader.getOptionalInt(gridElement, UIFormXmlConstants.ATTRIBUTE_INDENT, 0));
      constraints.setUseParentLayout(LwXmlReader.getOptionalBoolean(gridElement, UIFormXmlConstants.ATTRIBUTE_USE_PARENT_LAYOUT, false));

      // minimum size
      final Element minSizeElement = LwXmlReader.getChild(gridElement, "minimum-size");
      if (minSizeElement != null) {
        constraints.myMinimumSize.width = LwXmlReader.getRequiredInt(minSizeElement, "width");
        constraints.myMinimumSize.height = LwXmlReader.getRequiredInt(minSizeElement, "height");
      }

      // preferred size
      final Element prefSizeElement = LwXmlReader.getChild(gridElement, "preferred-size");
      if (prefSizeElement != null) {
        constraints.myPreferredSize.width = LwXmlReader.getRequiredInt(prefSizeElement, "width");
        constraints.myPreferredSize.height = LwXmlReader.getRequiredInt(prefSizeElement, "height");
      }

      // maximum size
      final Element maxSizeElement = LwXmlReader.getChild(gridElement, "maximum-size");
      if (maxSizeElement != null) {
        constraints.myMaximumSize.width = LwXmlReader.getRequiredInt(maxSizeElement, "width");
        constraints.myMaximumSize.height = LwXmlReader.getRequiredInt(maxSizeElement, "height");
      }

      component.getConstraints().restore(constraints);
    }
  }
}
