/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.codeInsight.completion;

import com.intellij.psi.statistics.StatisticsInfo;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public class StatisticsComparable implements Comparable<StatisticsComparable> {
  private final int myScalar;
  private final StatisticsInfo myStatisticsInfo;

  public StatisticsComparable(int scalar, @NotNull StatisticsInfo statisticsInfo) {
    myScalar = scalar;
    myStatisticsInfo = statisticsInfo;
  }

  public int getScalar() {
    return myScalar;
  }

  @NotNull
  public StatisticsInfo getStatisticsInfo() {
    return myStatisticsInfo;
  }

  @Override
  public String toString() {
    return String.valueOf(myScalar);
  }

  @Override
  public int compareTo(StatisticsComparable o) {
    return Integer.compare(myScalar, o.myScalar);
  }
}
