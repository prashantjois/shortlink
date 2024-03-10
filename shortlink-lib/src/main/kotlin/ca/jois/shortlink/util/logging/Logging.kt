package ca.jois.shortlink.util.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Logging {
  /**
   * Simple wrapper to make logging more kotlin-y Example:
   * ```
   * class SomeClass {
   *   companion object {
   *     private val logger = getLogger<SomeClass>()
   *   }
   * }
   *
   * ```
   */
  inline fun <reified T> getLogger(): Logger = LoggerFactory.getLogger(T::class.java)!!
}
