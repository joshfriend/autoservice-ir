package com.fueledbycaffeine.autoservice.fir

import com.fueledbycaffeine.autoservice.AutoServiceSymbols
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * FIR extension that provides IDE error checking for @AutoService annotations.
 *
 * This enables real-time error reporting in the IDE without requiring a full build.
 * Errors are shown as you type, improving the developer experience.
 */
internal class AutoServiceFirCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
  override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
    override val classCheckers: Set<FirClassChecker> = setOf(AutoServiceClassChecker)
  }
}

/**
 * Checks classes annotated with @AutoService for common errors:
 * - Abstract classes (service providers must be concrete)
 * - Non-public/non-internal visibility (must be accessible by ServiceLoader)
 * - Wrong class kinds (interface, enum, annotation, object)
 * - Missing service interface (no explicit interface and can't infer from supertypes)
 * - Not implementing declared service interface
 */
internal object AutoServiceClassChecker : FirClassChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(declaration: FirClass) {
    val autoServiceAnnotation = findAutoServiceAnnotation(declaration) ?: return
    val source = declaration.source ?: return

    checkNotAbstract(declaration, source)
    checkVisibility(declaration, source)
    checkClassKind(declaration, source)
    checkServiceInterfaces(declaration, autoServiceAnnotation, source)
  }

  private fun findAutoServiceAnnotation(declaration: FirClass): FirAnnotation? {
    return declaration.annotations.firstOrNull { annotation ->
      val classId = annotation.annotationTypeRef.coneType.classId
      classId == AutoServiceSymbols.ClassIds.AUTOSERVICE ||
        classId == AutoServiceSymbols.ClassIds.GOOGLE_AUTOSERVICE
    }
  }

  context(context: CheckerContext, reporter: DiagnosticReporter)
  private fun checkNotAbstract(declaration: FirClass, source: KtSourceElement) {
    if (declaration.isAbstract) {
      with(context) {
        reporter.reportOn(source, AutoServiceDiagnostics.AUTOSERVICE_ABSTRACT_CLASS)
      }
    }
  }

  context(context: CheckerContext, reporter: DiagnosticReporter)
  private fun checkVisibility(declaration: FirClass, source: KtSourceElement) {
    val visibility = declaration.visibility
    if (visibility != Visibilities.Public && visibility != Visibilities.Internal) {
      with(context) {
        reporter.reportOn(source, AutoServiceDiagnostics.AUTOSERVICE_VISIBILITY_ERROR)
      }
    }
  }

  context(context: CheckerContext, reporter: DiagnosticReporter)
  private fun checkClassKind(declaration: FirClass, source: KtSourceElement) {
    val classKindError = when (declaration.classKind) {
      ClassKind.INTERFACE -> "an interface"
      ClassKind.ENUM_CLASS -> "an enum class"
      ClassKind.ANNOTATION_CLASS -> "an annotation class"
      ClassKind.OBJECT -> "an object"
      else -> null
    }
    if (classKindError != null) {
      with(context) {
        reporter.reportOn(source, AutoServiceDiagnostics.AUTOSERVICE_WRONG_CLASS_KIND, classKindError)
      }
    }
  }

  context(context: CheckerContext, reporter: DiagnosticReporter)
  private fun checkServiceInterfaces(
    declaration: FirClass,
    annotation: FirAnnotation,
    source: KtSourceElement
  ) {
    val explicitServiceInterfaces = extractExplicitServiceInterfaces(annotation)
    val supertypes = declaration.superTypeRefs
      .mapNotNull { it.coneType.classId }
      .filter { it != StandardClassIds.Any }

    // Check: Must have a service interface (explicit or inferred)
    if (explicitServiceInterfaces.isEmpty() && (supertypes.isEmpty() || supertypes.size > 1)) {
      with(context) {
        reporter.reportOn(source, AutoServiceDiagnostics.AUTOSERVICE_MISSING_SERVICE_INTERFACE)
      }
      return
    }

    // Check: Class must implement the declared service interfaces
    val serviceInterfaces = explicitServiceInterfaces.ifEmpty {
      if (supertypes.size == 1) supertypes else emptyList()
    }

    for (serviceInterface in serviceInterfaces) {
      val implementsInterface = declaration.superTypeRefs.any { it.coneType.classId == serviceInterface }
      if (!implementsInterface) {
        with(context) {
          reporter.reportOn(
            source,
            AutoServiceDiagnostics.AUTOSERVICE_DOES_NOT_IMPLEMENT,
            serviceInterface.asFqNameString()
          )
        }
      }
    }
  }

  private fun extractExplicitServiceInterfaces(annotation: FirAnnotation): List<ClassId> {
    val argument = annotation.argumentMapping.mapping.values.firstOrNull() ?: return emptyList()
    return extractClassIdsFromArgument(argument)
  }

  private fun extractClassIdsFromArgument(argument: FirExpression): List<ClassId> {
    return when (argument) {
      is FirArrayLiteral -> argument.argumentList.arguments.flatMap { extractClassIdsFromArgument(it) }
      is FirVarargArgumentsExpression -> argument.arguments.flatMap { extractClassIdsFromArgument(it) }
      is FirGetClassCall -> listOfNotNull(argument.argument.resolvedType.classId)
      else -> emptyList()
    }
  }
}
