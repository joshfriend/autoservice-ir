package com.fueledbycaffeine.autoservice.ir

import com.fueledbycaffeine.autoservice.AutoServiceSymbols
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.nonDispatchArguments
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class AutoServiceIrVisitor(
  private val diagnosticReporter: IrDiagnosticReporter,
  private val serviceRegistry: ServiceRegistry,
  private val debugLogger: AutoServiceDebugLogger,
) : IrVisitorVoid() {

  override fun visitElement(element: IrElement) {
    element.acceptChildrenVoid(this)
  }

  private fun IrClass.locationString(): String {
    val irFile = fileOrNull ?: return fqNameWhenAvailable?.asString() ?: "<unknown>"
    val file = irFile.path
    
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
    val className = declaration.jvmBinaryName
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

    debugLogger.log("${declaration.locationString()} Processing @$annotation on class: ${declaration.fqNameWhenAvailable}")

    // Extract service interfaces from the annotation
    // FIR has already validated everything, so this extraction is safe
    val serviceInterfaces = extractServiceInterfacesFromAnnotation(annotation, declaration)
    
    if (serviceInterfaces.isEmpty()) {
      // This should never happen if FIR validation worked correctly
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.ERROR,
        "${declaration.locationString()} @AutoService annotation found but no service interfaces specified. " +
          "This indicates a compiler plugin issue."
      )
      return
    }

    val providerClass = declaration.jvmBinaryName ?: run {
      diagnosticReporter.report(
        AutoServiceIrDiagnostics.ERROR,
        "${declaration.locationString()} Could not determine JVM binary class name for @AutoService target '${declaration.fqNameWhenAvailable}'. " +
          "This may indicate a compiler issue with local or anonymous classes."
      )
      return
    }

    // Register each service interface with the provider class
    // No validation needed - FIR has already validated everything
    for (serviceInterface in serviceInterfaces) {
      val serviceInterfaceJvmName = serviceInterface.jvmBinaryName
      debugLogger.log("${declaration.locationString()} Registering service: $serviceInterfaceJvmName -> $providerClass")
      serviceRegistry.register(serviceInterfaceJvmName, providerClass)
    }
  }

  /**
   * Extracts service interfaces from the @AutoService annotation's `value` parameter.
   * 
   * If the annotation has no explicit value (inferred type), infers service interfaces
   * from the class's supertypes.
   * 
   * FIR has already validated the annotation, so this extraction is safe.
   */
  private fun extractServiceInterfacesFromAnnotation(
    annotation: IrConstructorCall,
    declaration: IrClass
  ): List<ClassId> {
    // Try to get explicit service interfaces from annotation value parameter
    val valueArgs = annotation.nonDispatchArguments.firstOrNull() as? IrVararg
    
    if (valueArgs != null && valueArgs.elements.isNotEmpty()) {
      // Explicit service interfaces provided
      return valueArgs.elements.mapNotNull { element ->
        when (element) {
          is IrClassReference -> element.classType.classOrNull?.owner?.classId
          else -> null
        }
      }
    }
    
    // No explicit service interfaces - infer from implemented interfaces
    // FIR has already validated that there's exactly one interface to infer from
    return declaration.superTypes.mapNotNull { superType ->
      // Skip Any as possible inferred service class
      when (val classId = superType.classOrNull?.owner?.classId) {
        StandardClassIds.Any -> null
        else -> classId
      }
    }
  }
}
