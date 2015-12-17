package ca.uwaterloo.flix.language.backend.phase

import java.nio.file.{Paths, Files}

import ca.uwaterloo.flix.language.ast.{BinaryOperator, UnaryOperator, Name, SourceLocation}
import ca.uwaterloo.flix.language.backend.ir.ReducedIR.{Definition, LocalVar, Type}
import ca.uwaterloo.flix.language.backend.ir.ReducedIR.Expression._
import ca.uwaterloo.flix.language.backend.ir.ReducedIR.Definition.Function

import org.scalatest.FunSuite

class TestCodegen extends FunSuite {

  val name = Name.Resolved.mk(List("foo", "bar", "main"))
  val name01 = Name.Resolved.mk(List("foo", "bar", "f"))
  val name02 = Name.Resolved.mk(List("foo", "bar", "g"))
  val name03 = Name.Resolved.mk(List("foo", "bar", "h"))

  val loc = SourceLocation.Unknown
  val compiledClassName = "ca.uwaterloo.flix.compiled.FlixDefinitions"

  class CompiledCode(definitions: List[Definition], debug: Boolean = false) {
    object Loader extends ClassLoader {
      def apply(name: String, b: Array[Byte]): Class[_] = {
        defineClass(name, b, 0, b.length)
      }
    }

    val code = Codegen.compile(new Codegen.Context(definitions, compiledClassName.replace(".", "/")))
    if (debug) dumpBytecode()
    val clazz = Loader(compiledClassName, code)

    // Write to a class file, for debugging.
    def dumpBytecode(path: String = "FlixBytecode.class"): Unit = {
      Files.write(Paths.get(path), code)
    }

