/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This benchmark is a port of the V8 JavaScript benchmark suite
// DeltaBlue benchmark:
//   https://chromium.googlesource.com/external/v8/+/ba56077937e154aa0adbabd8abb9c24e53aae85d/benchmarks/deltablue.js

// Copyright 2008 the V8 project authors. All rights reserved.
// Copyright 1996 John Maloney and Mario Wolczko.

// This program is free software you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

// This implementation of the DeltaBlue benchmark is derived
// from the Smalltalk implementation by John Maloney and Mario
// Wolczko. Some parts have been translated directly, whereas
// others have been modified more aggresively to make it feel
// more like a JavaScript program.

/**
 * A JavaScript implementation of the DeltaBlue constraint-solving
 * algorithm, as described in:
 *
 * "The DeltaBlue Algorithm: An Incremental Constraint Hierarchy Solver"
 *   Bjorn N. Freeman-Benson and John Maloney
 *   January 1990 Communications of the ACM,
 *   also available as University of Washington TR 89-08-06.
 *
 * Beware: this benchmark is written in a grotesque style where
 * the constraint model is built by side-effects from constructors.
 * I've kept it this way to avoid deviating too much from the original
 * implementation.
 */

fun alert(msg: String) {
  throw Error(msg)
}

/* --- O b j e c t   M o d e l --- */

class OrderedCollection<T> {
  var elms = mutableListOf<T>()

  fun add(elm: T) = elms.add(elm)
  fun at(index: Int) = elms[index]
  fun size() = elms.size
  fun removeFirst() = elms.removeLast()
  
  fun remove(elm: T) {
    var index = 0
    var skipped = 0
    for (i in 0 until elms.size) {
      val value = elms[i]
      if (value != elm) {
        elms[index] = value
        index++
      } else {
        skipped++
      }
    }
    for (i in 0 until skipped) elms.removeLast()
  }

  operator fun iterator() = elms.iterator()
}

/* --- *
 * S t r e n g t h
 * --- */

/**
 * Strengths are used to measure the relative importance of constraints.
 * New strengths may be inserted in the strength hierarchy without
 * disrupting current constraints.  Strengths cannot be created outside
 * this class, so pointer comparison can be used for value comparison.
 */
enum class Strength {
  REQUIRED,
  STRONG_PREFERRED,
  PREFERRED,
  STRONG_DEFAULT,
  NORMAL,
  WEAK_DEFAULT,
  WEAKEST;

  val strengthValue get() = ordinal

  fun nextWeaker() = when (this) {
    REQUIRED -> STRONG_PREFERRED
    STRONG_PREFERRED -> PREFERRED
    PREFERRED -> STRONG_DEFAULT
    STRONG_DEFAULT -> NORMAL
    NORMAL -> WEAK_DEFAULT
    WEAK_DEFAULT -> WEAKEST
    WEAKEST -> WEAKEST
  }

  companion object {
    fun stronger(s1: Strength, s2: Strength) = s1.strengthValue < s2.strengthValue
    fun weaker(s1: Strength, s2: Strength) = s1.strengthValue > s2.strengthValue
    fun weakestOf(s1: Strength, s2: Strength) = if (weaker(s1, s2)) s1 else s2
  }
}

/* --- *
 * C o n s t r a i n t
 * --- */

/**
 * An abstract class representing a system-maintainable relationship
 * (or "constraint") between a set of variables. A constraint supplies
 * a strength instance variable concrete subclasses provide a means
 * of storing the constrained variables and other information required
 * to represent a constraint.
 */
abstract class Constraint(val strength: Strength) {
  abstract fun addToGraph()
  abstract fun removeFromGraph()
  abstract fun isSatisfied() : Boolean
  abstract fun chooseMethod(mark: Int)
  abstract fun markInputs(mark: Int)
  abstract fun output(): Variable
  abstract fun markUnsatisfied()
  abstract fun recalculate()
  abstract fun execute()
  abstract fun inputsKnown(mark: Int): Boolean

