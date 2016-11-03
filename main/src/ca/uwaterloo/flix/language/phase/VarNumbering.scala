/*
 * Copyright 2015-2016 Ming-Ho Yee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.uwaterloo.flix.language.phase

import ca.uwaterloo.flix.language.ast.{Ast, SimplifiedAst, Type}
import ca.uwaterloo.flix.util.InternalCompilerException

import scala.collection.mutable

object VarNumbering {

  /**
    * A wrapper for the map of variable names to variable offsets.
    *
    * The wrapper provides `get` and `set` methods for looking up or adding new variable numbers.
    * Internally, this class also tracks the next offset to be assigned, incrementing it by 1 (2 for Int64 and Float64)
    * for every new variable added.
    */
  class NumberingsMap {
    private[this] val map: mutable.Map[String, Int] = mutable.Map.empty
    private[this] var offset = 0

    def get(s: String): Int = map(s)

    // Returns the offset that was just added.
    def set(s: String, t: Type): Int = {
      map(s) = offset
      val old = offset
      t match {
        case Type.Int64 | Type.Float64 => offset += 2
        case _ => offset += 1
      }
      old
    }
  }

  /**
    * Iterate over all top-level definitions and number all variables
    */
  def number(root: SimplifiedAst.Root): SimplifiedAst.Root = {
    val t = System.nanoTime()
    val defs = root.definitions.map { case (name, defn) => name -> number(defn) }
    val e = System.nanoTime() - t
    root.copy(definitions = defs, time = root.time.copy(varNumbering = e))
  }

  /**
    * Numbers each variable with an offset so that we can generate code.
    *
    * On the JVM, each method has a local variable array, and each variable is referenced by a 0-based offset. The first
    * few slots in the array are initialized to the values of the parameters. Normally each value takes up a single
    * slot, but longs and doubles require two consecutive slots. Thus, the n-th variable may not necessarily be the
    * n-th slot.
    *
    * This phase assumes closures have already been converted and lambdas have been lifted.
    */
  def number(decl: SimplifiedAst.Definition.Constant): SimplifiedAst.Definition.Constant = {
    def visit(m: NumberingsMap, e: SimplifiedAst.Expression): SimplifiedAst.Expression = e match {
      case SimplifiedAst.Expression.Unit => e
      case SimplifiedAst.Expression.True => e
      case SimplifiedAst.Expression.False => e
      case SimplifiedAst.Expression.Char(lit) => e
      case SimplifiedAst.Expression.Float32(lit) => e
      case SimplifiedAst.Expression.Float64(lit) => e
      case SimplifiedAst.Expression.Int8(lit) => e
      case SimplifiedAst.Expression.Int16(lit) => e
      case SimplifiedAst.Expression.Int32(lit) => e
      case SimplifiedAst.Expression.Int64(lit) => e
      case SimplifiedAst.Expression.BigInt(lit) => e
      case SimplifiedAst.Expression.Str(lit) => e
      case SimplifiedAst.Expression.LoadBool(n, o) => e
      case SimplifiedAst.Expression.LoadInt8(b, o) => e
      case SimplifiedAst.Expression.LoadInt16(b, o) => e
      case SimplifiedAst.Expression.LoadInt32(b, o) => e
      case SimplifiedAst.Expression.StoreBool(b, o, v) => e
      case SimplifiedAst.Expression.StoreInt8(b, o, v) => e
      case SimplifiedAst.Expression.StoreInt16(b, o, v) => e
      case SimplifiedAst.Expression.StoreInt32(b, o, v) => e

      case SimplifiedAst.Expression.Var(sym, tpe, loc) => SimplifiedAst.Expression.Var(sym, tpe, loc)

      case SimplifiedAst.Expression.Ref(name, tpe, loc) => e
      case SimplifiedAst.Expression.Lambda(args, body, tpe, loc) =>
        throw InternalCompilerException("Lambdas should have been converted to closures and lifted.")
      case SimplifiedAst.Expression.Hook(hook, tpe, loc) => e
      case mkClosure@SimplifiedAst.Expression.MkClosureRef(ref, freeVars, tpe, loc) =>
        val numberedFreeVars = freeVars.map {
          case SimplifiedAst.FreeVar(sym, tpe) => SimplifiedAst.FreeVar(sym, tpe)
        }
        mkClosure.copy(freeVars = numberedFreeVars)
      case SimplifiedAst.Expression.MkClosure(lambda, freeVars, tpe, loc) =>
        throw InternalCompilerException("MkClosure should have been replaced by MkClosureRef after lambda lifting.")
      case SimplifiedAst.Expression.ApplyRef(name, args, tpe, loc) =>
        SimplifiedAst.Expression.ApplyRef(name, args.map(visit(m, _)), tpe, loc)
      case SimplifiedAst.Expression.ApplyTail(name, formals, actuals, tpe, loc) =>
        SimplifiedAst.Expression.ApplyTail(name, formals, actuals.map(visit(m, _)), tpe, loc)
      case SimplifiedAst.Expression.ApplyHook(hook, args, tpe, loc) =>
        SimplifiedAst.Expression.ApplyHook(hook, args.map(visit(m, _)), tpe, loc)
      case SimplifiedAst.Expression.Apply(exp, args, tpe, loc) =>
        SimplifiedAst.Expression.Apply(visit(m, exp), args.map(visit(m, _)), tpe, loc)
      case SimplifiedAst.Expression.Unary(op, exp, tpe, loc) =>
        SimplifiedAst.Expression.Unary(op, visit(m, exp), tpe, loc)
      case SimplifiedAst.Expression.Binary(op, exp1, exp2, tpe, loc) =>
        SimplifiedAst.Expression.Binary(op, visit(m, exp1), visit(m, exp2), tpe, loc)
      case SimplifiedAst.Expression.IfThenElse(exp1, exp2, exp3, tpe, loc) =>
        SimplifiedAst.Expression.IfThenElse(visit(m, exp1), visit(m, exp2), visit(m, exp3), tpe, loc)

      case SimplifiedAst.Expression.Let(sym, exp1, exp2, tpe, loc) =>
        // First we number the variables in `exp1`. // TODO: No particular reason to do this first...
        val e1 = visit(m, exp1)

        // The let-binding introduces a new variable, so we need to add a new variable/offset to the map.
        val offset = m.set(sym.toString, exp1.tpe)

        // Update the stack offset for the symbol.
        sym.setStackOffset(offset)

        // Then we can use the updated map to number the variables in `exp2`.
        val e2 = visit(m, exp2)

        // Finally we return the updated Let expression.
        SimplifiedAst.Expression.Let(sym, e1, e2, tpe, loc)

      case SimplifiedAst.Expression.CheckTag(tag, exp, loc) =>
        SimplifiedAst.Expression.CheckTag(tag, visit(m, exp), loc)
      case SimplifiedAst.Expression.GetTagValue(tag, exp, tpe, loc) =>
        SimplifiedAst.Expression.GetTagValue(tag, visit(m, exp), tpe, loc)
      case SimplifiedAst.Expression.Tag(enum, tag, exp, tpe, loc) =>
        SimplifiedAst.Expression.Tag(enum, tag, visit(m, exp), tpe, loc)
      case SimplifiedAst.Expression.GetTupleIndex(exp, offset, tpe, loc) =>
        SimplifiedAst.Expression.GetTupleIndex(visit(m, exp), offset, tpe, loc)
      case SimplifiedAst.Expression.Tuple(elms, tpe, loc) =>
        SimplifiedAst.Expression.Tuple(elms.map(visit(m, _)), tpe, loc)
      case SimplifiedAst.Expression.FSet(elms, tpe, loc) =>
        SimplifiedAst.Expression.FSet(elms.map(visit(m, _)), tpe, loc)
      case SimplifiedAst.Expression.Existential(params, exp, loc) => ???
      case SimplifiedAst.Expression.Universal(params, exp, loc) => ???
      case SimplifiedAst.Expression.UserError(tpe, loc) => e
      case SimplifiedAst.Expression.MatchError(tpe, loc) => e
      case SimplifiedAst.Expression.SwitchError(tpe, loc) => e
    }

    // Construct a numbering map to record the offsets
    val m = new NumberingsMap

    // First, we number the parameters
    for (SimplifiedAst.FormalParam(sym, tpe) <- decl.formals) {
      val offset = m.set(sym.toString, tpe)
      sym.setStackOffset(offset)
    }

    // Now we can number the body of the declaration
    val numbered = visit(m, decl.exp)

    // Update and return the top-level definition
    SimplifiedAst.Definition.Constant(Ast.Annotations(Nil), decl.sym, decl.formals, numbered, decl.isSynthetic, decl.tpe, decl.loc)
  }

}
