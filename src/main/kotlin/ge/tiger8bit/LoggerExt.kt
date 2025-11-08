package ge.tiger8bit

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.full.companionObject

/**
 * Elegant logger extension for any class.
 * Usage: private val logger = getLogger()
 */
inline fun <reified T : Any> T.getLogger(): Logger =
    LoggerFactory.getLogger(
        T::class.companionObject?.java ?: T::class.java
    )