  /**
   * Attempt to find a way to enforce this constraint. If successful,
   * record the solution, perhaps modifying the current dataflow
   * graph. Answer the constraint that this constraint overrides, if
   * there is one, or nil, if there isn't.
   * Assume: I am not already satisfied.
   */
  fun satisfy(mark: Int, planner: Planner): Constraint? {
    chooseMethod(mark)
    if (!isSatisfied()) {
      if (strength == Strength.REQUIRED) alert("Could not satisfy a required constraint!")
      return null
    }
    markInputs(mark)
    val out = this.output()
    val overridden = out.determinedBy
    if (overridden != null) overridden.markUnsatisfied()
    out.determinedBy = this
    if (!planner.addPropagate(this, mark))
      alert("Cycle encountered")
    out.mark = mark
    return overridden
  }

  fun destroyConstraint(planner: Planner) {
    if (isSatisfied()) planner.incrementalRemove(this)
    else removeFromGraph()
  }

  /**
   * Normal constraints are not input constraints.  An input constraint
   * is one that depends on external state, such as the mouse, the
   * keybord, a clock, or some arbitraty piece of imperative code.
   */
  open fun isInput() = false
}

/* --- *
 * U n a r y   C o n s t r a i n t
 * --- */

/**
 * Abstract superclass for constraints having a single possible output
 * variable.
 */
abstract class UnaryConstraint(val myOutput: Variable, strength: Strength) : Constraint(strength) {
  var satisfied = false

  /**
   * Adds this constraint to the constraint graph
   */
  override fun addToGraph() {
    myOutput.addConstraint(this)
    satisfied = false
  }

  /**
   * Decides if this constraint can be satisfied and records that
   * decision.
   */
  override fun chooseMethod(mark: Int) {
    satisfied = (myOutput.mark != mark)
      && Strength.stronger(strength, myOutput.walkStrength)
  }

  /**
   * Returns true if this constraint is satisfied in the current solution.
   */
  override fun isSatisfied() = satisfied

  override fun markInputs(mark: Int) {
    // has no inputs
  }

  /**
   * Returns the current output variable.
   */
  override fun output() = myOutput

  /**
   * Calculate the walkabout strength, the stay flag, and, if it is
   * 'stay', the value for the current output of this constraint. Assume
   * this constraint is satisfied.
   */
  override fun recalculate() {
    myOutput.walkStrength = strength
    myOutput.stay = !isInput()
    if (myOutput.stay) execute() // Stay optimization
  }

  /**
   * Records that this constraint is unsatisfied
   */
  override fun markUnsatisfied() {
    this.satisfied = false
  }

  override fun inputsKnown(mark: Int) = true

  override fun removeFromGraph() {
//  if (myOutput != null)
    myOutput.removeConstraint(this)
    satisfied = false
  }
}

/* --- *
 * S t a y   C o n s t r a i n t
 * --- */

/**
 * Variables that should, with some level of preference, stay the same.
 * Planners may exploit the fact that instances, if satisfied, will not
 * change their output during plan execution.  This is called "stay
 * optimization".
 */
class StayConstraint(v: Variable, str: Strength) : UnaryConstraint(v, str) {
  override fun execute() {
    // Stay constraints do nothing
  }
}

/* --- *
 * E d i t   C o n s t r a i n t
 * --- */

/**
 * A unary input constraint used to mark a variable that the client
 * wishes to change.
 */
class EditConstraint(v: Variable, str: Strength) : UnaryConstraint(v, str) {
  /**
   * Edits indicate that a variable is to be changed by imperative code.
   */
  override fun isInput() = true

  override fun execute() {
    // Edit constraints do nothing
  }
}

/* --- *
 * B i n a r y   C o n s t r a i n t
 * --- */

enum class Direction {
  BACKWARD, // = -1
  NONE, //     = 0
  FORWARD //   = 1
}

