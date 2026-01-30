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
import org.jetbrains.kotlin.fir.types.*

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
 */
internal object AutoServiceClassChecker : FirClassChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(declaration: FirClass) {
    // Only check classes with @AutoService annotation
    val hasAnnotation = declaration.annotations.any { annotation ->
      val annotationType = annotation.annotationTypeRef.coneType
      val classId = annotationType.classId
      classId == AutoServiceSymbols.ClassIds.AUTOSERVICE ||
        classId == AutoServiceSymbols.ClassIds.GOOGLE_AUTOSERVICE
    }

    if (!hasAnnotation) return

    val source = declaration.source ?: return

    // Check 1: Class must not be abstract
    if (declaration.isAbstract) {
      with(context) {
        reporter.reportOn(
          source,
          AutoServiceFirErrors.AUTOSERVICE_WRONG_CLASS_KIND
        )
      }
    }

    // Check 2: Class must be public or internal
    val visibility = declaration.visibility
    if (visibility != Visibilities.Public && visibility != Visibilities.Internal) {
      with(context) {
        reporter.reportOn(
          source,
          AutoServiceFirErrors.AUTOSERVICE_NON_PUBLIC_CLASS
        )
      }
    }

    // Check 3: Class must not be an interface, enum, annotation, or object
    when (declaration.classKind) {
      ClassKind.INTERFACE,
      ClassKind.ENUM_CLASS,
      ClassKind.ANNOTATION_CLASS,
      ClassKind.OBJECT -> {
        with(context) {
          reporter.reportOn(
            source,
            AutoServiceFirErrors.AUTOSERVICE_WRONG_CLASS_KIND
          )
        }
      }
      else -> {
        // CLASS - this is valid
      }
    }
  }
}
