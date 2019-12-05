/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.navigation


import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * @author Denis Zhdanov
 */
class DocPreviewUtilTest {

  @Test
  void classDocPreview() {
    
    def header = '''\
[&lt; 1.7 &gt;] java.lang
 public final class String extends Object
 implements Serializable, Comparable&lt;String&gt;, CharSequence\
'''
    
    def fullText = '''\
<html><head>    <style type="text/css">        #error {            background-color: #eeeeee;            margin-bottom: 10px;        }        p {            margin: 5px 0;        }    </style></head><body><small><b>java.lang</b></small><PRE>public final class <b>java.lang.String</b>
extends <a href="psi_element://java.lang.Object"><code>java.lang.Object</code></a>
implements <a href="psi_element://java.io.Serializable"><code>java.io.Serializable</code></a>,&nbsp;<a href="psi_element://java.lang.Comparable"><code>java.lang.Comparable</code></a>&lt;<a href="psi_element://java.lang.String"><code>java.lang.String</code></a>&gt;,&nbsp;<a href="psi_element://java.lang.CharSequence"><code>java.lang.CharSequence</code></a></PRE>
   The <code>String</code> class represents character strings. All
   string literals in Java programs, such as <code>"abc"</code>, are
   implemented as instances of this class.
   <p>
   Strings are constant; their values cannot be changed after they
   are created. String buffers support mutable strings.
   Because String objects are immutable they can be shared. For example:
   <p><blockquote><pre>
       String str = "abc";
   </pre></blockquote><p>
   is equivalent to:
   <p><blockquote><pre>
       char data[] = {'a', 'b', 'c'};
       String str = new String(data);
   </pre></blockquote><p>
   Here are some more examples of how strings can be used:
   <p><blockquote><pre>
       System.out.println("abc");
       String cde = "cde";
       System.out.println("abc" + cde);
       String c = "abc".substring(2,3);
       String d = cde.substring(1, 2);
   </pre></blockquote>
   <p>
   The class <code>String</code> includes methods for examining
   individual characters of the sequence, for comparing strings, for
   searching strings, for extracting substrings, and for creating a
   copy of a string with all characters translated to uppercase or to
   lowercase. Case mapping is based on the Unicode Standard version
   specified by the <a href="psi_element://java.lang.Character"><code>Character</code></a> class.
   <p>
   The Java language provides special support for the string
   concatenation operator (&nbsp;+&nbsp;), and for conversion of
   other objects to strings. String concatenation is implemented
   through the <code>StringBuilder</code>(or <code>StringBuffer</code>)
   class and its <code>append</code> method.
   String conversions are implemented through the method
   <code>toString</code>, defined by <code>Object</code> and
   inherited by all classes in Java. For additional information on
   string concatenation and conversion, see Gosling, Joy, and Steele,
   <i>The Java Language Specification</i>.
  
   <p> Unless otherwise noted, passing a <tt>null</tt> argument to a constructor
   or method in this class will cause a <a href="psi_element://java.lang.NullPointerException"><code>NullPointerException</code></a> to be
   thrown.
  
   <p>A <code>String</code> represents a string in the UTF-16 format
   in which <em>supplementary characters</em> are represented by <em>surrogate
   pairs</em> (see the section <a href="Character.html#unicode">Unicode
   Character Representations</a> in the <code>Character</code> class for
   more information).
   Index values refer to <code>char</code> code units, so a supplementary
   character uses two positions in a <code>String</code>.
   <p>The <code>String</code> class provides methods for dealing with
   Unicode code points (i.e., characters), in addition to those for
   dealing with Unicode code units (i.e., <code>char</code> values).
  
   <DD><DL><DT><b>Since:</b><DD>JDK1.0</DD></DL></DD><DD><DL><DT><b>See Also:</b><DD><a href="psi_element://java.lang.Object#toString()"><code>Object.toString()</code></a>,
<a href="psi_element://java.lang.StringBuffer"><code>StringBuffer</code></a>,
<a href="psi_element://java.lang.StringBuilder"><code>StringBuilder</code></a>,
<a href="psi_element://java.nio.charset.Charset"><code>Charset</code></a></DD></DL></DD></body></html>\
'''
    
    def expected = '''\
java.lang<br/> public final class <a href="psi_element://java.lang.String">String</a> extends <a href="psi_element://java.lang.Object">Object</a><br/> implements <a href="psi_element://java.io.Serializable">Serializable</a>, <a href="psi_element://java.lang.Comparable">Comparable</a>&lt;<a href="psi_element://java.lang.String">String</a>&gt;, <a href="psi_element://java.lang.CharSequence">CharSequence</a>\
'''

    def actual = DocPreviewUtil.buildPreview(header, "java.lang.String", fullText)
    assertTrue(actual.endsWith(expected)) // Can't check for equals() because jdk name might differ on different machines.
  }