/**
 * Abstract superclass for constraints having two possible output
 * variables.
 */
abstract class BinaryConstraint(val v1: Variable, val v2: Variable, strength: Strength) : Constraint(strength) {
  var direction = Direction.NONE

  /**
   * Decides if this constraint can be satisfied and which way it
   * should flow based on the relative strength of the variables related,
   * and record that decision.
   */
  override fun chooseMethod(mark: Int) {
    if (v1.mark == mark) {
      direction = if (v2.mark != mark && Strength.stronger(strength, v2.walkStrength))
        Direction.FORWARD else Direction.NONE
    }
    if (v2.mark == mark) {
      direction = if (v1.mark != mark && Strength.stronger(strength, v1.walkStrength))
        Direction.BACKWARD else Direction.NONE
    }
    if (Strength.weaker(v1.walkStrength, v2.walkStrength)) {
      direction = if (Strength.stronger(strength, v1.walkStrength))
        Direction.BACKWARD else Direction.NONE
    } else {
      direction = if (Strength.stronger(strength, v2.walkStrength))
        Direction.FORWARD else Direction.BACKWARD
    }
  }

  /**
   * Add this constraint to the constraint graph
   */
  override fun addToGraph() {
    v1.addConstraint(this)
    v2.addConstraint(this)
    direction = Direction.NONE
  }

  /**
   * Answer true if this constraint is satisfied in the current solution.
   */
  override fun isSatisfied() = direction != Direction.NONE

  /**
   * Mark the input variable with the given mark.
   */
  override fun markInputs(mark: Int) {
    input().mark = mark
  }

  /**
   * Returns the current input variable
   */
  fun input() = if (direction == Direction.FORWARD) v1 else v2

  /**
   * Returns the current output variable
   */
  override fun output() = if (direction == Direction.FORWARD) v2 else v1

  /**
   * Calculate the walkabout strength, the stay flag, and, if it is
   * 'stay', the value for the current output of this
   * constraint. Assume this constraint is satisfied.
   */
  override fun recalculate() {
    val ihn = input()
    val out = output()
    out.walkStrength = Strength.weakestOf(this.strength, ihn.walkStrength)
    out.stay = ihn.stay
    if (out.stay) execute()
  }

  /**
   * Record the fact that this constraint is unsatisfied.
   */
  override fun markUnsatisfied() {
    direction = Direction.NONE
  }

  override fun inputsKnown(mark: Int): Boolean {
    val i = this.input()
    return i.mark == mark || i.stay || i.determinedBy == null
  }

  override fun removeFromGraph() {
//    if (v1 != null)
      v1.removeConstraint(this)
//    if (v2 != null)
      v2.removeConstraint(this)
    this.direction = Direction.NONE
  }
}

/* --- *
 * S c a l e   C o n s t r a i n t
 * --- */

/**
 * Relates two variables by the linear scaling relationship: "v2 =
 * (v1 * scale) + offset". Either v1 or v2 may be changed to maintain
 * this relationship but the scale factor and offset are considered
 * read-only.
 */
class ScaleConstraint(src: Variable, val scale: Variable, val offset: Variable, dest: Variable, strength: Strength): BinaryConstraint(src, dest, strength) {
  /**
   * Adds this constraint to the constraint graph.
   */
  override fun addToGraph() {
    super.addToGraph()
    scale.addConstraint(this)
    offset.addConstraint(this)
  }

  override fun removeFromGraph() {
    super.removeFromGraph()
//    if (this.scale != null)
      scale.removeConstraint(this)
//    if (this.offset != null)
      offset.removeConstraint(this)
  }

  override fun markInputs(mark: Int) {
    super.markInputs(mark)
    scale.mark = mark
    offset.mark = mark
  }

