package com.fueledbycaffeine.autoservice

import kotlin.reflect.KClass

/**
 * An annotation for service providers as described in [java.util.ServiceLoader].
 * 
 * The annotation processor generates the configuration files that allow the annotated
 * class to be loaded with [ServiceLoader.load()][java.util.ServiceLoader.load].
 *
 * The annotated class must conform to the service provider specification:
 * - Must be a non-inner, non-anonymous, concrete class
 * - Must have a publicly accessible no-arg constructor
 * - Must implement the interface type(s) specified in [value] or inferred from its supertypes
 *
 * ## Type Inference
 *
 * If [value] is empty and the annotated class implements or extends exactly one interface
 * or class (besides [Any]), that type will be automatically used as the service interface:
 *
 * ```kotlin
 * interface Logger { fun log(msg: String) }
 * 
 * @AutoService  // Logger is automatically inferred
 * class ConsoleLogger : Logger {
 *     override fun log(msg: String) = println(msg)
 * }
 * ```
 *
 * ## Multiple Service Interfaces
 *
 * You can register a single implementation for multiple service interfaces:
 *
 * ```kotlin
 * @AutoService(ServiceA::class, ServiceB::class)
 * class MultiImpl : ServiceA, ServiceB {
 *     // ...
 * }
 * ```
 *
 * @param value The service interface(s) implemented by this provider.
 *              If empty, will be inferred from the class's single supertype.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
public annotation class AutoService(
  vararg val value: KClass<*> = []
)
