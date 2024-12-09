package replpp.shaded
package os
trait GeneratedTupleConversions[R]{
  protected def flatten(vs: R*): R
    implicit def tuple2Conversion[T1, T2]
    (t: (T1, T2))
    (implicit f1: T1 => R, f2: T2 => R): R = {
      this.flatten(f1(t._1), f2(t._2))
  }

  implicit def tuple3Conversion[T1, T2, T3]
    (t: (T1, T2, T3))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3))
  }

  implicit def tuple4Conversion[T1, T2, T3, T4]
    (t: (T1, T2, T3, T4))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4))
  }

  implicit def tuple5Conversion[T1, T2, T3, T4, T5]
    (t: (T1, T2, T3, T4, T5))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5))
  }

  implicit def tuple6Conversion[T1, T2, T3, T4, T5, T6]
    (t: (T1, T2, T3, T4, T5, T6))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6))
  }

  implicit def tuple7Conversion[T1, T2, T3, T4, T5, T6, T7]
    (t: (T1, T2, T3, T4, T5, T6, T7))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7))
  }

  implicit def tuple8Conversion[T1, T2, T3, T4, T5, T6, T7, T8]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8))
  }

  implicit def tuple9Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9))
  }

  implicit def tuple10Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10))
  }

  implicit def tuple11Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11))
  }

  implicit def tuple12Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12))
  }

  implicit def tuple13Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13))
  }

  implicit def tuple14Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R, f14: T14 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13), f14(t._14))
  }

  implicit def tuple15Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R, f14: T14 => R, f15: T15 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13), f14(t._14), f15(t._15))
  }

  implicit def tuple16Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R, f14: T14 => R, f15: T15 => R, f16: T16 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13), f14(t._14), f15(t._15), f16(t._16))
  }

  implicit def tuple17Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R, f14: T14 => R, f15: T15 => R, f16: T16 => R, f17: T17 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13), f14(t._14), f15(t._15), f16(t._16), f17(t._17))
  }

  implicit def tuple18Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R, f14: T14 => R, f15: T15 => R, f16: T16 => R, f17: T17 => R, f18: T18 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13), f14(t._14), f15(t._15), f16(t._16), f17(t._17), f18(t._18))
  }

  implicit def tuple19Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R, f14: T14 => R, f15: T15 => R, f16: T16 => R, f17: T17 => R, f18: T18 => R, f19: T19 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13), f14(t._14), f15(t._15), f16(t._16), f17(t._17), f18(t._18), f19(t._19))
  }

  implicit def tuple20Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R, f14: T14 => R, f15: T15 => R, f16: T16 => R, f17: T17 => R, f18: T18 => R, f19: T19 => R, f20: T20 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13), f14(t._14), f15(t._15), f16(t._16), f17(t._17), f18(t._18), f19(t._19), f20(t._20))
  }

  implicit def tuple21Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R, f14: T14 => R, f15: T15 => R, f16: T16 => R, f17: T17 => R, f18: T18 => R, f19: T19 => R, f20: T20 => R, f21: T21 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13), f14(t._14), f15(t._15), f16(t._16), f17(t._17), f18(t._18), f19(t._19), f20(t._20), f21(t._21))
  }

  implicit def tuple22Conversion[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]
    (t: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22))
    (implicit f1: T1 => R, f2: T2 => R, f3: T3 => R, f4: T4 => R, f5: T5 => R, f6: T6 => R, f7: T7 => R, f8: T8 => R, f9: T9 => R, f10: T10 => R, f11: T11 => R, f12: T12 => R, f13: T13 => R, f14: T14 => R, f15: T15 => R, f16: T16 => R, f17: T17 => R, f18: T18 => R, f19: T19 => R, f20: T20 => R, f21: T21 => R, f22: T22 => R): R = {
      this.flatten(f1(t._1), f2(t._2), f3(t._3), f4(t._4), f5(t._5), f6(t._6), f7(t._7), f8(t._8), f9(t._9), f10(t._10), f11(t._11), f12(t._12), f13(t._13), f14(t._14), f15(t._15), f16(t._16), f17(t._17), f18(t._18), f19(t._19), f20(t._20), f21(t._21), f22(t._22))
  }

}