  @Test
  void crossingLinks() {
    def header = '''\
OCCompletionPriority
OCCompletionPriority SMART_COMPLETION_PRIORITY'''
    
    def fullText = '''\
<html><head>    <style type="text/css">        #error {            background-color: #eeeeee;            margin-bottom: 10px;        }        p {            margin: 5px 0;        }    </style></head><body><small><b><a href="psi_element://com.jetbrains.objc.lang.completion.OCCompletionPriority"><code>com.jetbrains.objc.lang.completion.OCCompletionPriority</code></a></b></small><PRE><a href="psi_element://com.jetbrains.objc.lang.completion.OCCompletionPriority"><code>OCCompletionPriority</code></a> <b>SMART_COMPLETION_PRIORITY</b></PRE></body></html>

Qname'''
    
    def expected = '''\
<a href="psi_element://com.jetbrains.objc.lang.completion.OCCompletionPriority">OCCompletionPriority</a><br/><a href="psi_element://com.jetbrains.objc.lang.completion.OCCompletionPriority">OCCompletionPriority</a> SMART_COMPLETION_PRIORITY'''
    
    assertEquals(expected, DocPreviewUtil.buildPreview(header, null, fullText))
  }
  
  @Test
  void tailSubstrings() {
    def header = '''\
PsiResolveHelperImpl
public static Pair&lt;PsiType, ConstraintType&gt; getSubstitutionForTypeParameterConstraint (PsiTypeParameter typeParam, PsiType param, PsiType arg, boolean isContraVariantPosition, LanguageLevel languageLevel)'''
    
    def fullText = '''\
<html><head>    <style type="text/css">        #error {            background-color: #eeeeee;            margin-bottom: 10px;        }        p {            margin: 5px 0;        }    </style></head><body><small><b><a href="psi_element://com.intellij.psi.impl.source.resolve.PsiResolveHelperImpl"><code>com.intellij.psi.impl.source.resolve.PsiResolveHelperImpl</code></a></b></small><PRE>@<a href="psi_element://org.jetbrains.annotations.Nullable"><code>Nullable</code></a>&nbsp;public static&nbsp;<a href="psi_element://com.intellij.openapi.util.Pair"><code>Pair</code></a>&lt;<a href="psi_element://com.intellij.psi.PsiType"><code>PsiType</code></a>, <a href="psi_element://com.intellij.psi.ConstraintType"><code>ConstraintType</code></a>&gt;&nbsp;<b>getSubstitutionForTypeParameterConstraint</b>(<a href="psi_element://com.intellij.psi.PsiTypeParameter"><code>PsiTypeParameter</code></a>&nbsp;typeParam,
                                                                                  <a href="psi_element://com.intellij.psi.PsiType"><code>PsiType</code></a>&nbsp;param,
                                                                                  <a href="psi_element://com.intellij.psi.PsiType"><code>PsiType</code></a>&nbsp;arg,
                                                                                  boolean&nbsp;isContraVariantPosition,
                                                                                  <a href="psi_element://com.intellij.pom.java.LanguageLevel"><code>LanguageLevel</code></a>&nbsp;languageLevel)</PRE></body></html>'''
    
    def expected = '''\
<a href="psi_element://com.intellij.psi.impl.source.resolve.PsiResolveHelperImpl">PsiResolveHelperImpl</a><br/>public static <a href="psi_element://com.intellij.openapi.util.Pair">Pair</a>&lt;<a href="psi_element://com.intellij.psi.PsiType">PsiType</a>, <a href="psi_element://com.intellij.psi.ConstraintType">ConstraintType</a>&gt; getSubstitutionForTypeParameterConstraint (<a href="psi_element://com.intellij.psi.PsiTypeParameter">PsiTypeParameter</a> typeParam, <a href="psi_element://com.intellij.psi.PsiType">PsiType</a> param, <a href="psi_element://com.intellij.psi.PsiType">PsiType</a> arg, boolean isContraVariantPosition, <a href="psi_element://com.intellij.pom.java.LanguageLevel">LanguageLevel</a> languageLevel)'''

    assertEquals(expected, DocPreviewUtil.buildPreview(header, null, fullText))
  }
  
