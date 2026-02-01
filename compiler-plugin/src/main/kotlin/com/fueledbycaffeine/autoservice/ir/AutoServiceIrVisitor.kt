package com.fueledbycaffeine.autoservice.ir

import com.fueledbycaffeine.autoservice.AutoServiceSymbols
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.nonDispatchArguments
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Convert a ClassId to its JVM binary class name.
 * For nested classes, this uses '$' as the separator instead of '.'.
 * e.g., ClassId for "com.example.Outer.Inner" becomes "com.example.Outer$Inner"
 */
internal fun ClassId.toJvmBinaryName(): String {
  val packageFqName = packageFqName.asString()
  val relativeClassName = relativeClassName.asString().replace('.', '$')
  return if (packageFqName.isEmpty()) {
    relativeClassName
  } else {
    "$packageFqName.$relativeClassName"
  }
}

/**
 * Convert an IrClass to its JVM binary class name.
 * For nested classes, this uses '$' as the separator instead of '.'.
 * e.g., "com.example.Outer.Inner" becomes "com.example.Outer$Inner"
 */
private fun IrClass.jvmBinaryName(): String? {
  return classId?.toJvmBinaryName()
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class AutoServiceIrVisitor(
  private val pluginContext: IrPluginContext,
  private val diagnosticReporter: IrDiagnosticReporter,
  private val serviceRegistry: ServiceRegistry,
  private val debugLogger: AutoServiceDebugLogger?,
  private val projectRoot: String?,
) : IrVisitorVoid() {

  override fun visitElement(element: IrElement) {
    element.acceptChildrenVoid(this)
  }

  private fun IrClass.locationString(): String {
    // Find the containing IrFile by traversing up the parent chain
    var current: IrElement? = this
    var irFile: IrFile? = null
    while (current != null) {
      if (current is IrFile) {
        irFile = current
        break
      }
      current = (current as? IrDeclaration)?.parent
    }
    
    // Use absolute path for IDE clickable links
    val file = irFile?.path ?: return fqNameWhenAvailable?.asString() ?: "<unknown>"
    
    // Get line and column numbers for proper IDE integration
    // Format: /absolute/path/file.kt:line:column (IDE standard format)
    return if (startOffset >= 0) {
      val sourceRangeInfo = irFile.fileEntry.getSourceRangeInfo(startOffset, endOffset)
      val line = sourceRangeInfo.startLineNumber + 1  // +1 because lines are 0-indexed
      val column = sourceRangeInfo.startColumnNumber + 1  // +1 because columns are 0-indexed
      "$file:$line:$column"
    } else {
      file
    }
  }

  override fun visitClass(declaration: IrClass) {
    super.visitClass(declaration)

    // Track all classes in the module for detecting deleted files
    val className = declaration.jvmBinaryName()
    if (className != null) {
      serviceRegistry.trackModuleClass(className)
      serviceRegistry.trackCompiledClass(className)
    }

    // Check for either our annotation or Google's
    val annotation = declaration.getAnnotation(AutoServiceSymbols.FqNames.AUTOSERVICE)
      ?: declaration.getAnnotation(AutoServiceSymbols.FqNames.GOOGLE_AUTOSERVICE)
      
    if (annotation == null) {
      return
    }

    debugLogger?.let {
      val annotationName = if (declaration.hasAnnotation(AutoServiceSymbols.FqNames.AUTOSERVICE)) {
        AutoServiceSymbols.FqNames.AUTOSERVICE.asString()
      } else {
        AutoServiceSymbols.FqNames.GOOGLE_AUTOSERVICE.asString()
      }
      it.log("${declaration.locationString()} Processing @$annotationName on class: ${declaration.fqNameWhenAvailable}")
    }

    val serviceInterfaces = extractServiceInterfaces(annotation, declaration)
    if (serviceInterfaces.isEmpty()) {
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.ERROR,
        "${declaration.locationString()} @AutoService requires a service interface. " +
          "Either specify it explicitly (e.g., @AutoService(MyInterface::class)) or ensure the class has exactly one supertype."
      )
      return
    }

    val providerClass = declaration.jvmBinaryName() ?: run {
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.ERROR,
        "${declaration.locationString()} Could not determine JVM binary class name for @AutoService target '${declaration.fqNameWhenAvailable}'. " +
          "This may indicate a compiler issue with local or anonymous classes."
      )
      return
    }

    for (serviceInterface in serviceInterfaces) {
      if (!checkImplements(declaration, serviceInterface)) {
        diagnosticReporter.report(
          AutoServiceIrDiagnostics.ERROR,
          "${declaration.locationString()} @AutoService class '${declaration.fqNameWhenAvailable}' does not implement $serviceInterface"
        )
        continue
      }

      if (declaration.modality == Modality.ABSTRACT) {
        diagnosticReporter.report(
          AutoServiceIrDiagnostics.ERROR,
          "${declaration.locationString()} @AutoService cannot be applied to abstract class '${declaration.fqNameWhenAvailable}'"
        )
        continue
      }

      debugLogger?.log("${declaration.locationString()} Registering service: $serviceInterface -> $providerClass")

      serviceRegistry.register(serviceInterface, providerClass)
    }
  }

  private fun extractServiceInterfaces(annotation: IrConstructorCall, declaration: IrClass): List<String> {
      // Extract explicitly specified service interfaces
    val explicitInterfaces = when (val valueArgument = annotation.nonDispatchArguments.firstOrNull()) {
      is IrVararg -> {
        valueArgument.elements.mapNotNull { element ->
          (element as? IrClassReference)?.classType?.classOrNull?.owner?.jvmBinaryName()
        }
      }
      is IrClassReference -> {
        valueArgument.classType.classOrNull?.owner?.jvmBinaryName()?.let { listOf(it) } ?: emptyList()
      }
      else -> emptyList()
    }
    
    // If no explicit interfaces provided, try to infer from supertypes
    if (explicitInterfaces.isEmpty()) {
      val inferredInterfaces = inferServiceInterfaces(declaration)
      if (inferredInterfaces.isNotEmpty()) {
        debugLogger?.log("${declaration.locationString()} Inferred service interface(s) ${inferredInterfaces.joinToString()} for ${declaration.fqNameWhenAvailable}")
        return inferredInterfaces
      }
    }
    
    return explicitInterfaces
  }
  
  private fun inferServiceInterfaces(declaration: IrClass): List<String> {
    // Get all non-Any supertypes (interfaces and classes)
    val supertypes = declaration.superTypes.mapNotNull { superType ->
      val superClass = superType.classOrNull?.owner
      // Exclude kotlin.Any
      if (superClass != null && superClass.fqNameWhenAvailable?.asString() != "kotlin.Any") {
        superClass.jvmBinaryName()
      } else {
        null
      }
    }
    
    // If there's exactly one supertype, use it as the service interface
    return when {
      supertypes.size == 1 -> supertypes
      supertypes.isEmpty() -> {
        debugLogger?.log("${declaration.locationString()} Class ${declaration.fqNameWhenAvailable} has no supertypes to infer service interface from")
        emptyList()
      }
      else -> {
        debugLogger?.log("${declaration.locationString()} Class ${declaration.fqNameWhenAvailable} has multiple supertypes (${supertypes.joinToString()}), cannot infer service interface")
        emptyList()
      }
    }
  }

  private fun checkImplements(declaration: IrClass, serviceInterface: String): Boolean {
    // Convert JVM binary name (with $) back to ClassId
    // e.g., "com.example.Outer$Inner" -> ClassId(FqName("com.example"), FqName("Outer.Inner"))
    val lastDot = serviceInterface.lastIndexOf('.')
    val (packageName, relativeClassName) = if (lastDot >= 0) {
      // Find where the package ends and the class name begins
      // We need to find the first segment that starts with uppercase (class name)
      val segments = serviceInterface.split('.')
      var packageEndIndex = 0
      for ((index, segment) in segments.withIndex()) {
        // Check if this segment contains $ (nested class) or starts with uppercase (class name)
        if (segment.contains('$') || segment.firstOrNull()?.isUpperCase() == true) {
          packageEndIndex = index
          break
        }
      }
      val packagePart = segments.take(packageEndIndex).joinToString(".")
      val classPart = segments.drop(packageEndIndex).joinToString(".").replace('$', '.')
      packagePart to classPart
    } else {
      "" to serviceInterface.replace('$', '.')
    }
    
    val classId = ClassId(FqName(packageName), FqName(relativeClassName), false)
    val serviceClass = pluginContext.referenceClass(classId)?.owner ?: return false

    return declaration.isSubclassOf(serviceClass)
  }
}
