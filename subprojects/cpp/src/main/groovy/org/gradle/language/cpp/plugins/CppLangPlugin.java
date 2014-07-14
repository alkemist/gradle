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
package org.gradle.language.cpp.plugins;

import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.language.base.internal.LanguageRegistration;
import org.gradle.language.base.internal.LanguageRegistry;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.language.cpp.CppSourceSet;
import org.gradle.language.cpp.internal.DefaultCppSourceSet;

/**
 * Adds core C++ language support.
 */
@Incubating
public class CppLangPlugin implements Plugin<ProjectInternal> {
    public void apply(ProjectInternal project) {
        project.getPlugins().apply(ComponentModelBasePlugin.class);
        project.getExtensions().getByType(LanguageRegistry.class).add(new Cpp());
    }

    private static class Cpp implements LanguageRegistration<CppSourceSet> {
        public String getName() {
            return "cpp";
        }

        public Class<CppSourceSet> getSourceSetType() {
            return CppSourceSet.class;
        }

        public Class<? extends CppSourceSet> getSourceSetImplementation() {
            return DefaultCppSourceSet.class;
        }
    }
}