  /**
   * Enforce this constraint. Assume that it is satisfied.
   */
  override fun execute() {
    if (direction == Direction.FORWARD) {
      v2.value = v1.value * scale.value + offset.value
    } else {
      v1.value = (v2.value - offset.value) / scale.value
    }
  }

  /**
   * Calculate the walkabout strength, the stay flag, and, if it is
   * 'stay', the value for the current output of this constraint. Assume
   * this constraint is satisfied.
   */
  override fun recalculate() {
    val ihn = input()
    val out = output()
    out.walkStrength = Strength.weakestOf(strength, ihn.walkStrength)
    out.stay = ihn.stay && scale.stay && offset.stay
    if (out.stay) execute()
  }
}

/* --- *
 * E q u a l i t  y   C o n s t r a i n t
 * --- */

/**
 * Constrains two variables to have the same value.
 */
class EqualityConstraint(var1: Variable, var2: Variable, strength: Strength): BinaryConstraint(var1, var2, strength) {
  /**
   * Enforce this constraint. Assume that it is satisfied.
   */
  override fun execute() {
    output().value = input().value
  }
}

/* --- *
 * V a r i a b l e
 * --- */

/**
 * A constrained variable. In addition to its value, it maintain the
 * structure of the constraint graph, the current dataflow graph, and
 * various parameters of interest to the DeltaBlue incremental
 * constraint solver.
 **/
class Variable(val name: String, var value : Int = 0) {
  val constraints = OrderedCollection<Constraint>()
  var determinedBy: Constraint? = null
  var mark = 0
  var walkStrength = Strength.WEAKEST
  var stay = true

  /**
   * Add the given constraint to the set of all constraints that refer
   * this variable.
   */
  fun addConstraint(c: Constraint) = constraints.add(c)

  /**
   * Removes all traces of c from this variable.
   */
  fun removeConstraint(c: Constraint) {
    constraints.remove(c)
    if (determinedBy == c) determinedBy = null
  }
}

/* --- *
 * P l a n n e r
 * --- */

/**
 * The DeltaBlue planner
 */
class Planner {
  var currentMark = 0

  /**
   * Activate the constraint and attempt to satisfy it.
   */
  fun add(c: Constraint) {
    c.addToGraph()
    incrementalAdd(c)
  }

  /**
   * Attempt to satisfy the given constraint and, if successful,
   * incrementally update the dataflow graph.  Details: If satifying
   * the constraint is successful, it may override a weaker constraint
   * on its output. The algorithm attempts to resatisfy that
   * constraint using some other method. This process is repeated
   * until either a) it reaches a variable that was not previously
   * determined by any constraint or b) it reaches a constraint that
   * is too weak to be satisfied using any of its methods. The
   * variables of constraints that have been processed are marked with
   * a unique mark value so that we know where we've been. This allows
   * the algorithm to avoid getting into an infinite loop even if the
   * constraint graph has an inadvertent cycle.
   */
  fun incrementalAdd(c: Constraint) {
    val mark = newMark()
    var overridden = c.satisfy(mark, this)
    while (overridden != null)
      overridden = overridden.satisfy(mark, this)
  }

  /**
   * Entry point for retracting a constraint. Remove the given
   * constraint and incrementally update the dataflow graph.
   * Details: Retracting the given constraint may allow some currently
   * unsatisfiable downstream constraint to be satisfied. We therefore collect
   * a list of unsatisfied downstream constraints and attempt to
   * satisfy each one in turn. This list is traversed by constraint
   * strength, strongest first, as a heuristic for avoiding
   * unnecessarily adding and then overriding weak constraints.
   * Assume: c is satisfied.
   */
  fun incrementalRemove(c: Constraint) {
    val out = c.output()
    c.markUnsatisfied()
    c.removeFromGraph()
    var unsatisfied = removePropagateFrom(out)
    var strength = Strength.REQUIRED
    do {
      for (u in unsatisfied) {
        if (u.strength == strength)
          this.incrementalAdd(u)
      }
      strength = strength.nextWeaker()
    } while (strength != Strength.WEAKEST)
  }

