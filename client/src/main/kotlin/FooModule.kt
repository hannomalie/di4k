data class Dependency(val aString: String)
data class ComplexDependency(val aString: String, val aDependency: Dependency)
class Requirement

abstract class FooModule(
    val requirement: Requirement
) {
    val bar: String
        get() = "barStringValue"

    abstract val aDependency: Dependency
    abstract val aComplexDependency: ComplexDependency
}