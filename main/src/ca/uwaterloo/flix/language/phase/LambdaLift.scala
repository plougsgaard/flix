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

import ca.uwaterloo.flix.language.GenSym
import ca.uwaterloo.flix.language.ast.SimplifiedAst.Expression
import ca.uwaterloo.flix.language.ast.{Ast, SimplifiedAst, Symbol}
import ca.uwaterloo.flix.util.InternalCompilerException

import scala.collection.mutable

object LambdaLift {

  /**
    * Mutable map of top level definitions.
    */
  private type TopLevel = mutable.Map[Symbol.DefnSym, SimplifiedAst.Definition.Constant]

  /**
    * Performs lambda lifting on all definitions in the AST.
    */
  def lift(root: SimplifiedAst.Root)(implicit genSym: GenSym): SimplifiedAst.Root = {
    val t = System.nanoTime()

    // A mutable map to hold lambdas that are lifted to the top level.
    val m: TopLevel = mutable.Map.empty

    val definitions = root.definitions.map {
      case (name, decl) => name -> lift(decl, m)
    }
    val properties = root.properties.map(p => lift(p, m))

    // Return the updated AST root.
    val e = System.nanoTime() - t
    root.copy(definitions = definitions ++ m, properties = properties, time = root.time.copy(lambdaLift = e))
  }

  /**
    * Performs lambda lifting on the given declaration `decl`.
    *
    * The definition's expression is closure converted, and then lifted definitions are added to the mutable map `m`.
    * The updated definition is then returned.
    */
  private def lift(decl: SimplifiedAst.Definition.Constant, m: TopLevel)(implicit genSym: GenSym): SimplifiedAst.Definition.Constant = {
    val convExp = ClosureConv.convert(decl.exp)
    val liftExp = lift(convExp, m, Some(decl.sym))
    decl.copy(exp = liftExp)
  }

  /**
    * Performs lambda lifting on the given property `prop`.
    *
    * The property's expression is closure converted, and then the lifted definitions are added to the mutable map `m`.
    * The updated definition is then returned.
    */
  private def lift(prop: SimplifiedAst.Property, m: TopLevel)(implicit genSym: GenSym): SimplifiedAst.Property = {
    val convExp = ClosureConv.convert(prop.exp)
    val liftExp = lift(convExp, m, None)
    prop.copy(exp = liftExp)
  }