  /**
   * Select a previously unused mark value.
   */
  fun newMark() = ++currentMark

  /**
   * Extract a plan for resatisfaction starting from the given source
   * constraints, usually a set of input constraints. This method
   * assumes that stay optimization is desired the plan will contain
   * only constraints whose output variables are not stay. Constraints
   * that do no computation, such as stay and edit constraints, are
   * not included in the plan.
   * Details: The outputs of a constraint are marked when it is added
   * to the plan under construction. A constraint may be appended to
   * the plan when all its input variables are known. A variable is
   * known if either a) the variable is marked (indicating that has
   * been computed by a constraint appearing earlier in the plan), b)
   * the variable is 'stay' (i.e. it is a constant at plan execution
   * time), or c) the variable is not determined by any
   * constraint. The last provision is for past states of history
   * variables, which are not stay but which are also not computed by
   * any constraint.
   * Assume: sources are all satisfied.
   */
  fun makePlan(sources: OrderedCollection<Constraint>): Plan {
    var mark = this.newMark()
    var plan = Plan()
    var todo = sources
    while (todo.size() > 0) {
      var c = todo.removeFirst()
      if (c.output().mark != mark && c.inputsKnown(mark)) {
        plan.addConstraint(c)
        c.output().mark = mark
        addConstraintsConsumingTo(c.output(), todo)
      }
    }
    return plan
  }

  /**
   * Extract a plan for resatisfying starting from the output of the
   * given constraints, usually a set of input constraints.
   */
  fun extractPlanFromConstraints(constraints: OrderedCollection<Constraint>): Plan {
    val sources = OrderedCollection<Constraint>()
    for (c in constraints) {
      if (c.isInput() && c.isSatisfied())
        // not in plan already and eligible for inclusion
        sources.add(c)
    }
    return makePlan(sources)
  }

  /**
   * Recompute the walkabout strengths and stay flags of all variables
   * downstream of the given constraint and recompute the actual
   * values of all variables whose stay flag is true. If a cycle is
   * detected, remove the given constraint and answer
   * false. Otherwise, answer true.
   * Details: Cycles are detected when a marked variable is
   * encountered downstream of the given constraint. The sender is
   * assumed to have marked the inputs of the given constraint with
   * the given mark. Thus, encountering a marked node downstream of
   * the output constraint means that there is a path from the
   * constraint's output to one of its inputs.
   */
  fun addPropagate(c: Constraint, mark: Int): Boolean {
    val todo = OrderedCollection<Constraint>()
    todo.add(c)
    while (todo.size() > 0) {
      var d = todo.removeFirst()
      if (d.output().mark == mark) {
        incrementalRemove(c)
        return false
      }
      d.recalculate()
      addConstraintsConsumingTo(d.output(), todo)
    }
    return true
  }

  /**
   * Update the walkabout strengths and stay flags of all variables
   * downstream of the given constraint. Answer a collection of
   * unsatisfied constraints sorted in order of decreasing strength.
   */
  fun removePropagateFrom(out: Variable): OrderedCollection<Constraint> {
    out.determinedBy = null
    out.walkStrength = Strength.WEAKEST
    out.stay = true
    val unsatisfied = OrderedCollection<Constraint>()
    val todo = OrderedCollection<Variable>()
    todo.add(out)
    while (todo.size() > 0) {
      var v = todo.removeFirst()
      for (c in v.constraints) {
        if (!c.isSatisfied())
          unsatisfied.add(c)
      }
      var determining = v.determinedBy
      for (next in v.constraints) {
        if (next != determining && next.isSatisfied()) {
          next.recalculate()
          todo.add(next.output())
        }
      }
    }
    return unsatisfied
  }

  fun addConstraintsConsumingTo(v: Variable, coll: OrderedCollection<Constraint>) {
    var determining = v.determinedBy
    for (c in v.constraints) {
      if (c != determining && c.isSatisfied())
        coll.add(c)
    }
  }