    def call(name: String, tpes: List[Class[_]] = List(), args: List[Object] = List()): Any = {
      val method = clazz.getMethod(name, tpes: _*)
      method.invoke(null, args: _*)
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // Int constants                                                           //
  /////////////////////////////////////////////////////////////////////////////

  test("Codegen - Const01") {
    val definition = Function(name, args = List(),
      body = Const(42, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate, List())

    assertResult(42)(result)
  }

  test("Codegen - Const02") {
    // Constants -1 to 5 (inclusive) each have their own instruction

    val name_m1 = Name.Resolved.mk(List("foo", "bar", "f_m1"))
    val name_0 = Name.Resolved.mk(List("foo", "bar", "f_0"))
    val name_1 = Name.Resolved.mk(List("foo", "bar", "f_1"))
    val name_2 = Name.Resolved.mk(List("foo", "bar", "f_2"))
    val name_3 = Name.Resolved.mk(List("foo", "bar", "f_3"))
    val name_4 = Name.Resolved.mk(List("foo", "bar", "f_4"))
    val name_5 = Name.Resolved.mk(List("foo", "bar", "f_5"))

    val def_m1 = Function(name_m1, args = List(),
      body = Const(-1, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def_0 = Function(name_0, args = List(),
      body = Const(0, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def_1 = Function(name_1, args = List(),
      body = Const(1, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def_2 = Function(name_2, args = List(),
      body = Const(2, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def_3 = Function(name_3, args = List(),
      body = Const(3, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def_4 = Function(name_4, args = List(),
      body = Const(4, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def_5 = Function(name_5, args = List(),
      body = Const(5, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(def_m1, def_0, def_1, def_2, def_3, def_4, def_5))

    val result_m1 = code.call(name_m1.decorate)
    val result_0 = code.call(name_0.decorate)
    val result_1 = code.call(name_1.decorate)
    val result_2 = code.call(name_2.decorate)
    val result_3 = code.call(name_3.decorate)
    val result_4 = code.call(name_4.decorate)
    val result_5 = code.call(name_5.decorate)

    assertResult(-1)(result_m1)
    assertResult(0)(result_0)
    assertResult(1)(result_1)
    assertResult(2)(result_2)
    assertResult(3)(result_3)
    assertResult(4)(result_4)
    assertResult(5)(result_5)
  }

  test("Codegen - Const03") {
    // Test some constants that are loaded with a BIPUSH, i.e. i <- [Byte.MinValue, -1) UNION (5, Byte,MaxValue]

    val name01 = Name.Resolved.mk(List("foo", "bar", "f01"))
    val name02 = Name.Resolved.mk(List("foo", "bar", "f02"))
    val name03 = Name.Resolved.mk(List("foo", "bar", "f03"))
    val name04 = Name.Resolved.mk(List("foo", "bar", "f04"))
    val name05 = Name.Resolved.mk(List("foo", "bar", "f05"))
    val name06 = Name.Resolved.mk(List("foo", "bar", "f06"))

    val def01 = Function(name01, args = List(),
      body = Const(Byte.MinValue, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def02 = Function(name02, args = List(),
      body = Const(Byte.MinValue + 42, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def03 = Function(name03, args = List(),
      body = Const(-2, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def04 = Function(name04, args = List(),
      body = Const(6, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def05 = Function(name05, args = List(),
      body = Const(Byte.MaxValue - 42, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def06 = Function(name06, args = List(),
      body = Const(Byte.MaxValue, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(def01, def02, def03, def04, def05, def06))

    val result01 = code.call(name01.decorate)
    val result02 = code.call(name02.decorate)
    val result03 = code.call(name03.decorate)
    val result04 = code.call(name04.decorate)
    val result05 = code.call(name05.decorate)
    val result06 = code.call(name06.decorate)

    assertResult(Byte.MinValue)(result01)
    assertResult(Byte.MinValue + 42)(result02)
    assertResult(-2)(result03)
    assertResult(6)(result04)
    assertResult(Byte.MaxValue - 42)(result05)
    assertResult(Byte.MaxValue)(result06)
  }

  test("Codegen - Const04") {
    // Test some constants that are loaded with an SIPUSH, i.e. i <- [Short.MinValue, Byte.MinValue) UNION (Byte.MaxValue, Short,MaxValue]

    val name01 = Name.Resolved.mk(List("foo", "bar", "f01"))
    val name02 = Name.Resolved.mk(List("foo", "bar", "f02"))
    val name03 = Name.Resolved.mk(List("foo", "bar", "f03"))
    val name04 = Name.Resolved.mk(List("foo", "bar", "f04"))
    val name05 = Name.Resolved.mk(List("foo", "bar", "f05"))
    val name06 = Name.Resolved.mk(List("foo", "bar", "f06"))

    val def01 = Function(name01, args = List(),
      body = Const(Short.MinValue, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def02 = Function(name02, args = List(),
      body = Const(Short.MinValue + 42, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def03 = Function(name03, args = List(),
      body = Const(Byte.MinValue - 1, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def04 = Function(name04, args = List(),
      body = Const(Byte.MaxValue + 1, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def05 = Function(name05, args = List(),
      body = Const(Short.MaxValue - 42, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def06 = Function(name06, args = List(),
      body = Const(Short.MaxValue, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(def01, def02, def03, def04, def05, def06))

    val result01 = code.call(name01.decorate)
    val result02 = code.call(name02.decorate)
    val result03 = code.call(name03.decorate)
    val result04 = code.call(name04.decorate)
    val result05 = code.call(name05.decorate)
    val result06 = code.call(name06.decorate)

    assertResult(Short.MinValue)(result01)
    assertResult(Short.MinValue + 42)(result02)
    assertResult(Byte.MinValue - 1)(result03)
    assertResult(Byte.MaxValue + 1)(result04)
    assertResult(Short.MaxValue - 42)(result05)
    assertResult(Short.MaxValue)(result06)
  }

  test("Codegen - Const05") {
    // Test some constants that are loaded with an LDC, i.e. i <- [Int.MinValue, Short.MinValue) UNION (Short.MaxValue, Int,MaxValue]

    val name01 = Name.Resolved.mk(List("foo", "bar", "f01"))
    val name02 = Name.Resolved.mk(List("foo", "bar", "f02"))
    val name03 = Name.Resolved.mk(List("foo", "bar", "f03"))
    val name04 = Name.Resolved.mk(List("foo", "bar", "f04"))
    val name05 = Name.Resolved.mk(List("foo", "bar", "f05"))
    val name06 = Name.Resolved.mk(List("foo", "bar", "f06"))

    val def01 = Function(name01, args = List(),
      body = Const(Int.MinValue, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def02 = Function(name02, args = List(),
      body = Const(Int.MinValue + 42, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def03 = Function(name03, args = List(),
      body = Const(Short.MinValue - 1, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def04 = Function(name04, args = List(),
      body = Const(Short.MaxValue + 1, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def05 = Function(name05, args = List(),
      body = Const(Int.MaxValue - 42, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val def06 = Function(name06, args = List(),
      body = Const(Int.MaxValue, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(def01, def02, def03, def04, def05, def06))

    val result01 = code.call(name01.decorate)
    val result02 = code.call(name02.decorate)
    val result03 = code.call(name03.decorate)
    val result04 = code.call(name04.decorate)
    val result05 = code.call(name05.decorate)
    val result06 = code.call(name06.decorate)

    assertResult(Int.MinValue)(result01)
    assertResult(Int.MinValue + 42)(result02)
    assertResult(Short.MinValue - 1)(result03)
    assertResult(Short.MaxValue + 1)(result04)
    assertResult(Int.MaxValue - 42)(result05)
    assertResult(Int.MaxValue)(result06)
  }

  test("Codegen - Const06") {
    val definition = Function(name, args = List(),
      body = Const(1, Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate, List())

    assertResult(true)(result)
  }

  test("Codegen - Const07") {
    val definition = Function(name, args = List(),
      body = Const(1, Type.Int8, loc),
      Type.Lambda(List(), Type.Int8), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate, List())

    assertResult(1)(result)
  }

  test("Codegen - Const08") {
    val definition = Function(name, args = List(),
      body = Const(1, Type.Int16, loc),
      Type.Lambda(List(), Type.Int16), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate, List())

    assertResult(1)(result)
  }

  test("Codegen - Const09") {
    val definition = Function(name, args = List(),
      body = Const(1, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate, List())

    assertResult(1)(result)
  }

  test("Codegen - Const10") {
    val definition = Function(name, args = List(),
      body = Const(123456789123456789L, Type.Int64, loc),
      Type.Lambda(List(), Type.Int64), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate, List())

    assertResult(123456789123456789L)(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Variables                                                               //
  /////////////////////////////////////////////////////////////////////////////

  test("Codegen - Var01") {
    val definition = Function(name, args = List("x"),
      body = Const(-1, Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val method = code.clazz.getMethod(name.decorate, Integer.TYPE)
    val result = method.invoke(null, 42.asInstanceOf[Object])

    assertResult(-1)(result)
  }

  test("Codegen - Var02") {
    val definition = Function(name, args = List("x"),
      body = Var(LocalVar(0, "x"), Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate, List(Integer.TYPE), List(42).map(_.asInstanceOf[Object]))

    assertResult(42)(result)
  }

  test("Codegen - Var03") {
    val definition = Function(name, args = List("x", "y", "z"),
      body = Var(LocalVar(1, "y"), Type.Int32, loc),
      Type.Lambda(List(Type.Int32, Type.Int32, Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate,
      List(Integer.TYPE, Integer.TYPE, Integer.TYPE),
      List(1337, -101010, 42).map(_.asInstanceOf[Object]))

    assertResult(-101010)(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Let expression                                                          //
  /////////////////////////////////////////////////////////////////////////////

  test("Codegen - Let01") {
    val definition = Function(name, args = List(),
      body = Let(LocalVar(0, "x"), Const(42, Type.Int32, loc),
        Const(-1, Type.Int32, loc), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-1)(result)
  }

  test("Codegen - Let02") {
    val definition = Function(name, args = List(),
      body = Let(LocalVar(0, "x"), Const(42, Type.Int32, loc),
        Var(LocalVar(0, "x"), Type.Int32, loc), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42)(result)
  }

  test("Codegen - Let03") {
    val definition = Function(name, args = List(),
      body = Let(LocalVar(0, "x"), Const(1337, Type.Int32, loc),
        Let(LocalVar(1, "y"), Const(-101010, Type.Int32, loc),
          Let(LocalVar(2, "z"), Const(42, Type.Int32, loc),
            Var(LocalVar(1, "y"), Type.Int32, loc),
            Type.Int32, loc),
          Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-101010)(result)
  }

  test("Codegen - Let04") {
    val definition = Function(name, args = List("a", "b", "c"),
      body = Let(LocalVar(3, "x"), Const(1337, Type.Int32, loc),
        Let(LocalVar(4, "y"), Const(-101010, Type.Int32, loc),
          Let(LocalVar(5, "z"), Const(42, Type.Int32, loc),
            Var(LocalVar(4, "y"), Type.Int32, loc),
            Type.Int32, loc),
          Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32, Type.Int32, Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate,
      List(Integer.TYPE, Integer.TYPE, Integer.TYPE),
      List(-1337, 101010, -42).map(_.asInstanceOf[Object]))

    assertResult(-101010)(result)
  }

  test("Codegen - Let05") {
    val definition = Function(name, args = List("a", "b", "c"),
      body = Let(LocalVar(3, "x"), Const(1337, Type.Int32, loc),
        Let(LocalVar(4, "y"), Const(-101010, Type.Int32, loc),
          Let(LocalVar(5, "z"), Const(42, Type.Int32, loc),
            Var(LocalVar(0, "a"), Type.Int32, loc),
            Type.Int32, loc),
          Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32, Type.Int32, Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate,
      List(Integer.TYPE, Integer.TYPE, Integer.TYPE),
      List(-1337, 101010, -42).map(_.asInstanceOf[Object]))

    assertResult(-1337)(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Function application                                                    //
  /////////////////////////////////////////////////////////////////////////////

  test("Codegen - Apply01") {
    // def main(): Int = f()
    // def f(): Int = 24
    val main = Function(name, args = List(),
      body = Apply(name01, List(), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val f = Function(name01, args = List(),
      body = Const(24, Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(main, f))
    val result = code.call(name.decorate)

    assertResult(24)(result)
  }

  test("Codegen - Apply02") {
    // def main(): Int = f(3)
    // def f(x: Int): Int = 24
    val main = Function(name, args = List(),
      body = Apply(name01, List(Const(3, Type.Int32, loc)), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val f = Function(name01, args = List("x"),
      body = Const(24, Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(main, f))
    val result = code.call(name.decorate)

    assertResult(24)(result)
  }

  test("Codegen - Apply03") {
    // def main(): Int = f(3)
    // def f(x: Int): Int = x
    val main = Function(name, args = List(),
      body = Apply(name01, List(Const(3, Type.Int32, loc)), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val f = Function(name01, args = List("x"),
      body = Var(LocalVar(0, "x"), Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(main, f))
    val result = code.call(name.decorate)

    assertResult(3)(result)
  }

  test("Codegen - Apply04") {
    // def main(): Int = f(3, 42)
    // def f(x: Int, y: Int): Int = x * y - 6
    val main = Function(name, args = List(),
      body = Apply(name01, List(Const(3, Type.Int32, loc), Const(42, Type.Int32, loc)), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val f = Function(name01, args = List("x", "y"),
      body = Binary(BinaryOperator.Minus,
        Binary(BinaryOperator.Times,
          Var(LocalVar(0, "x"), Type.Int32, loc),
          Var(LocalVar(1, "y"), Type.Int32, loc),
          Type.Int32, loc),
        Const(6, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32, Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(main, f))
    val result = code.call(name.decorate)

    assertResult(120)(result)
  }

  test("Codegen - Apply05") {
    // def main(): Int = f(5)
    // def f(x: Int): Int = let y = g(x + 1) in y * y
    // def g(x: Int): Int = x - 4
    val main = Function(name, args = List(),
      body = Apply(name01, List(Const(5, Type.Int32, loc)), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val f = Function(name01, args = List("x"),
      body = Let(LocalVar(1, "y"), Apply(name02,
        List(Binary(BinaryOperator.Plus,
          Var(LocalVar(0, "x"), Type.Int32, loc),
          Const(1, Type.Int32, loc), Type.Int32, loc)), Type.Int32, loc),
        Binary(BinaryOperator.Times,
          Var(LocalVar(1, "y"), Type.Int32, loc),
          Var(LocalVar(1, "y"), Type.Int32, loc),
          Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)
    val g = Function(name02, args = List("x"),
      body =Binary(BinaryOperator.Minus,
        Var(LocalVar(0, "x"), Type.Int32, loc),
        Const(4, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(main, f, g))
    val result = code.call(name.decorate)

    assertResult(4)(result)
  }

  test("Codegen - Apply06") {
    // def main(): Int = f(3)
    // def f(x: Int): Int = g(x + 1)
    // def g(x: Int): Int = h(x + 10)
    // def h(x: Int): Int = x * x
    val main = Function(name, args = List(),
      body = Apply(name01, List(Const(3, Type.Int32, loc)), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val f = Function(name01, args = List("x"),
      body = Apply(name02, List(
        Binary(BinaryOperator.Plus,
          Var(LocalVar(0, "x"), Type.Int32, loc),
          Const(1, Type.Int32, loc),
          Type.Int32, loc)),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)
    val g = Function(name02, args = List("x"),
      body = Apply(name03, List(
        Binary(BinaryOperator.Plus,
          Var(LocalVar(0, "x"), Type.Int32, loc),
          Const(10, Type.Int32, loc),
          Type.Int32, loc)),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)
    val h = Function(name03, args = List("x"),
      body = Binary(BinaryOperator.Times,
        Var(LocalVar(0, "x"), Type.Int32, loc),
        Var(LocalVar(0, "x"), Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(main, f, g, h))
    val result = code.call(name.decorate)

    assertResult(196)(result)
  }

  test("Codegen - Apply07") {
    // def main(): Int = let x = 7 in f(g(3), h(h(x)))
    // def f(x: Int, y: Int): Int = x - y
    // def g(x: Int): Int = x * 3
    // def h(x: Int): Int = g(x - 1)
    val main = Function(name, args = List(),
      body = Let(LocalVar(0, "x"), Const(7, Type.Int32, loc),
        Apply(name01, List(
          Apply(name02, List(Const(3, Type.Int32, loc)), Type.Int32, loc),
          Apply(name03, List(
            Apply(name03, List(Var(LocalVar(0, "x"), Type.Int32, loc)),
              Type.Int32, loc)), Type.Int32, loc)),
          Type.Int32, loc), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)
    val f = Function(name01, args = List("x", "y"),
      body = Binary(BinaryOperator.Minus,
        Var(LocalVar(0, "x"), Type.Int32, loc),
        Var(LocalVar(1, "y"), Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32, Type.Int32), Type.Int32), loc)
    val g = Function(name02, args = List("x"),
      body = Binary(BinaryOperator.Times,
          Var(LocalVar(0, "x"), Type.Int32, loc),
          Const(3, Type.Int32, loc),
          Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)
    val h = Function(name03, args = List("x"),
      body = Apply(name02, List(
        Binary(BinaryOperator.Minus,
          Var(LocalVar(0, "x"), Type.Int32, loc),
          Const(1, Type.Int32, loc),
          Type.Int32, loc)),
        Type.Int32, loc),
      Type.Lambda(List(Type.Int32), Type.Int32), loc)

    val code = new CompiledCode(List(main, f, g, h))
    val result = code.call(name.decorate)

    assertResult(-42)(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Unary operators                                                         //
  /////////////////////////////////////////////////////////////////////////////

  test("Codegen - Unary.Not01") {
    val definition = Function(name, args = List(),
      body = Unary(UnaryOperator.Not, Const(0, Type.Bool, loc), Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Unary.Not02") {
    val definition = Function(name, args = List(),
      body = Unary(UnaryOperator.Not, Const(1, Type.Bool, loc), Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Unary.Plus01") {
    val definition = Function(name, args = List(),
      body = Unary(UnaryOperator.Plus, Const(42, Type.Int32, loc), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42)(result)
  }

  test("Codegen - Unary.Plus02") {
    val definition = Function(name, args = List(),
      body = Unary(UnaryOperator.Plus, Const(-42, Type.Int32, loc), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-42)(result)
  }

  test("Codegen - Unary.Minus01") {
    val definition = Function(name, args = List(),
      body = Unary(UnaryOperator.Minus, Const(42, Type.Int32, loc), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-42)(result)
  }

  test("Codegen - Unary.Minus02") {
    val definition = Function(name, args = List(),
      body = Unary(UnaryOperator.Minus, Const(-42, Type.Int32, loc), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42)(result)
  }

  test("Codegen - Unary.Negate01") {
    val definition = Function(name, args = List(),
      body = Unary(UnaryOperator.Negate, Const(42, Type.Int32, loc), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(~42)(result)
  }

  test("Codegen - Unary.Negate02") {
    val definition = Function(name, args = List(),
      body = Unary(UnaryOperator.Negate, Const(-42, Type.Int32, loc), Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(~(-42))(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Binary operators                                                        //
  /////////////////////////////////////////////////////////////////////////////

  test("Codegen - Binary.And01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.And,
        Const(1, Type.Bool, loc),
        Const(1, Type.Bool, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.And02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.And,
        Const(1, Type.Bool, loc),
        Const(0, Type.Bool, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.And03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.And,
        Const(0, Type.Bool, loc),
        Const(0, Type.Bool, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.And04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.And,
        Const(0, Type.Bool, loc),
        Const(1, Type.Bool, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Or01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Or,
        Const(1, Type.Bool, loc),
        Const(1, Type.Bool, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Or02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Or,
        Const(1, Type.Bool, loc),
        Const(0, Type.Bool, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Or03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Or,
        Const(0, Type.Bool, loc),
        Const(0, Type.Bool, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Or04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Or,
        Const(0, Type.Bool, loc),
        Const(1, Type.Bool, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Plus01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Plus,
        Const(400, Type.Int32, loc),
        Const(100, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(500)(result)
  }

  test("Codegen - Binary.Plus02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Plus,
        Const(100, Type.Int32, loc),
        Const(400, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(500)(result)
  }

  test("Codegen - Binary.Plus03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Plus,
        Const(-400, Type.Int32, loc),
        Const(100, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-300)(result)
  }

  test("Codegen - Binary.Plus04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Plus,
        Const(-100, Type.Int32, loc),
        Const(400, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(300)(result)
  }

  test("Codegen - Binary.Plus05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Plus,
        Const(-400, Type.Int32, loc),
        Const(-100, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-500)(result)
  }

  test("Codegen - Binary.Minus01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Minus,
        Const(400, Type.Int32, loc),
        Const(100, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(300)(result)
  }

  test("Codegen - Binary.Minus02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Minus,
        Const(100, Type.Int32, loc),
        Const(400, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-300)(result)
  }

  test("Codegen - Binary.Minus03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Minus,
        Const(-400, Type.Int32, loc),
        Const(100, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-500)(result)
  }

  test("Codegen - Binary.Minus04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Minus,
        Const(-100, Type.Int32, loc),
        Const(400, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-500)(result)
  }

  test("Codegen - Binary.Minus05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Minus,
        Const(-400, Type.Int32, loc),
        Const(-100, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-300)(result)
  }

  test("Codegen - Binary.Times01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Times,
        Const(2, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(6)(result)
  }

  test("Codegen - Binary.Times02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Times,
        Const(3, Type.Int32, loc),
        Const(2, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(6)(result)
  }

  test("Codegen - Binary.Times03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Times,
        Const(-2, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-6)(result)
  }

  test("Codegen - Binary.Times04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Times,
        Const(-3, Type.Int32, loc),
        Const(2, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-6)(result)
  }

  test("Codegen - Binary.Times05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Times,
        Const(-2, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(6)(result)
  }

  test("Codegen - Binary.Divide01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Divide,
        Const(12, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(4)(result)
  }

  test("Codegen - Binary.Divide02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Divide,
        Const(3, Type.Int32, loc),
        Const(12, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(0)(result)
  }

  test("Codegen - Binary.Divide03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Divide,
        Const(-12, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-4)(result)
  }

  test("Codegen - Binary.Divide04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Divide,
        Const(-3, Type.Int32, loc),
        Const(12, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(0)(result)
  }

  test("Codegen - Binary.Divide05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Divide,
        Const(-12, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(4)(result)
  }

  test("Codegen - Binary.Modulo01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Modulo,
        Const(12, Type.Int32, loc),
        Const(2, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(0)(result)
  }

  test("Codegen - Binary.Modulo02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Modulo,
        Const(12, Type.Int32, loc),
        Const(5, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(2)(result)
  }

  test("Codegen - Binary.Modulo03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Modulo,
        Const(-12, Type.Int32, loc),
        Const(5, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-2)(result)
  }

  test("Codegen - Binary.Modulo04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Modulo,
        Const(12, Type.Int32, loc),
        Const(-5, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(2)(result)
  }

  test("Codegen - Binary.Modulo05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Modulo,
        Const(-12, Type.Int32, loc),
        Const(-5, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(-2)(result)
  }

  test("Codegen - Binary.BitwiseAnd01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseAnd,
        Const(42, Type.Int32, loc),
        Const(0xFFFF, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42 & 0xFFFF)(result)
  }

  test("Codegen - Binary.BitwiseAnd02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseAnd,
        Const(42, Type.Int32, loc),
        Const(42, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42 & 42)(result)
  }

  test("Codegen - Binary.BitwiseAnd03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseAnd,
        Const(42, Type.Int32, loc),
        Const(0, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42 & 0)(result)
  }

  test("Codegen - Binary.BitwiseOr01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseOr,
        Const(42, Type.Int32, loc),
        Const(0xFFFF, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42 | 0xFFFF)(result)
  }

  test("Codegen - Binary.BitwiseOr02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseOr,
        Const(42, Type.Int32, loc),
        Const(42, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42 | 42)(result)
  }

  test("Codegen - Binary.BitwiseOr03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseOr,
        Const(42, Type.Int32, loc),
        Const(0, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42 | 0)(result)
  }

  test("Codegen - Binary.BitwiseXor01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseXor,
        Const(42, Type.Int32, loc),
        Const(0xFFFF, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42 ^ 0xFFFF)(result)
  }

  test("Codegen - Binary.BitwiseXor02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseXor,
        Const(42, Type.Int32, loc),
        Const(42, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42 ^ 42)(result)
  }

  test("Codegen - Binary.BitwiseXor03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseXor,
        Const(42, Type.Int32, loc),
        Const(0, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(42 ^ 0)(result)
  }

  test("Codegen - Binary.BitwiseLeftShift01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseLeftShift,
        Const(4, Type.Int32, loc),
        Const(0, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(4 << 0)(result)
  }

  test("Codegen - Binary.BitwiseLeftShift02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseLeftShift,
        Const(4, Type.Int32, loc),
        Const(14, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(4 << 14)(result)
  }

  test("Codegen - Binary.BitwiseLeftShift03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseLeftShift,
        Const(4, Type.Int32, loc),
        Const(29, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(4 << 29)(result)
  }

  test("Codegen - Binary.BitwiseLeftShift04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseLeftShift,
        Const(4, Type.Int32, loc),
        Const(30, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(4 << 30)(result)
  }

  test("Codegen - Binary.BitwiseRightShift01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseRightShift,
        Const(12345, Type.Int32, loc),
        Const(20, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(12345 >> 20)(result)
  }

  test("Codegen - Binary.BitwiseRightShift02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseRightShift,
        Const(12345, Type.Int32, loc),
        Const(10, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(12345 >> 10)(result)
  }

  test("Codegen - Binary.BitwiseRightShift03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.BitwiseRightShift,
        Const(12345, Type.Int32, loc),
        Const(0, Type.Int32, loc),
        Type.Int32, loc),
      Type.Lambda(List(), Type.Int32), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(12345 >> 0)(result)
  }

  test("Codegen - Binary.Less01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Less,
        Const(12, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Less02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Less,
        Const(3, Type.Int32, loc),
        Const(12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Less03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Less,
        Const(3, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Less04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Less,
        Const(-12, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Less05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Less,
        Const(-3, Type.Int32, loc),
        Const(-12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Less06") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Less,
        Const(-3, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.LessEqual01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.LessEqual,
        Const(12, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.LessEqual02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.LessEqual,
        Const(3, Type.Int32, loc),
        Const(12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.LessEqual03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.LessEqual,
        Const(3, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.LessEqual04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.LessEqual,
        Const(-12, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.LessEqual05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.LessEqual,
        Const(-3, Type.Int32, loc),
        Const(-12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.LessEqual06") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.LessEqual,
        Const(-3, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Greater01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Greater,
        Const(12, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Greater02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Greater,
        Const(3, Type.Int32, loc),
        Const(12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Greater03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Greater,
        Const(3, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Greater04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Greater,
        Const(-12, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Greater05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Greater,
        Const(-3, Type.Int32, loc),
        Const(-12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Greater06") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Greater,
        Const(-3, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.GreaterEqual01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.GreaterEqual,
        Const(12, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.GreaterEqual02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.GreaterEqual,
        Const(3, Type.Int32, loc),
        Const(12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.GreaterEqual03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.GreaterEqual,
        Const(3, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.GreaterEqual04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.GreaterEqual,
        Const(-12, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.GreaterEqual05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.GreaterEqual,
        Const(-3, Type.Int32, loc),
        Const(-12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.GreaterEqual06") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.GreaterEqual,
        Const(-3, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Equal01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Equal,
        Const(12, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Equal02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Equal,
        Const(3, Type.Int32, loc),
        Const(12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Equal03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Equal,
        Const(3, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.Equal04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Equal,
        Const(-12, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Equal05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Equal,
        Const(-3, Type.Int32, loc),
        Const(-12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.Equal06") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.Equal,
        Const(-3, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.NotEqual01") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.NotEqual,
        Const(12, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.NotEqual02") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.NotEqual,
        Const(3, Type.Int32, loc),
        Const(12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.NotEqual03") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.NotEqual,
        Const(3, Type.Int32, loc),
        Const(3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }

  test("Codegen - Binary.NotEqual04") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.NotEqual,
        Const(-12, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.NotEqual05") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.NotEqual,
        Const(-3, Type.Int32, loc),
        Const(-12, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(true)(result)
  }

  test("Codegen - Binary.NotEqual06") {
    val definition = Function(name, args = List(),
      body = Binary(BinaryOperator.NotEqual,
        Const(-3, Type.Int32, loc),
        Const(-3, Type.Int32, loc),
        Type.Bool, loc),
      Type.Lambda(List(), Type.Bool), loc)

    val code = new CompiledCode(List(definition))
    val result = code.call(name.decorate)

    assertResult(false)(result)
  }
}