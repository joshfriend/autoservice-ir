package com.fueledbycaffeine.autoservice.fir

import com.fueledbycaffeine.autoservice.AutoServiceSymbols
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * Generates synthetic "mirror" declarations for classes annotated with @AutoService.
 * 
 * This enables proper incremental compilation support:
 * - When a source file is deleted, the mirror class is deleted
 * - This triggers Kotlin's incremental compilation to handle the change
 * 
 * For a class like:
 * ```
 * @AutoService(MyService::class)
 * class MyServiceImpl : MyService
 * ```
 * 
 * We generate:
 * ```
 * class MyServiceImpl {
 *   private class `__AutoService__` { }
 * }
 * ```
 * 
 * The mirror class existence triggers IC tracking, so when the source file
 * is deleted, the mirror class deletion triggers proper cleanup.
 */
internal class AutoServiceMirrorFirGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {

  // Generated declaration key for our plugin
  internal object Key : GeneratedDeclarationKey() {
    override fun toString() = "AutoServiceMirror"
  }

  // Predicate to match @AutoService annotated classes
  private val autoServicePredicate = LookupPredicate.create {
    annotated(
      setOf(
        AutoServiceSymbols.FqNames.AUTOSERVICE, 
        AutoServiceSymbols.FqNames.GOOGLE_AUTOSERVICE,
      )
    )
  }

  // Track mirror classes we need to generate
  private val mirrorClassesToGenerate = mutableSetOf<ClassId>()

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(autoServicePredicate)
  }

  @OptIn(SymbolInternals::class)
  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
  ): Set<Name> {
    // Check if this class has @AutoService annotation (unresolved check)
    val hasAutoServiceAnnotation = classSymbol.fir.annotations.any { annotation ->
      val classId = annotation.annotationTypeRef.coneType.classId
      classId?.asSingleFqName() == AutoServiceSymbols.FqNames.AUTOSERVICE ||
        classId?.asSingleFqName() == AutoServiceSymbols.FqNames.GOOGLE_AUTOSERVICE
    }
    
    if (!hasAutoServiceAnnotation) {
      return emptySet()
    }

    val mirrorClassId = classSymbol.classId.createNestedClassId(AutoServiceSymbols.Names.MIRROR_CLASS)
    mirrorClassesToGenerate.add(mirrorClassId)
    
    return setOf(AutoServiceSymbols.Names.MIRROR_CLASS)
  }

  override fun generateNestedClassLikeDeclaration(
    owner: FirClassSymbol<*>,
    name: Name,
    context: NestedClassGenerationContext,
  ): FirClassLikeSymbol<*>? {
    if (name != AutoServiceSymbols.Names.MIRROR_CLASS) return null

    val mirrorClassId = owner.classId.createNestedClassId(name)
    if (mirrorClassId !in mirrorClassesToGenerate) return null

    return createNestedClass(owner, name, Key) {
      modality = Modality.FINAL
      visibility = Visibilities.Private
    }.symbol
  }

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    val classId = classSymbol.classId
    
    // For mirror classes, generate only the constructor
    if (classId in mirrorClassesToGenerate) {
      return setOf(SpecialNames.INIT)
    }
    
    return emptySet()
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    if (context.owner.classId !in mirrorClassesToGenerate) {
      return emptyList()
    }
    
    // Private constructor to prevent instantiation
    return listOf(createDefaultPrivateConstructor(context.owner, Key).symbol)
  }
}