  fun change(v: Variable, newValue: Int) {
    val edit = EditConstraint(v, Strength.PREFERRED)
    add(edit)
    val edits = OrderedCollection<Constraint>()
    edits.add(edit)
    val plan = extractPlanFromConstraints(edits)
    for (i in 0 until 10) {
      v.value = newValue
      plan.execute()
    }
    edit.destroyConstraint(this)
  }
}

/* --- *
 * P l a n
 * --- */

/**
 * A Plan is an ordered list of constraints to be executed in sequence
 * to resatisfy all currently satisfiable constraints in the face of
 * one or more changing inputs.
 */
class Plan {
  val v = OrderedCollection<Constraint>()

  fun addConstraint(c: Constraint) = v.add(c)
  fun size() = v.size()
  fun constraintAt(index: Int) = v.at(index)

  fun execute() {
    for (c in v) {
      c.execute()
    }
  }
}

/* --- *
 * M a i n
 * --- */

class DeltaBlueBenchmark {
  fun deltaBlue() {
    chainTest(100)
    projectionTest(100)
  }
  /**
   * This is the standard DeltaBlue benchmark. A long chain of equality
   * constraints is constructed with a stay constraint on one end. An
   * edit constraint is then added to the opposite end and the time is
   * measured for adding and removing this constraint, and extracting
   * and executing a constraint satisfaction plan. There are two cases.
   * In case 1, the added constraint is stronger than the stay
   * constraint and values must propagate down the entire length of the
   * chain. In case 2, the added constraint is weaker than the stay
   * constraint so it cannot be accomodated. The cost in this case is,
   * of course, very low. Typical situations lie somewhere between these
   * two extremes.
   */
  fun chainTest(n: Int) {
    val planner = Planner()
    val variables = (0..n).map{ Variable("v$it") }.toList()
    var first = variables.first()
    var last = variables.last()
    // Build chain of n equality constraints
    variables.windowed(2) {
      (v1, v2) -> planner.add(EqualityConstraint(v1, v2, Strength.REQUIRED))
    }

    planner.add(StayConstraint(last, Strength.STRONG_DEFAULT))
    val edit = EditConstraint(first, Strength.PREFERRED)
    planner.add(edit)
    val edits = OrderedCollection<Constraint>()
    edits.add(edit)
    val plan = planner.extractPlanFromConstraints(edits)
    for (i in 0 until 100) {
      first.value = i
      plan.execute()
      if (last.value != i)
      alert("Chain test failed.")
    }
  }

  /**
   * This test constructs a two sets of variables related to each
   * other by a simple linear transformation (scale and offset). The
   * time is measured to change a variable on either side of the
   * mapping and to change the scale and offset factors.
   */
  fun projectionTest(n: Int) {
    val planner = Planner()
    var scale = Variable("scale", 10)
    var offset = Variable("offset", 1000)
    var src: Variable? = null
    var dst: Variable? = null

    var dests = OrderedCollection<Variable>()
    for (i in 0 until n) {
      src = Variable("src$i", i)
      dst = Variable("dst$i", i)
      dests.add(dst)
      planner.add(StayConstraint(src, Strength.NORMAL))
      planner.add(ScaleConstraint(src, scale, offset, dst, Strength.REQUIRED))
    }

    planner.change(src!!, 17)
    if (dst!!.value != 1170) alert("Projection 1 failed")
    planner.change(dst, 1050)
    if (src.value != 5) alert("Projection 2 failed")
    planner.change(scale, 5)
    for (i in 0 until n - 1) {
      if (dests.at(i).value != i * 5 + 1000)
      alert("Projection 3 failed")
    }
    planner.change(offset, 2000)
    for (i in 0 until n - 1) {
      if (dests.at(i).value != i * 5 + 2000)
      alert("Projection 4 failed")
    }
  }
}
