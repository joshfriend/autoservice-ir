package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.Test
import kotlin.test.assertEquals

class JvmBinaryNameTest {

  @Test
  fun `top-level class in package`() {
    val classId = ClassId(FqName("com.example"), FqName("MyClass"), false)
    assertEquals("com.example.MyClass", classId.jvmBinaryName)
  }

  @Test
  fun `top-level class in default package`() {
    val classId = ClassId(FqName(""), FqName("MyClass"), false)
    assertEquals("MyClass", classId.jvmBinaryName)
  }

  @Test
  fun `nested class uses dollar sign`() {
    val classId = ClassId(FqName("com.example"), FqName("Outer.Inner"), false)
    assertEquals("com.example.Outer\$Inner", classId.jvmBinaryName)
  }

  @Test
  fun `deeply nested class uses multiple dollar signs`() {
    val classId = ClassId(FqName("com.example"), FqName("Outer.Middle.Inner"), false)
    assertEquals("com.example.Outer\$Middle\$Inner", classId.jvmBinaryName)
  }

  @Test
  fun `nested class in default package`() {
    val classId = ClassId(FqName(""), FqName("Outer.Inner"), false)
    assertEquals("Outer\$Inner", classId.jvmBinaryName)
  }

  @Test
  fun `class in deeply nested package`() {
    val classId = ClassId(FqName("com.example.feature.impl"), FqName("MyClass"), false)
    assertEquals("com.example.feature.impl.MyClass", classId.jvmBinaryName)
  }

  @Test
  fun `nested class in deeply nested package`() {
    val classId = ClassId(FqName("com.example.feature.impl"), FqName("Outer.Inner"), false)
    assertEquals("com.example.feature.impl.Outer\$Inner", classId.jvmBinaryName)
  }
}
