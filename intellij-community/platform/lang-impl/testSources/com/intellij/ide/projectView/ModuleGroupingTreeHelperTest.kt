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
package com.intellij.ide.projectView

import com.intellij.ide.projectView.impl.ModuleGroup
import com.intellij.ide.projectView.impl.ModuleGroupingImplementation
import com.intellij.ide.projectView.impl.ModuleGroupingTreeHelper
import com.intellij.openapi.util.Pair
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import junit.framework.TestCase
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * @author nik
 */
class ModuleGroupingTreeHelperTest: UsefulTestCase() {
  private lateinit var tree: Tree
  private lateinit var root: MockModuleTreeNode
  private lateinit var model: DefaultTreeModel

  override fun setUp() {
    super.setUp()
    root = MockModuleTreeNode("root")
    model = DefaultTreeModel(root)
    tree = Tree(model)
  }

  fun `test disabled grouping`() {
    createHelper(false).createModuleNodes("a.main", "a.util")
    assertTreeEqual("""
            -root
             a.main
             a.util""")
    createHelperFromTree(true).moveAllModuleNodesAndCheckResult("""
            -root
             -a
              a.main
              a.util""")
  }

  fun `test disabled grouping and compacting nodes`() {
    createHelper(enableGrouping = false, compactGroupNodes = false).createModuleNodes("a.main", "a.util")
    assertTreeEqual("""
            -root
             a.main
             a.util""")
    createHelperFromTree(enableGrouping = true, compactGroupNodes = false).moveAllModuleNodesAndCheckResult("""
            -root
             -a
              a.main
              a.util""")
  }

  fun `test single module`() {
    createHelper().createModuleNodes("a.main")
    assertTreeEqual("""
            -root
             a.main""")

    createHelperFromTree(false).moveAllModuleNodesAndCheckResult("""
            -root
             a.main""")
  }

  fun `test single module with disabled compacting`() {
    createHelper(compactGroupNodes = false).createModuleNodes("a.main")
    assertTreeEqual("""
            -root
             -a
              a.main""")

    createHelperFromTree(false).moveAllModuleNodesAndCheckResult("""
            -root
             a.main""")
  }

  fun `test two modules`() {
    createHelper().createModuleNodes("a.main", "a.util")
    assertTreeEqual("""
            -root
             -a
              a.main
              a.util
""")

    createHelperFromTree(false).moveAllModuleNodesAndCheckResult("""
            -root
             a.main
             a.util""")
  }

  fun `test two modules with common prefix`() {
    createHelper().createModuleNodes("com.a.main", "com.a.util")
    assertTreeEqual("""
            -root
             -a
              com.a.main
              com.a.util
""")
    createHelperFromTree(false).moveAllModuleNodesAndCheckResult("""
            -root
             com.a.main
             com.a.util
             """)

  }

  fun `test two modules with common prefix and parent module as a group`() {
    createHelper().createModuleNodes("com.a.main", "com.a.util", "com.a")
    assertTreeEqual("""
            -root
             -com.a
              com.a.main
              com.a.util
""")
    createHelperFromTree(false).moveAllModuleNodesAndCheckResult("""
            -root
             com.a
             com.a.main
             com.a.util
             """)
  }

  fun `test create two nested groups`() {
    createHelper().createModuleNodes("com.a.foo.bar", "com.a.baz", "com.a.foo.baz")
    assertTreeEqual("""
            -root
             -a
              com.a.baz
              -foo
               com.a.foo.bar
               com.a.foo.baz
""")
    createHelperFromTree(false).moveAllModuleNodesAndCheckResult("""
            -root
             com.a.baz
             com.a.foo.bar
             com.a.foo.baz
             """)
  }

  fun `test two groups`() {
    createHelper().createModuleNodes("a.main", "b.util", "a.util", "b.main")
    assertTreeEqual("""
            -root
             -a
              a.main
              a.util
             -b
              b.main
              b.util
""")

    createHelperFromTree(false).moveAllModuleNodesAndCheckResult("""
            -root
             a.main
             a.util
             b.main
             b.util
""")
  }

  fun `test module as a group`() {
    createHelper().createModuleNodes("a.impl", "a", "a.tests")
    assertTreeEqual("""
            -root
             -a
              a.impl
              a.tests
""")

    createHelperFromTree(false).moveAllModuleNodesAndCheckResult("""
            -root
             a
             a.impl
             a.tests""")
  }

