package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Convert a ClassId to its JVM binary class name.
 * For nested classes, this uses '$' as the separator instead of '.'.
 * 
 * Examples:
 * - ClassId("com.example", "MyClass") -> "com.example.MyClass"
 * - ClassId("com.example", "Outer.Inner") -> "com.example.Outer$Inner"
 */
internal val ClassId.jvmBinaryName: String
  get() {
    val packageFqName = packageFqName.asString()
    val relativeClassName = relativeClassName.asString().replace('.', '$')
    return if (packageFqName.isEmpty()) {
      relativeClassName
    } else {
      "$packageFqName.$relativeClassName"
    }
  }

/**
 * Get the JVM binary name for an IrClass.
 * Returns null if the class doesn't have a ClassId.
 */
internal val IrClass.jvmBinaryName: String?
  get() = classId?.jvmBinaryName

/**
 * Convert JVM binary name back to ClassId.
 * 
 * Examples:
 * - "com.example.MyClass" -> ClassId("com.example", "MyClass")
 * - "com.example.Outer$Inner" -> ClassId("com.example", "Outer.Inner")
 * - "MyClass" -> ClassId("", "MyClass")
 */
internal fun parseJvmBinaryNameToClassId(jvmBinaryName: String): ClassId {
  // Find the start of the class name by looking for the first uppercase letter
  // or the first segment containing '$' (nested class marker)
  val segments = jvmBinaryName.split('.')
  val firstClassSegmentIndex = segments.indexOfFirst { segment ->
    segment.contains('$') || segment.firstOrNull()?.isUpperCase() == true
  }.takeIf { it >= 0 } ?: 0
  
  val packageName = segments.take(firstClassSegmentIndex).joinToString(".")
  val className = segments.drop(firstClassSegmentIndex).joinToString(".").replace('$', '.')
  
  return ClassId(FqName(packageName), FqName(className), isLocal = false)
}
