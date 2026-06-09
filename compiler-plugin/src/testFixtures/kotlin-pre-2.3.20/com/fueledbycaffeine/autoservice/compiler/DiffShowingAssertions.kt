package com.fueledbycaffeine.autoservice.compiler

/**
 * Pre-2.3.20 variant: `AssertionsService` does not declare `assertTimeoutPreemptively`,
 * so the base implementation is sufficient.
 */
object DiffShowingAssertions : DiffShowingAssertionsBase()