  fun `test module as a group with inner modules`() {
    createHelper().createModuleNodes("com.foo", "com.foo.bar", "com.foo.baz.zoo1", "com.foo.baz.zoo2")
    assertTreeEqual("""
            -root
             -com.foo
              -baz
               com.foo.baz.zoo1
               com.foo.baz.zoo2
              com.foo.bar
""")
  }



  fun `test add prefix to module name`() {
    val nodes = createHelper().createModuleNodes("main", "util")
    assertTreeEqual("""
            -root
             main
             util""")
    val node = nodes.find { it.second.name == "main" }!!
    node.second.name = "a.main"
    moveModuleNodeToProperGroupAndCheckResult(node, """
                  -root
                   a.main
                   util""")
  }

  fun `test move module node to new group`() {
    val nodes = createHelper().createModuleNodes("main", "util", "a.foo")
    assertTreeEqual("""
            -root
             a.foo
             main
             util""")
    val node = nodes.find { it.second.name == "main" }!!
    node.second.name = "a.main"
    moveModuleNodeToProperGroupAndCheckResult(node, """
                  -root
                   -a
                    a.foo
                    a.main
                   util""")
  }

  fun `test move module node from parent module`() {
    val nodes = createHelper().createModuleNodes("a", "a.main")
    assertTreeEqual("""
            -root
             -a
              a.main""")
    val node = nodes.find { it.second.name == "a.main" }!!
    node.second.name = "main"
    moveModuleNodeToProperGroupAndCheckResult(node, """
            -root
             a
             main""")
  }

  fun `test move module node to parent module`() {
    val nodes = createHelper().createModuleNodes("a", "main")
    val node = nodes.find { it.second.name == "main" }!!
    node.second.name = "a.main"
    moveModuleNodeToProperGroupAndCheckResult(node, """
            -root
             -a
              a.main""")
  }

  fun `test insert component into the middle of a module name`() {
    val nodes = createHelper().createModuleNodes("a", "a.main")
    val node = nodes.find { it.second.name == "a.main" }!!
    node.second.name = "a.main.util"
    moveModuleNodeToProperGroupAndCheckResult(node, """
            -root
             -a
              a.main.util""")
  }

  fun `test move module node to child node`() {
    val nodes = createHelper().createModuleNodes("a.main", "a.util")
    val node = nodes.find { it.second.name == "a.util" }!!
    node.second.name = "a.main.util"
    moveModuleNodeToProperGroupAndCheckResult(node, """
            -root
             -a.main
              a.main.util""")
  }

  fun `test remove component from the middle of a module name`() {
    val nodes = createHelper().createModuleNodes("a.foo", "a.foo.bar.baz")
    val node = nodes.find { it.second.name == "a.foo.bar.baz" }!!
    node.second.name = "a.baz"
    moveModuleNodeToProperGroupAndCheckResult(node, """
            -root
             -a
              a.baz
              a.foo""")
  }

  fun `test module node become parent module`() {
    val nodes = createHelper().createModuleNodes("b", "a.main")
    val node = nodes.find { it.second.name == "b" }!!
    node.second.name = "a"
    moveModuleNodeToProperGroupAndCheckResult(node, """
            -root
             -a
              a.main""")
  }

  fun `test parent module become ordinary module`() {
    val nodes = createHelper().createModuleNodes("a", "a.main")
    val node = nodes.find { it.second.name == "a" }!!
    node.second.name = "b"
    moveModuleNodeToProperGroupAndCheckResult(node, """
            -root
             a.main
             b""")
  }

  fun `test parent module become ordinary module with disabled compacting`() {
    val nodes = createHelper(compactGroupNodes = false).createModuleNodes("a", "a.main")
    val node = nodes.find { it.second.name == "a" }!!
    node.second.name = "b"
    moveModuleNodeToProperGroupAndCheckResult(node, """
            -root
             -a
              a.main
             b""", compactGroupNodes = false)
  }

  fun `test parent module become ordinary module but group remains`() {
    val nodes = createHelper().createModuleNodes("a", "a.main", "a.util")
    val node = nodes.find { it.second.name == "a" }!!
    node.second.name = "b"
    moveModuleNodeToProperGroupAndCheckResult(node, """
            -root
             -a
              a.main
              a.util
             b""")
  }