  /**
    * Performs lambda lifting on the given expression `exp0`.
    *
    * Adds new top-level definitions to the mutable map `m`.
    */
  private def lift(exp0: Expression, m: TopLevel, symOpt: Option[Symbol.DefnSym])(implicit genSym: GenSym): Expression = {
    def visit(e: Expression): Expression = e match {
      case Expression.Unit => e
      case Expression.True => e
      case Expression.False => e
      case Expression.Char(lit) => e
      case Expression.Float32(lit) => e
      case Expression.Float64(lit) => e
      case Expression.Int8(lit) => e
      case Expression.Int16(lit) => e
      case Expression.Int32(lit) => e
      case Expression.Int64(lit) => e
      case Expression.BigInt(lit) => e
      case Expression.Str(lit) => e
      case Expression.LoadBool(n, o) => e
      case Expression.LoadInt8(b, o) => e
      case Expression.LoadInt16(b, o) => e
      case Expression.LoadInt32(b, o) => e
      case Expression.StoreBool(b, o, v) => e
      case Expression.StoreInt8(b, o, v) => e
      case Expression.StoreInt16(b, o, v) => e
      case Expression.StoreInt32(b, o, v) => e
      case Expression.Var(sym, tpe, loc) => e
      case Expression.Ref(name, tpe, loc) => e

      case Expression.Lambda(fparams, body, tpe, loc) =>
        // Lift the lambda to a top-level definition, and replacing the Lambda expression with a Ref.

        // First, recursively visit the lambda body, lifting any inner lambdas.
        val liftedBody = visit(body)

        // Generate a fresh symbol for the definition.
        val freshSymbol = symOpt match {
          case None => Symbol.freshDefnSym("none") // TODO: This seems suspicious.
          case Some(oldSym) => Symbol.freshDefnSym(oldSym)
        }

        // Generate fresh symbols for each of the formal parameters.
        val subst = mutable.Map.empty[Symbol.VarSym, Symbol.VarSym]
        val freshParams = fparams map {
          case SimplifiedAst.FormalParam(oldSym, paramType) =>
            val newSym = Symbol.freshVarSym(oldSym)
            subst += (oldSym -> newSym)
            SimplifiedAst.FormalParam(newSym, paramType)
        }

        // Replace old symbols by fresh symbols in the expression body.
        val freshBody = replace(liftedBody, subst.toMap)

        // Create a new top-level definition, using the fresh name and lifted body.
        val defn = SimplifiedAst.Definition.Constant(Ast.Annotations(Nil), freshSymbol, freshParams, freshBody, isSynthetic = true, tpe, loc)

        // Update the map that holds newly-generated definitions
        m += (freshSymbol -> defn)

        // Finally, replace this current Lambda node with a Ref to the newly-generated name.
        SimplifiedAst.Expression.Ref(freshSymbol, tpe, loc)

      case Expression.Hook(hook, tpe, loc) => e
      case Expression.MkClosureRef(ref, freeVars, tpe, loc) => e

      case SimplifiedAst.Expression.MkClosure(lambda, freeVars, tpe, loc) =>
        // Replace the MkClosure node with a MkClosureRef node, since the Lambda has been replaced by a Ref.
        visit(lambda) match {
          case ref: SimplifiedAst.Expression.Ref =>
            SimplifiedAst.Expression.MkClosureRef(ref, freeVars, tpe, loc)
          case _ => throw InternalCompilerException(s"Unexpected expression: '$lambda'.")
        }

      case Expression.ApplyRef(name, args, tpe, loc) =>
        Expression.ApplyRef(name, args.map(visit), tpe, loc)
      case Expression.ApplyTail(name, formals, args, tpe, loc) =>
        Expression.ApplyTail(name, formals, args.map(visit), tpe, loc)
      case Expression.ApplyHook(hook, args, tpe, loc) =>
        Expression.ApplyHook(hook, args.map(visit), tpe, loc)
      case Expression.Apply(exp, args, tpe, loc) =>
        Expression.Apply(visit(exp), args.map(visit), tpe, loc)
      case Expression.Unary(op, exp, tpe, loc) =>
        Expression.Unary(op, visit(exp), tpe, loc)
      case Expression.Binary(op, exp1, exp2, tpe, loc) =>
        Expression.Binary(op, visit(exp1), visit(exp2), tpe, loc)
      case Expression.IfThenElse(exp1, exp2, exp3, tpe, loc) =>
        Expression.IfThenElse(visit(exp1), visit(exp2), visit(exp3), tpe, loc)
      case Expression.Let(sym, exp1, exp2, tpe, loc) =>
        Expression.Let(sym, visit(exp1), visit(exp2), tpe, loc)
      case Expression.CheckTag(tag, exp, loc) =>
        Expression.CheckTag(tag, visit(exp), loc)
      case Expression.GetTagValue(tag, exp, tpe, loc) =>
        Expression.GetTagValue(tag, visit(exp), tpe, loc)
      case Expression.Tag(enum, tag, exp, tpe, loc) =>
        Expression.Tag(enum, tag, visit(exp), tpe, loc)
      case Expression.GetTupleIndex(exp, offset, tpe, loc) =>
        Expression.GetTupleIndex(visit(exp), offset, tpe, loc)
      case Expression.Tuple(elms, tpe, loc) =>
        Expression.Tuple(elms.map(visit), tpe, loc)
      case Expression.FSet(elms, tpe, loc) =>
        Expression.FSet(elms.map(visit), tpe, loc)
      case Expression.Existential(params, exp, loc) =>
        Expression.Existential(params, visit(exp), loc)
      case Expression.Universal(params, exp, loc) =>
        Expression.Universal(params, visit(exp), loc)
      case Expression.UserError(tpe, loc) => e
      case Expression.MatchError(tpe, loc) => e
      case Expression.SwitchError(tpe, loc) => e
    }

    visit(exp0)
  }

