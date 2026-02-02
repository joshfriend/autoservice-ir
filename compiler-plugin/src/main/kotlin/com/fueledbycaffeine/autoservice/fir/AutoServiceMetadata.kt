package com.fueledbycaffeine.autoservice.fir

import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.name.ClassId

/**
 * Metadata about @AutoService annotations, computed during FIR phase and consumed in IR phase.
 * 
 * This avoids duplicating the service interface inference logic in IR by having FIR
 * store the resolved service interfaces directly on the class declaration.
 *
 * @property serviceInterfaces The list of service interface ClassIds that this class provides.
 *                             Empty list means the class is not annotated with @AutoService.
 */
internal data class AutoServiceMetadata(
  val serviceInterfaces: List<ClassId>,
)

/**
 * Key for storing [AutoServiceMetadata] on FIR class declarations.
 */
internal object AutoServiceMetadataKey : FirDeclarationDataKey()

/**
 * Extension property to get/set [AutoServiceMetadata] on a [FirClass].
 */
internal var FirClass.autoServiceMetadata: AutoServiceMetadata?
  by FirDeclarationDataRegistry.data(AutoServiceMetadataKey)

/**
 * Extension property to read [AutoServiceMetadata] from an [IrClass] via its FIR metadata.
 * 
 * This allows IR to access the service interface information computed during FIR phase
 * without reparsing annotations or re-inferring types.
 * 
 * @return The [AutoServiceMetadata] if available, or null if the class has no FIR metadata
 *         or was not processed by AutoService FIR extensions.
 */
internal val IrClass.autoServiceMetadata: AutoServiceMetadata?
  get() {
    val firMetadata = metadata as? FirMetadataSource.Class ?: return null
    return firMetadata.fir.autoServiceMetadata
  }

