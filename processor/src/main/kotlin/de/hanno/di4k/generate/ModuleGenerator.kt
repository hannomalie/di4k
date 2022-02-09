package de.hanno.di4k.generate

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

private val KSClassDeclaration.isModule: Boolean
    get() = classKind == ClassKind.CLASS && modifiers.contains(Modifier.ABSTRACT) && simpleName.asString().contains("Module")

class ModuleGeneratorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val codeGenerator = environment.codeGenerator
        val logger = environment.logger

        return ModuleImplementationGenerator(logger, codeGenerator)
    }
}

class ModuleImplementationGenerator(val logger: KSPLogger, val codeGenerator: CodeGenerator) : SymbolProcessor {

    private val KSClassDeclaration.nonEmptyPackageName: String
        get() {
            val packageNameOrEmpty = packageName.asString()
            return if (packageNameOrEmpty.isNotEmpty()) "default" else packageNameOrEmpty
        }

    val visitor = FindPropertiesVisitor()
    private var invoked = false


    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) { return emptyList() }

        resolver.getAllFiles().toList().map {
            it.accept(visitor, Unit)
        }

        invoked = true
        return emptyList()
    }

    inner class FindPropertiesVisitor : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if(classDeclaration.isModule) {
                val abstractClassFqn = classDeclaration.qualifiedName!!.asString()
                val abstractClassName = classDeclaration.simpleName.asString()
                val implClassName = abstractClassName + "Impl"
                codeGenerator.createNewFile(
                    Dependencies.ALL_FILES,
                    classDeclaration.nonEmptyPackageName,
                    implClassName,
                    "kt"
                ).use { stream ->
                    var resultBodyCode = ""

                    val declaredProperties = classDeclaration.getDeclaredProperties()

                    val propertiesAndTypes = declaredProperties.associateWith { it.type.resolve().declaration }

                    val typesAndProperties: Map<KSPropertyDeclaration, KSDeclaration> = propertiesAndTypes.keys.associateWith { propertiesAndTypes[it]!! }

                    require(typesAndProperties.keys.groupBy { it }.all { it.value.size <= 1 }) { "Multiple or no providers for type found" } // TODO: Print which type

                    var requirementsList = ""
                    var requirementsParameterList = ""
                    declaredProperties.forEach { propertyDeclaration ->
                        val isAbstract = propertyDeclaration.modifiers.contains(Modifier.ABSTRACT)
                        val hasDefaultImplementation = !isAbstract

                        val isRequirement = classDeclaration.primaryConstructor!!.parameters.map { it.name!!.asString() }.contains(propertyDeclaration.simpleName.asString())

                        if(isRequirement) {
                            val propertyType = propertyDeclaration.type.resolve()

                            when(val propertyTypeDeclaration = propertyType.declaration) {
                                is KSClassDeclaration -> {
                                    val propertyTypeFqn = propertyTypeDeclaration.qualifiedName!!.asString()
                                    requirementsList += "${propertyDeclaration.simpleName.asString()}: $propertyTypeFqn,\n"
                                    requirementsParameterList += "${propertyDeclaration.simpleName.asString()},\n"
                                }
                            }
                        } else if(!hasDefaultImplementation) {

                            val propertyType = propertyDeclaration.type.resolve()

                            when(val propertyTypeDeclaration = propertyType.declaration) {
                                is KSClassDeclaration -> {
                                    resultBodyCode += if(propertyTypeDeclaration.qualifiedName?.asString() == "kotlin.Function0") {
                                        val typeParameter = propertyType.arguments.first()
                                        val typeParameterClassDeclaration = typeParameter.type!!.resolve().declaration as KSClassDeclaration
                                        val typeParameterTypeFqn = typeParameterClassDeclaration.qualifiedName!!.asString()
                                        val constructorParameterList = typeParameterClassDeclaration.getConstructorParameterList(typesAndProperties)
                                        "    override val ${propertyDeclaration.simpleName.asString()} = { $typeParameterTypeFqn($constructorParameterList) }\n"
                                    } else {
                                        val propertyTypeFqn = propertyTypeDeclaration.qualifiedName!!.asString()
                                        val constructorParameterList = propertyTypeDeclaration.getConstructorParameterList(typesAndProperties)
                                        "    override val ${propertyDeclaration.simpleName.asString()} = $propertyTypeFqn($constructorParameterList)\n"
                                    }
                                }
                            }
                        }
                    }
                    requirementsList = requirementsList.removeSuffix("\n")
                    requirementsParameterList = requirementsParameterList.removeSuffix("\n")

                    stream.write((
                        "open class $implClassName(\n" +
                            "    " + requirementsList +
                        "\n): $abstractClassFqn(\n" +
                            "    " + requirementsParameterList +
                        "\n) {\n").toByteArray())
                    stream.write(resultBodyCode.toByteArray())
                    stream.write("}".toByteArray())
                }
            }
        }

        private fun KSClassDeclaration.getConstructorParameterList(
            typesAndProperties: Map<KSPropertyDeclaration, KSDeclaration>
        ) = primaryConstructor!!.parameters.joinToString(", ") {
            val parameterType = it.type.resolve()
            val providedDependency =
                typesAndProperties.entries.firstOrNull { it.value.qualifiedName == parameterType.declaration.qualifiedName }

            providedDependency?.key?.simpleName?.asString()
                ?: "${parameterType.declaration.qualifiedName!!.asString()}()"
        }

        override fun visitFile(file: KSFile, data: Unit) {
            file.declarations.toList().map { it.accept(this, Unit) }
        }
    }
}