  /**
    * Applies the given substitution map `subst` to the given expression `e`.
    */
  private def replace(e0: Expression, subst: Map[Symbol.VarSym, Symbol.VarSym]): Expression = {
    def visit(e: Expression): Expression = e match {
      case Expression.Unit => e
      case Expression.True => e
      case Expression.False => e
      case Expression.Char(lit) => e
      case Expression.Float32(lit) => e
      case Expression.Float64(lit) => e
      case Expression.Int8(lit) => e
      case Expression.Int16(lit) => e
      case Expression.Int32(lit) => e
      case Expression.Int64(lit) => e
      case Expression.BigInt(lit) => e
      case Expression.Str(lit) => e
      case Expression.LoadBool(n, o) => e
      case Expression.LoadInt8(b, o) => e
      case Expression.LoadInt16(b, o) => e
      case Expression.LoadInt32(b, o) => e
      case Expression.StoreBool(b, o, v) => e
      case Expression.StoreInt8(b, o, v) => e
      case Expression.StoreInt16(b, o, v) => e
      case Expression.StoreInt32(b, o, v) => e
      case Expression.Var(sym, tpe, loc) => subst.get(sym) match {
        case None => Expression.Var(sym, tpe, loc)
        case Some(newSym) => Expression.Var(newSym, tpe, loc)
      }
      case Expression.Ref(name, tpe, loc) => e
      case Expression.Lambda(fparams, exp, tpe, loc) =>
        val fs = replace(fparams, subst)
        val e = visit(exp)
        Expression.Lambda(fs, e, tpe, loc)
      case Expression.Hook(hook, tpe, loc) => e
      case Expression.MkClosureRef(ref, freeVars, tpe, loc) => e
      case Expression.MkClosure(exp, freeVars, tpe, loc) =>
        val e = visit(exp).asInstanceOf[Expression.Lambda]
        Expression.MkClosure(e, freeVars, tpe, loc)
      case Expression.ApplyRef(sym, args, tpe, loc) =>
        val as = args map visit
        Expression.ApplyRef(sym, as, tpe, loc)
      case Expression.ApplyTail(sym, fparams, args, tpe, loc) =>
        val as = args map visit
        Expression.ApplyTail(sym, fparams, as, tpe, loc)
      case Expression.ApplyHook(hook, args, tpe, loc) =>
        val as = args map visit
        Expression.ApplyHook(hook, as, tpe, loc)
      case Expression.Apply(exp, args, tpe, loc) =>
        val e = visit(exp)
        val as = args map visit
        Expression.Apply(e, as, tpe, loc)
      case Expression.Unary(op, exp, tpe, loc) =>
        val e = visit(exp)
        Expression.Unary(op, e, tpe, loc)
      case Expression.Binary(op, exp1, exp2, tpe, loc) =>
        val e1 = visit(exp1)
        val e2 = visit(exp2)
        Expression.Binary(op, e1, e2, tpe, loc)
      case Expression.IfThenElse(exp1, exp2, exp3, tpe, loc) =>
        val e1 = visit(exp1)
        val e2 = visit(exp2)
        val e3 = visit(exp3)
        Expression.IfThenElse(e1, e2, e3, tpe, loc)
      case Expression.Let(sym, exp1, exp2, tpe, loc) =>
        val e1 = visit(exp1)
        val e2 = visit(exp2)
        subst.get(sym) match {
          case None => Expression.Let(sym, e1, e2, tpe, loc)
          case Some(newSym) => Expression.Let(newSym, e1, e2, tpe, loc)
        }
      case Expression.CheckTag(tag, exp, loc) =>
        val e = visit(exp)
        Expression.CheckTag(tag, e, loc)
      case Expression.GetTagValue(tag, exp, tpe, loc) =>
        val e = visit(exp)
        Expression.GetTagValue(tag, e, tpe, loc)
      case Expression.Tag(enum, tag, exp, tpe, loc) =>
        val e = visit(exp)
        Expression.Tag(enum, tag, e, tpe, loc)
      case Expression.GetTupleIndex(exp, offset, tpe, loc) =>
        val e = visit(exp)
        Expression.GetTupleIndex(e, offset, tpe, loc)
      case Expression.Tuple(elms, tpe, loc) =>
        val es = elms map visit
        Expression.Tuple(es, tpe, loc)
      case Expression.FSet(elms, tpe, loc) =>
        val es = elms map visit
        Expression.FSet(es, tpe, loc)
      case Expression.Existential(fparams, exp, loc) =>
        val fs = replace(fparams, subst)
        val e = visit(exp)
        Expression.Existential(fs, e, loc)
      case Expression.Universal(fparams, exp, loc) =>
        val fs = replace(fparams, subst)
        val e = visit(exp)
        Expression.Universal(fs, e, loc)
      case Expression.UserError(tpe, loc) => e
      case Expression.MatchError(tpe, loc) => e
      case Expression.SwitchError(tpe, loc) => e
    }

    visit(e0)
  }

  /**
    * Applies the given substitution map `subst` to the given formal parameters `fs`.
    */
  private def replace(fs: List[SimplifiedAst.FormalParam], subst: Map[Symbol.VarSym, Symbol.VarSym]): List[SimplifiedAst.FormalParam] = fs map {
    case SimplifiedAst.FormalParam(sym, tpe) =>
      subst.get(sym) match {
        case None => SimplifiedAst.FormalParam(sym, tpe)
        case Some(newSym) => SimplifiedAst.FormalParam(newSym, tpe)
      }
  }

}
