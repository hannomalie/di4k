package de.hanno.di4k.generate

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ModuleGeneratorTest {

    @Test
    fun foo() {

        val source = """
            data class Dependency(val aString: String)
            data class FactoryDependency(val aString: String)
            data class ComplexDependency(val aString: String, val aDependency: Dependency)
            class Requirement

            abstract class FooModule(
                val requirement: Requirement,
            ) {
                val bar: String
                    get() = "barStringValue"
                val baz = "bazStringValue"

                abstract val factory: () -> FactoryDependency
                abstract val aDependency: Dependency
                abstract val aComplexDependency: ComplexDependency
            }
        """.trimIndent()
        val sourceFile = SourceFile.kotlin("FooModule.kt", source, false)

        val compilation = KotlinCompilation().apply {
            sources = listOf(sourceFile)
            symbolProcessorProviders = listOf(ModuleGeneratorProvider())
        }
        val result = compilation.compile()
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kotlinKspSources = compilation.kspSourcesDir.resolve("kotlin")
        assertThat(kotlinKspSources).isNotEmptyDirectory()
        assertThat(kotlinKspSources.resolve("FooModuleImpl.kt").readText()).isEqualTo("""
            open class FooModuleImpl(
                requirement: Requirement,
            ): FooModule(
                requirement,
            ) {
                override val factory = { FactoryDependency(bar) }
                override val aDependency = Dependency(bar)
                override val aComplexDependency = ComplexDependency(bar, aDependency)
            }
        """.trimIndent())
    }
}