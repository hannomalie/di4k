import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FooModuleTest {

    @Test
    fun test() {
        assertThat(object: FooModuleImpl(Requirement()) {
            override val aDependency = Dependency("overridden")
        }.aComplexDependency).isNotNull
    }
}