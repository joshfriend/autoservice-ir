package com.fueledbycaffeine.autoservice.fir

import com.fueledbycaffeine.autoservice.AutoServiceSymbols
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.getDeprecationsProvider
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import java.util.concurrent.ConcurrentHashMap

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
 *   private class __AutoService__ { }
 * }
 * ```
 * 
 * The mirror class exists purely for incremental compilation tracking:
 * - When source files are deleted, the mirror is deleted, triggering recompilation
 * - FIR validates all @AutoService annotations during compilation
 * - IR extracts service interfaces directly from annotations (trusting FIR's validation)
 */
internal class AutoServiceMirrorFirGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
  internal object Key : GeneratedDeclarationKey() {
    override fun toString() = "AutoServiceMirror"
  }

  private val autoServicePredicate = LookupPredicate.create {
    annotated(
      setOf(
        AutoServiceSymbols.FqNames.AUTOSERVICE, 
        AutoServiceSymbols.FqNames.GOOGLE_AUTOSERVICE,
      )
    )
  }

  // Track mirror classes we need to generate (thread-safe for parallel compilation)
  private val mirrorClassesToGenerate: MutableSet<ClassId> = ConcurrentHashMap.newKeySet()

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(autoServicePredicate)
  }

  @OptIn(SymbolInternals::class)
  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
  ): Set<Name> {
    val firClass = classSymbol.fir
    val annotation = firClass.annotations.firstOrNull { annotation ->
      isAutoServiceAnnotation(annotation.annotationTypeRef)
    }
    
    if (annotation == null) {
      return emptySet()
    }

    // Note: Service interfaces metadata is stored by AutoServiceClassChecker
    // which runs after types are resolved. We just generate the mirror class here.

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

    return createNestedClass(owner, name, Key) {
      modality = Modality.FINAL
      visibility = Visibilities.Private
    }.apply { markAsDeprecatedHidden() }.symbol
  }

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    val classId = classSymbol.classId
    
    if (classId in mirrorClassesToGenerate) {
      return setOf(SpecialNames.INIT)
    }
    
    return emptySet()
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    if (context.owner.classId.shortClassName != AutoServiceSymbols.Names.MIRROR_CLASS) {
      return emptyList()
    }
    
    // Private constructor to prevent instantiation
    return listOf(createDefaultPrivateConstructor(context.owner, Key).symbol)
  }
  
  private fun isAutoServiceAnnotation(typeRef: FirTypeRef): Boolean {
    // During early FIR phases (like getNestedClassifiersNames called during
    // FirCompanionGenerationTransformer), type references may not be resolved yet.
    // If the type isn't resolved yet, return false - FIR runs multiple rounds and
    // will call us again once the type is resolved.
    if (typeRef !is FirResolvedTypeRef) return false

    val classId = typeRef.coneType.classId ?: return false
    return classId == AutoServiceSymbols.ClassIds.AUTOSERVICE ||
      classId == AutoServiceSymbols.ClassIds.GOOGLE_AUTOSERVICE
  }

  /**
   * Creates a @Deprecated(message = "...", level = DeprecationLevel.HIDDEN) annotation.
   * This hides the mirror class from the FIR generated code that is shown in the IDE.
   * Another genius strategy I stole from Metro.
   */
  private fun createDeprecatedHiddenAnnotation(): FirAnnotation {
    val deprecatedClassSymbol = session.symbolProvider
      .getClassLikeSymbolByClassId(StandardClassIds.Annotations.Deprecated) as FirRegularClassSymbol
    
    val deprecatedType = ConeClassLikeTypeImpl(
      deprecatedClassSymbol.toLookupTag(),
      emptyArray(),
      isMarkedNullable = false,
    )

    return buildAnnotation {
      annotationTypeRef = buildResolvedTypeRef { coneType = deprecatedType }
      argumentMapping = buildAnnotationArgumentMapping {
        mapping[Name.identifier("message")] = buildLiteralExpression(
          source = null,
          kind = ConstantValueKind.String,
          value = "Hides the ${AutoServiceSymbols.Names.MIRROR_CLASS} class generated for @AutoService incremental compilation support",
          setType = true,
        )
        mapping[Name.identifier("level")] = buildEnumEntryDeserializedAccessExpression {
          enumClassId = StandardClassIds.DeprecationLevel
          enumEntryName = Name.identifier("HIDDEN")
        }
      }
    }
  }

  /**
   * Marks the declaration as @Deprecated with HIDDEN level.
   */
  private fun FirClassLikeDeclaration.markAsDeprecatedHidden() {
    replaceAnnotations(annotations + listOf(createDeprecatedHiddenAnnotation()))
    replaceDeprecationsProvider(getDeprecationsProvider(session))
  }
}
