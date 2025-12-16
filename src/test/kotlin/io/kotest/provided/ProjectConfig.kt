package io.kotest.provided

import io.kotest.common.ExperimentalKotest
import io.kotest.core.config.AbstractProjectConfig
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension

object ProjectConfig : AbstractProjectConfig() {

    override fun extensions() = listOf(MicronautKotest5Extension)

    override val parallelism: Int =
        Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

    // Each spec gets its own PostgreSQL schema, so parallel execution is safe
    @OptIn(ExperimentalKotest::class)
    override val concurrentSpecs: Int? = parallelism

    // Tests within a single spec run sequentially (they share the same schema)
    @OptIn(ExperimentalKotest::class)
    override val concurrentTests: Int? = 1
}
