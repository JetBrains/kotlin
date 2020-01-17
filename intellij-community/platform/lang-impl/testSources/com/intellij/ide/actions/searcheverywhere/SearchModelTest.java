// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.intellij.ide.actions.searcheverywhere.SearchEverywhereUI.SearchListModel;

public class SearchModelTest extends BasePlatformTestCase {

  private final static SearchEverywhereContributor<Object> STUB_CONTRIBUTOR_1 = createStubContributor(100);
  private final static SearchEverywhereContributor<Object> STUB_CONTRIBUTOR_2 = createStubContributor(200);
  private final static SearchEverywhereContributor<Object> STUB_CONTRIBUTOR_3 = createStubContributor(300);

  public void testElementsAdding() {
    SearchListModel model = new SearchListModel();

    // adding to empty -----------------------------------------------------------------------
    model.addElements(Arrays.asList(
      new SearchEverywhereFoundElementInfo("item_2_20", 250, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_3_20", 340, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_1_20", 160, STUB_CONTRIBUTOR_1),
      new SearchEverywhereFoundElementInfo("item_3_30", 330, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_2_10", 280, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_1_10", 180, STUB_CONTRIBUTOR_1),
      new SearchEverywhereFoundElementInfo("item_2_30", 230, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_3_10", 350, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_1_30", 140, STUB_CONTRIBUTOR_1)
    ));

    List<Object> actualItems = model.getItems();
    List<Object> expectedItems = Arrays.asList("item_1_10", "item_1_20", "item_1_30", "item_2_10", "item_2_20", "item_2_30", "item_3_10", "item_3_20", "item_3_30");
    Assert.assertEquals(expectedItems, actualItems);

    // "more" elements -----------------------------------------------------------------------
    model.setHasMore(STUB_CONTRIBUTOR_1, true);
    model.setHasMore(STUB_CONTRIBUTOR_3, true);

    actualItems = model.getItems();
    expectedItems = Arrays.asList("item_1_10", "item_1_20", "item_1_30", SearchListModel.MORE_ELEMENT,
                                  "item_2_10", "item_2_20", "item_2_30",
                                  "item_3_10", "item_3_20", "item_3_30", SearchListModel.MORE_ELEMENT);
    Assert.assertEquals(expectedItems, actualItems);

    // adding to existing -----------------------------------------------------------------------
    model.addElements(Arrays.asList(
      new SearchEverywhereFoundElementInfo("item_3_03", 370, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_2_23", 250, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_1_35", 130, STUB_CONTRIBUTOR_1),
      new SearchEverywhereFoundElementInfo("item_2_25", 245, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_1_25", 150, STUB_CONTRIBUTOR_1),
      new SearchEverywhereFoundElementInfo("item_3_50", 310, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_2_40", 210, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_3_40", 320, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_2_05", 290, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_3_05", 360, STUB_CONTRIBUTOR_3)
    ));

    actualItems = model.getItems();
    expectedItems = Arrays.asList("item_1_10", "item_1_20", "item_1_25", "item_1_30", "item_1_35", SearchListModel.MORE_ELEMENT,
                                  "item_2_05", "item_2_10", "item_2_20","item_2_23", "item_2_25","item_2_30", "item_2_40",
                                  "item_3_03", "item_3_05", "item_3_10", "item_3_20", "item_3_30", "item_3_40", "item_3_50", SearchListModel.MORE_ELEMENT);
    Assert.assertEquals(expectedItems, actualItems);

    // expiring results -----------------------------------------------------------------------
    model.expireResults();
    model.addElements(Arrays.asList(
      new SearchEverywhereFoundElementInfo("item_3_50", 310, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_1_20", 160, STUB_CONTRIBUTOR_1),
      new SearchEverywhereFoundElementInfo("item_3_10", 350, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_2_23", 250, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_3_30", 330, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_2_05", 290, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_2_10", 280, STUB_CONTRIBUTOR_2),
      new SearchEverywhereFoundElementInfo("item_1_35", 130, STUB_CONTRIBUTOR_1),
      new SearchEverywhereFoundElementInfo("item_3_20", 340, STUB_CONTRIBUTOR_3),
      new SearchEverywhereFoundElementInfo("item_1_25", 150, STUB_CONTRIBUTOR_1)
    ));
    model.setHasMore(STUB_CONTRIBUTOR_1, true);
    model.setHasMore(STUB_CONTRIBUTOR_2, true);

    actualItems = model.getItems();
    expectedItems = Arrays.asList("item_1_20", "item_1_25", "item_1_35", SearchListModel.MORE_ELEMENT,
                                  "item_2_05", "item_2_10", "item_2_23", SearchListModel.MORE_ELEMENT,
                                  "item_3_10", "item_3_20", "item_3_30", "item_3_50");
    Assert.assertEquals(expectedItems, actualItems);

    // removing items -----------------------------------------------------------------------
    model.removeElement("item_1_25", STUB_CONTRIBUTOR_1);
    model.removeElement("item_3_20", STUB_CONTRIBUTOR_3);
    model.removeElement("item_3_30", STUB_CONTRIBUTOR_3);
    model.setHasMore(STUB_CONTRIBUTOR_1, false);

    actualItems = model.getItems();
    expectedItems = Arrays.asList("item_1_20", "item_1_35",
                                  "item_2_05", "item_2_10", "item_2_23", SearchListModel.MORE_ELEMENT,
                                  "item_3_10", "item_3_50");
    Assert.assertEquals(expectedItems, actualItems);
  }

  @NotNull
  private static SearchEverywhereContributor<Object> createStubContributor(int weight) {
    String id = UUID.randomUUID().toString();
    return new SearchEverywhereContributor<Object>() {
      @NotNull
      @Override
      public String getSearchProviderId() {
        return id;
      }

      @NotNull
      @Override
      public String getGroupName() {
        return id;
      }

      @Override
      public int getSortWeight() {
        return weight;
      }

      @Override
      public boolean showInFindResults() {
        return false;
      }

      @Override
      public void fetchElements(@NotNull String pattern,
                                @NotNull ProgressIndicator progressIndicator,
                                @NotNull Processor<? super Object> consumer) {

      }

      @Override
      public boolean processSelectedItem(@NotNull Object selected, int modifiers, @NotNull String searchText) {
        return false;
      }

      @NotNull
      @Override
      public ListCellRenderer<? super Object> getElementsRenderer() {
        throw new UnsupportedOperationException();
      }

      @Nullable
      @Override
      public Object getDataForItem(@NotNull Object element, @NotNull String dataId) {
        return null;
      }
    };
  }
}
