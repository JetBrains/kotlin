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

package com.intellij.ide.todo.configurable;

import com.intellij.ide.IdeBundle;
import com.intellij.psi.search.TodoPattern;
import com.intellij.util.ui.ItemRemovable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.List;

final class PatternsTableModel extends AbstractTableModel implements ItemRemovable{
  private final String[] ourColumnNames=new String[]{
    IdeBundle.message("column.todo.patterns.icon"),
    IdeBundle.message("column.todo.patterns.case.sensitive"),
    IdeBundle.message("column.todo.patterns.pattern")
  };
  private final Class[] ourColumnClasses=new Class[]{Icon.class,Boolean.class,String.class};

  private final List<TodoPattern> myPatterns;

  PatternsTableModel(List<TodoPattern> patterns){
    myPatterns=patterns;
  }

  @Override
  public String getColumnName(int column){
    return ourColumnNames[column];
  }

  @Override
  public Class getColumnClass(int column){
    return ourColumnClasses[column];
  }

  @Override
  public int getColumnCount(){
    return 3;
  }

  @Override
  public int getRowCount(){
    return myPatterns.size();
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return columnIndex == 1;
  }

  @Override
  public Object getValueAt(int row,int column){
    TodoPattern pattern=myPatterns.get(row);
    switch(column){
      case 0:{ // "Icon" column
        return pattern.getAttributes().getIcon();
      }case 1:{ // "Case Sensitive" column
        return pattern.isCaseSensitive()?Boolean.TRUE:Boolean.FALSE;
      }case 2:{ // "Pattern" column
        return pattern.getPatternString();
      }default:{
        throw new IllegalArgumentException();
      }
    }
  }

  @Override
  public void setValueAt(Object value,int row,int column){
    TodoPattern pattern=myPatterns.get(row);
    switch(column){
      case 0:{
        pattern.getAttributes().setIcon((Icon)value);
        break;
      }case 1:{
        pattern.setCaseSensitive(((Boolean)value).booleanValue());
        break;
      }case 2:{
        pattern.setPatternString(((String)value).trim());
        break;
      }default:{
        throw new IllegalArgumentException();
      }
    }
  }

  @Override
  public void removeRow(int index){
    myPatterns.remove(index);
    fireTableRowsDeleted(index,index);
  }
}
