package com.fueledbycaffeine.autoservice.ir

import com.fueledbycaffeine.autoservice.fir.autoServiceMetadata
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

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

    // Get service interfaces from FIR metadata (computed during FIR phase)
    // This avoids re-parsing annotations and re-inferring types in IR
    val metadata = declaration.autoServiceMetadata ?: return
    val serviceInterfaces = metadata.serviceInterfaces
    
    if (serviceInterfaces.isEmpty()) return

    debugLogger.log("${declaration.locationString()} Processing @AutoService on class: ${declaration.fqNameWhenAvailable}")
    debugLogger.log("${declaration.locationString()} Service interfaces from FIR metadata: $serviceInterfaces")

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
}
