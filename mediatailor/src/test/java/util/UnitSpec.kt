package util

import io.kotest.core.spec.style.DescribeSpec

@Suppress("UNCHECKED_CAST")
abstract class UnitSpec(body: UnitSpec.() -> Unit) : DescribeSpec(body as DescribeSpec.() -> Unit)