package com.fueledbycaffeine.autoservice.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
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

private val GOOGLE_AUTO_SERVICE_ANNOTATION = FqName("com.google.auto.service.AutoService")
private val AUTOSERVICE_ANNOTATION = FqName("com.fueledbycaffeine.autoservice.AutoService")
private val SUPPRESS_WARNINGS_ANNOTATION = FqName("kotlin.Suppress")

/**
 * Convert an IrClass to its JVM binary class name.
 * For nested classes, this uses '$' as the separator instead of '.'.
 * e.g., "com.example.Outer.Inner" becomes "com.example.Outer$Inner"
 */
private fun IrClass.jvmBinaryName(): String? {
  val classId = this.classId ?: return null
  val packageFqName = classId.packageFqName.asString()
  val relativeClassName = classId.relativeClassName.asString().replace('.', '$')
  return if (packageFqName.isEmpty()) {
    relativeClassName
  } else {
    "$packageFqName.$relativeClassName"
  }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class AutoServiceIrVisitor(
  private val pluginContext: IrPluginContext,
  private val diagnosticReporter: IrDiagnosticReporter,
  private val serviceRegistry: ServiceRegistry,
  private val debug: Boolean,
  private val projectRoot: String?,
) : IrVisitorVoid() {

  override fun visitElement(element: IrElement) {
    element.acceptChildrenVoid(this)
  }

  private fun IrClass.locationString(): String {
    val file = (parent as? IrFile)?.path ?: return fqNameWhenAvailable?.asString() ?: "<unknown>"
    val displayPath = if (projectRoot != null && file.startsWith(projectRoot)) {
      file.removePrefix(projectRoot).removePrefix("/")
    } else {
      file
    }
    return "$displayPath:$startOffset"
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
    val annotation = declaration.getAnnotation(AUTOSERVICE_ANNOTATION)
      ?: declaration.getAnnotation(GOOGLE_AUTO_SERVICE_ANNOTATION)
      
    if (annotation == null) {
      return
    }

    if (debug) {
      val annotationName = if (declaration.hasAnnotation(AUTOSERVICE_ANNOTATION)) {
        "com.fueledbycaffeine.autoservice.AutoService"
      } else {
        "com.google.auto.service.AutoService"
      }
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.INFO,
        "${declaration.locationString()}: AutoService IR: Processing @$annotationName on class: ${declaration.fqNameWhenAvailable}"
      )
    }

    val serviceInterfaces = extractServiceInterfaces(annotation, declaration)
    if (serviceInterfaces.isEmpty()) {
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.ERROR,
        "${declaration.locationString()}: No service interfaces provided for element! ${declaration.fqNameWhenAvailable}"
      )
      return
    }

    val providerClass = declaration.jvmBinaryName() ?: run {
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.ERROR,
        "${declaration.locationString()}: Could not determine FQ name for ${declaration.name}"
      )
      return
    }

    for (serviceInterface in serviceInterfaces) {
      if (!hasSuppressWarning(declaration, "AutoService")) {
        if (!checkImplements(declaration, serviceInterface)) {
          diagnosticReporter.report(
            AutoServiceIrDiagnostics.ERROR,
            "${declaration.locationString()}: ServiceProviders must implement their service provider interface. " +
              "$providerClass does not implement $serviceInterface"
          )
          continue
        }

        if (declaration.modality == Modality.ABSTRACT) {
          diagnosticReporter.report(
            AutoServiceIrDiagnostics.ERROR,
            "${declaration.locationString()}: @AutoService can only be applied to a concrete class: $providerClass"
          )
          continue
        }
      }

      if (debug) {
        diagnosticReporter.report(
          AutoServiceIrDiagnostics.INFO,
          "${declaration.locationString()}: AutoService IR: Registering service: $serviceInterface -> $providerClass"
        )
      }

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
        if (debug) {
          diagnosticReporter.report(
            AutoServiceIrDiagnostics.INFO,
            "${declaration.locationString()}: AutoService IR: Inferred service interface(s) ${inferredInterfaces.joinToString()} for ${declaration.fqNameWhenAvailable}"
          )
        }
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
        if (debug) {
          diagnosticReporter.report(
            AutoServiceIrDiagnostics.INFO,
            "${declaration.locationString()}: AutoService IR: Class ${declaration.fqNameWhenAvailable} has no supertypes to infer service interface from"
          )
        }
        emptyList()
      }
      else -> {
        if (debug) {
          diagnosticReporter.report(
            AutoServiceIrDiagnostics.INFO,
            "${declaration.locationString()}: AutoService IR: Class ${declaration.fqNameWhenAvailable} has multiple supertypes (${supertypes.joinToString()}), cannot infer service interface"
          )
        }
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

  private fun hasSuppressWarning(declaration: IrClass, warning: String): Boolean {
    var current: IrClass? = declaration
    while (current != null) {
      val suppressAnnotation = current.getAnnotation(SUPPRESS_WARNINGS_ANNOTATION)
      if (suppressAnnotation != null) {
        val suppressedWarnings = extractSuppressedWarnings(suppressAnnotation)
        if (warning in suppressedWarnings) {
          return true
        }
      }
      current = current.parent as? IrClass
    }
    return false
  }

  private fun extractSuppressedWarnings(annotation: IrConstructorCall): List<String> {
    val valueArgument = annotation.nonDispatchArguments.firstOrNull() ?: return emptyList()
    
    return when (valueArgument) {
      is IrVararg -> {
        valueArgument.elements.mapNotNull { element ->
          (element as? IrConst)?.value as? String
        }
      }
      is IrConst -> {
        (valueArgument.value as? String)?.let { listOf(it) } ?: emptyList()
      }
      else -> emptyList()
    }
  }
}
