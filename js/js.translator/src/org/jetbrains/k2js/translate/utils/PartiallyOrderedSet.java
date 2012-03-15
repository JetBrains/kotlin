/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Pavel Talanov
 *         <p/>
 *         This is very inefficient but simple implementation of partially orderered set.
 *         Feel free to replace with library implementation.
 */
public final class PartiallyOrderedSet<Element> {

    private class Arc {
        @NotNull
        public final Element from;
        @NotNull
        public final Element to;

        private Arc(@NotNull Element from, @NotNull Element to) {
            this.from = from;
            this.to = to;
        }
    }

    public interface Order<Element> {
        boolean firstDependsOnSecond(@NotNull Element first, @NotNull Element Second);
    }

    @NotNull
    private final List<Arc> arcs = Lists.newArrayList();
    @NotNull
    private final Map<Element, Integer> incomingArcs = Maps.newHashMap();
    @NotNull
    private final List<Element> elementsWithZeroIncoming = Lists.newArrayList();

    public PartiallyOrderedSet(@NotNull Collection<Element> elements, @NotNull Order<Element> order) {
        elementsWithZeroIncoming.addAll(elements);
        for (@NotNull Element first : elements) {
            for (@NotNull Element second : elements) {
                if (order.firstDependsOnSecond(first, second)) {
                    arcs.add(new Arc(first, second));
                    increaseIncomingCount(second);
                }
            }
        }
    }

    private void increaseIncomingCount(@NotNull Element element) {
        if (!incomingArcs.containsKey(element)) {
            incomingArcs.put(element, 1);
            elementsWithZeroIncoming.remove(element);
        }
        else {
            Integer count = incomingArcs.get(element);
            incomingArcs.put(element, count + 1);
        }
    }

    private void decreaseIncomingCount(@NotNull Element element) {
        assert incomingArcs.containsKey(element);
        Integer count = incomingArcs.get(element);
        if (count == 1) {
            incomingArcs.remove(element);
            elementsWithZeroIncoming.add(element);
        }
        else {
            incomingArcs.put(element, count - 1);
        }
    }

    @NotNull
    public List<Element> partiallySortedElements() {
        List<Element> result = Lists.newArrayList();
        while (!elementsWithZeroIncoming.isEmpty()) {
            result.add(getNextElement());
        }
        return result;
    }

    @NotNull
    private Element getNextElement() {
        Element elementWithZeroIncoming = getElementWithZeroIncoming();
        for (Arc arc : arcs) {
            if (arc.from == elementWithZeroIncoming) {
                decreaseIncomingCount(arc.to);
            }
        }
        return elementWithZeroIncoming;
    }

    @NotNull
    private Element getElementWithZeroIncoming() {
        int indexOfLast = elementsWithZeroIncoming.size() - 1;
        Element element = elementsWithZeroIncoming.get(indexOfLast);
        elementsWithZeroIncoming.remove(indexOfLast);
        return element;
    }

}
