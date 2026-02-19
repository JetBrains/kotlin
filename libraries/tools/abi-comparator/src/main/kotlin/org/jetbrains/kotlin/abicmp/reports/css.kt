/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

const val REPORT_CSS = """
table, th, td {
  border: 1px solid black;
  border-collapse: collapse;
  padding: 2px;
}
th {
  background: #B0B0B0;
}
td {
  background: #EFEFB0;
  vertical-align: top;
}
td.location {
  background: #E0E0E0;
  vertical-align: top;
}
hr {
  border: 1px solid black;
  border-collapse: collapse;
}
"""