  fun `test do not move node if its group wasn't changed`() {
    val nodes = createHelper().createModuleNodes("a", "a.main")
    nodes.forEach {
      val node = it.first
      val newNode = createHelperFromTree(nodeToBeMovedFilter = {it == node}).moveModuleNodeToProperGroup(node, it.second, root, model, tree)
      assertSame(node, newNode)
    }
  }

  fun `test do not move node if its virtual group wasn't changed`() {
    val nodes = createHelper().createModuleNodes("a.util.foo", "a.main.bar")
    nodes.forEach {
      val node = it.first
      val newNode = createHelperFromTree(nodeToBeMovedFilter = {it == node}).moveModuleNodeToProperGroup(node, it.second, root, model, tree)
      assertSame(node, newNode)
    }
  }

  fun `test add new module node`() {
    val helper = createHelper()
    helper.createModuleNodes("a", "a.main")
    helper.createModuleNode(MockModule("a.b"), root, model)
    assertTreeEqual("""
           -root
            -a
             a.b
             a.main
""")
  }

  fun `test remove module node`() {
    val helper = createHelper()
    val nodes = helper.createModuleNodes("a.main", "a.util", "b")
    assertTreeEqual("""
           -root
            -a
             a.main
             a.util
            b
""")

    helper.removeNode(nodes[0].first, root, model)
    assertTreeEqual("""
           -root
            a.util
            b
""")

    helper.removeNode(nodes[1].first, root, model)
    assertTreeEqual("""
           -root
            b
""")
  }

  fun `test remove module node with disabled compacting`() {
    val helper = createHelper(compactGroupNodes = false)
    val nodes = helper.createModuleNodes("a.main", "a.util", "b")
    assertTreeEqual("""
           -root
            -a
             a.main
             a.util
            b
""")

    helper.removeNode(nodes[0].first, root, model)
    assertTreeEqual("""
           -root
            -a
             a.util
            b
""")

    helper.removeNode(nodes[1].first, root, model)
    assertTreeEqual("""
           -root
            b
""")
  }

  private fun moveModuleNodeToProperGroupAndCheckResult(node: Pair<MockModuleTreeNode, MockModule>,
                                                        expected: String, compactGroupNodes: Boolean = true) {
    val thisNode: (MockModuleTreeNode) -> Boolean = { it == node.first }
    val helper = createHelperFromTree(nodeToBeMovedFilter = thisNode, compactGroupNodes = compactGroupNodes)
    helper.checkConsistency(nodeToBeMovedFilter = thisNode)
    helper.moveModuleNodeToProperGroup(node.first, node.second, root, model, tree)
    assertTreeEqual(expected)
    helper.checkConsistency(nodeToBeMovedFilter = {false})
  }

  private fun ModuleGroupingTreeHelper<MockModule, MockModuleTreeNode>.moveAllModuleNodesAndCheckResult(expected: String) {
    checkConsistency(nodeToBeMovedFilter = {true})
    moveAllModuleNodesToProperGroups(root, model)
    assertTreeEqual(expected)
    checkConsistency(nodeToBeMovedFilter = {false})
  }

  private fun ModuleGroupingTreeHelper<MockModule, MockModuleTreeNode>.createModuleNodes(vararg names: String): List<Pair<MockModuleTreeNode, MockModule>> {
    val nodes = createModuleNodes(names.map { MockModule(it) }, root, model)
    checkConsistency { false }
    return nodes.map { Pair(it, (it as MockModuleNode).module)}
  }

  private fun assertTreeEqual(expected: String) {
    TreeUtil.expandAll(tree)
    PlatformTestUtil.assertTreeEqual(tree, expected.trimIndent() + "\n")
  }

  private fun createHelper(enableGrouping: Boolean = true, compactGroupNodes: Boolean = true): ModuleGroupingTreeHelper<MockModule, MockModuleTreeNode> {
    return ModuleGroupingTreeHelper.forEmptyTree(enableGrouping, MockModuleGrouping(compactGroupNodes), ::MockModuleGroupNode, ::MockModuleNode, nodeComparator)
  }

