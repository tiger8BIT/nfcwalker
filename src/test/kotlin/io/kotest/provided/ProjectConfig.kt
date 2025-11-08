package io.kotest.provided

import io.kotest.common.ExperimentalKotest
import io.kotest.core.config.AbstractProjectConfig
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension

object ProjectConfig : AbstractProjectConfig() {
    override fun extensions() = listOf(MicronautKotest5Extension)

    // Parallelism across the whole project
    override val parallelism: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)

    // Kotest 5.x API: these are Int? (counts), not booleans; they are experimental and need opt-in
    @OptIn(ExperimentalKotest::class)
    override val concurrentSpecs: Int? = parallelism

    @OptIn(ExperimentalKotest::class)
    override val concurrentTests: Int? = parallelism
}