  @Test
  void headSubstrings() {
    def header = '''\
ASTNode
TextRange getTextRange ()'''
    
    def fullText = '''\
<html><head>    <style type="text/css">        #error {            background-color: #eeeeee;            margin-bottom: 10px;        }        p {            margin: 5px 0;        }    </style></head><body><small><b><a href="psi_element://com.intellij.lang.ASTNode"><code>com.intellij.lang.ASTNode</code></a></b></small><PRE><a href="psi_element://com.intellij.openapi.util.TextRange"><code>TextRange</code></a>&nbsp;<b>getTextRange</b>()</PRE>
     Returns the text range (a combination of starting offset in the document and length) for this node.
    
     <DD><DL><DT><b>Returns:</b><DD>the text range.</DD></DL></DD></body></html>'''
    
    def expected = '''\
<a href="psi_element://com.intellij.lang.ASTNode">ASTNode</a><br/><a href="psi_element://com.intellij.openapi.util.TextRange">TextRange</a> getTextRange ()'''
    
    assertEquals(expected, DocPreviewUtil.buildPreview(header, null, fullText))
  }

  @Test
  void "single letter 'from' substitution"() {
    def header = '''\
E
E A
Enum constant ordinal: 0'''
    
    def fullText = '''\
<html><head>    <style type="text/css">        #error {            background-color: #eeeeee;            margin-bottom: 10px;        }        p {            margin: 5px 0;        }    </style></head><body><small><b><a href="psi_element://org.denis.E"><code>org.denis.E</code></a></b></small><PRE><a href="psi_element://org.denis.E"><code>E</code></a> <b>A</b></PRE></body></html>'''
    
    def expected = '''\
<a href="psi_element://org.denis.E">E</a><br/><a href="psi_element://org.denis.E">E</a> A<br/>Enum constant ordinal: 0'''
    
    assertEquals(expected, DocPreviewUtil.buildPreview(header, null, fullText))
  }

  @Test
  void "last element should have a hyperlink as well (WEB-17860)"() {
    def header = '''ExternalModule.ts
var abc: A &amp; B &amp; C'''

    def fullText = '''<b>Type:</b> <code><a href="psi_element://A">A</a> &amp; <a href="psi_element://B">B</a> &amp; <a href="psi_element://C">C</a></code>'''

    def expected = '''ExternalModule.ts<br/>var abc: <a href="psi_element://A">A</a> &amp; <a href="psi_element://B">B</a> &amp; <a href="psi_element://C">C</a>'''

    assertEquals(expected, DocPreviewUtil.buildPreview(header, null, fullText))
  }

  @Test
  void "empty qName doesn't hang"() {
    def header = '''Header'''
    def fullText = '''FullText'''
    def expected = '''Header'''

    assertEquals(expected, DocPreviewUtil.buildPreview(header, "", fullText))
  }
}