  private fun createHelperFromTree(enableGrouping: Boolean = true, compactGroupNodes: Boolean = true, nodeToBeMovedFilter: (MockModuleTreeNode) -> Boolean = {true}): ModuleGroupingTreeHelper<MockModule, MockModuleTreeNode> {
    return ModuleGroupingTreeHelper.forTree(root, { it.moduleGroup }, { (it as? MockModuleNode)?.module },
                                            enableGrouping, MockModuleGrouping(compactGroupNodes), ::MockModuleGroupNode, ::MockModuleNode, nodeComparator,
                                            nodeToBeMovedFilter)
  }

  private fun ModuleGroupingTreeHelper<MockModule, MockModuleTreeNode>.checkConsistency(nodeToBeMovedFilter: (MockModuleTreeNode) -> Boolean) {
    val expectedNodeForGroup = HashMap<ModuleGroup, MockModuleTreeNode>(getNodeForGroupMap())
    val expectedNodeVirtualGroupToChildNode = HashMap<ModuleGroup, MockModuleTreeNode>(getVirtualGroupToChildNodeMap())
    val expectedGroupByNode = HashMap<MockModuleTreeNode, ModuleGroup>(getGroupByNodeMap())
    val expectedModuleByNode = HashMap<MockModuleTreeNode, MockModule>(getModuleByNodeMap())
    TreeUtil.treeNodeTraverser(root).postOrderDfsTraversal().forEach { o ->
      val node = o as MockModuleTreeNode
      if (node == root) return@forEach
      if (node is MockModuleNode) {
        TestCase.assertEquals(node.module, expectedModuleByNode[node])
        expectedModuleByNode.remove(node)
      }
      if (!nodeToBeMovedFilter(node)) {
        val parentGroupPath = when (node) {
          is MockModuleNode -> MockModuleGrouping().getGroupPath(node.module)
          is MockModuleGroupNode -> node.moduleGroup.groupPath.dropLast(1)
          else -> emptyList()
        }
        for (i in parentGroupPath.size downTo 1) {
          val parentGroup = ModuleGroup(parentGroupPath.subList(0, i))
          val childNode = expectedNodeVirtualGroupToChildNode.remove(parentGroup)
          if (childNode != null) {
            TestCase.assertEquals(childNode, node)
            TestCase.assertNull("There are both virtual and real nodes for '${parentGroup.qualifiedName}' group",
                                expectedNodeForGroup[parentGroup])
          }
          else if (isGroupingEnabled()) {
            TestCase.assertNotNull("There is no virtual or real node for '${parentGroup.qualifiedName}' group",
                                   expectedNodeForGroup[parentGroup])
            break
          }
        }
      }
      val moduleGroup = node.moduleGroup
      TestCase.assertSame(node, expectedNodeForGroup[moduleGroup])
      expectedNodeForGroup.remove(moduleGroup)
      TestCase.assertEquals(moduleGroup, expectedGroupByNode[node])
      expectedGroupByNode.remove(node)
    }
    assertEmpty("Unexpected nodes in helper", expectedNodeForGroup.entries)
    assertEmpty("Unexpected groups in helper", expectedGroupByNode.entries)
    assertEmpty("Unexpected modules in helper", expectedModuleByNode.entries)
    if (TreeUtil.treeNodeTraverser(root).none { nodeToBeMovedFilter(it as MockModuleTreeNode) }) {
      assertEmpty("Unexpected virtual groups in helper", expectedNodeVirtualGroupToChildNode.entries)
    }
  }
}

private val nodeComparator = Comparator.comparing { node: MockModuleTreeNode -> node.text }

private open class MockModuleTreeNode(userObject: Any, val text: String = userObject.toString()): DefaultMutableTreeNode(text) {
  open val moduleGroup: ModuleGroup? = null
  override fun toString() = text
}

private class MockModuleGrouping(override val compactGroupNodes: Boolean = true) : ModuleGroupingImplementation<MockModule> {
  override fun getGroupPath(m: MockModule) = m.name.split('.').dropLast(1)
  override fun getModuleAsGroupPath(m: MockModule) = m.name.split('.')
}
private class MockModule(var name: String)

private class MockModuleNode(val module: MockModule): MockModuleTreeNode(module, module.name) {
  override val moduleGroup = ModuleGroup(MockModuleGrouping().getModuleAsGroupPath(module))
}

private class MockModuleGroupNode(override val moduleGroup: ModuleGroup): MockModuleTreeNode(moduleGroup)