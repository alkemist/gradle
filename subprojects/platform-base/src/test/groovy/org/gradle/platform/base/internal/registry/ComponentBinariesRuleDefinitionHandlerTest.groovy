/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.platform.base.internal.registry
import org.gradle.api.initialization.Settings
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.model.InvalidModelRuleDeclarationException
import org.gradle.model.collection.CollectionBuilder
import org.gradle.model.internal.inspect.DefaultMethodRuleDefinition
import org.gradle.model.internal.inspect.MethodRuleDefinition
import org.gradle.model.internal.inspect.RuleSourceDependencies
import org.gradle.model.internal.registry.ModelRegistry
import org.gradle.platform.base.*
import org.gradle.platform.base.binary.BaseBinarySpec
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Method
/**
 * Created by Rene on 05/09/14.
 */
class ComponentBinariesRuleDefinitionHandlerTest extends Specification {

    def ruleDefinition = Mock(MethodRuleDefinition)
    def modelRegistry = Mock(ModelRegistry)
    def ruleDependencies = Mock(RuleSourceDependencies)

    ComponentBinariesRuleDefinitionHandler ruleHandler = new ComponentBinariesRuleDefinitionHandler()

    def "handles methods annotated with @ComponentBinaries"() {
        when:
        1 * ruleDefinition.getAnnotation(ComponentBinaries) >> null

        then:
        !ruleHandler.spec.isSatisfiedBy(ruleDefinition)


        when:
        1 * ruleDefinition.getAnnotation(ComponentBinaries) >> Mock(ComponentBinaries)

        then:
        ruleHandler.spec.isSatisfiedBy(ruleDefinition)
    }

    def "applies ComponentModelBasePlugin and creates componentBinary rule"() {
        when:
        ruleHandler.register(ruleDefinitionForMethod("validTypeRule"), modelRegistry, ruleDependencies)

        then:
        1 * ruleDependencies.add(ComponentModelBasePlugin)

        and:
        1 * modelRegistry.mutate(_)
    }

    def ruleDefinitionForMethod(String methodName) {
        for (Method candidate : Rules.class.getDeclaredMethods()) {
            if (candidate.getName().equals(methodName)) {
                return DefaultMethodRuleDefinition.create(Rules.class, candidate)
            }
        }
        throw new IllegalArgumentException("Not a test method name")
    }

    @Unroll
    def "decent error message for #descr"() {
        def ruleMethod = ruleDefinitionForMethod(methodName)
        def ruleDescription = getStringDescription(ruleMethod)

        when:
        ruleHandler.register(ruleMethod, modelRegistry, ruleDependencies)

        then:
        def ex = thrown(InvalidModelRuleDeclarationException)
        ex.message == "${ruleDescription} is not a valid ComponentBinaries model rule method."
        ex.cause instanceof InvalidComponentModelException
        ex.cause.message == expectedMessage

        where:
        methodName                  | expectedMessage                                                                                                               | descr
        "noParams"                  | "ComponentBinaries method must have a parameter of type '${CollectionBuilder.name}'."                                         | "no CollectionBuilder parameter"
        "multipileComponentSpecs"   | "ComponentBinaries method must have one parameter extending ComponentSpec. Found multiple parameter extending ComponentSpec." | "additional component spec parameter"
        "noComponentSpec"           | "ComponentBinaries method must have one parameter extending ComponentSpec. Found no parameter extending ComponentSpec."       | "no component spec parameter"
        "missmatchingComponentSpec" | "ComponentBinaries method parameter of type SomeOtherLibrary does not support binaries of type SomeBinarySpec."               | "non matching CompnentSpec type"
        "returnValue"               | "ComponentBinaries method must not have a return value."                                                                      | "non void method"
    }

    def getStringDescription(MethodRuleDefinition ruleDefinition) {
        def builder = new StringBuilder()
        ruleDefinition.descriptor.describeTo(builder)
        builder.toString()
    }

    def aProjectPlugin() {
        ruleDependencies = ProjectBuilder.builder().build()
        _ * pluginApplication.target >> ruleDependencies
    }

    def aSettingsPlugin(def plugin) {
        Settings settings = Mock(Settings)
        _ * pluginApplication.target >> settings
        _ * pluginApplication.plugin >> plugin
        ruleHandler = new ComponentTypeRuleDefinitionHandler(instantiator)
    }

    interface SomeBinarySpec extends BinarySpec {}
    interface SomeOtherBinarySpec extends BinarySpec {}
    interface SomeLibrary extends ComponentSpec<SomeBinarySpec>{}
    interface SomeOtherLibrary extends ComponentSpec<SomeOtherBinarySpec>{}


    static class SomeBinarySpecImpl extends BaseBinarySpec implements SomeBinarySpec {}

    static class SomeBinarySpecOtherImpl extends SomeBinarySpecImpl {}

    interface NotBinarySpec {}

    static class NotImplementingCustomBinary extends BaseBinarySpec implements BinarySpec {}

    abstract static class NotExtendingBaseBinarySpec implements BinaryTypeRuleDefinitionHandlerTest.SomeBinarySpec {}

    static class NoDefaultConstructor extends BaseBinarySpec implements SomeBinarySpec {
        NoDefaultConstructor(String arg) {
        }
    }

    static class Rules {
        @ComponentBinaries
        static void validTypeRule(CollectionBuilder<SomeBinarySpec> binaries, SomeLibrary library) {
            binaries.create("${library.name}Binary", library)
        }

        @ComponentBinaries
        static void missmatchingComponentSpec(CollectionBuilder<SomeBinarySpec> binaries, SomeOtherLibrary library) {
            binaries.create("${library.name}Binary", library)
        }

        @ComponentBinaries
        static void multipileComponentSpecs(CollectionBuilder<SomeBinarySpec> binaries, SomeLibrary library, SomeLibrary otherLibrary) {
            binaries.create("${library.name}Binary", library)
        }

        @ComponentBinaries
        static void noParams() {
        }

        @ComponentBinaries
        static void noComponentSpec(CollectionBuilder<SomeBinarySpec> binaries) {
        }

        @ComponentBinaries
        static String returnValue(BinaryTypeBuilder<SomeBinarySpec> builder) {
        }
    }
}