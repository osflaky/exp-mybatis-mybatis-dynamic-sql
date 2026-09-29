/*
 *    Copyright 2016-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.dynamic.sql.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test classes that rely on Testcontainers need a Linux Docker engine. Some platforms (for example
 * Windows on ARM64) cannot provide one, so on those hosts the container based tests are reported as
 * disabled instead of erroring out, while every other test still runs. Registered through the JUnit
 * ServiceLoader mechanism.
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("Test does not require Docker, or Docker is available");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isEmpty() || !requiresDocker(testClass.get())) {
            return ENABLED;
        }

        if (DockerClientFactory.instance().isDockerAvailable()) {
            return ENABLED;
        }

        return ConditionEvaluationResult.disabled("Docker environment is not available on this platform");
    }

    private static boolean requiresDocker(Class<?> testClass) {
        for (Class<?> c = testClass; c != null; c = c.getEnclosingClass()) {
            if (AnnotationSupport.isAnnotated(c, Testcontainers.class) || hasStaticContainerField(c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasStaticContainerField(Class<?> c) {
        return !ReflectionSupport.findFields(c, DockerAvailableCondition::isStaticContainer,
                HierarchyTraversalMode.TOP_DOWN).isEmpty();
    }

    private static boolean isStaticContainer(Field field) {
        return Modifier.isStatic(field.getModifiers()) && GenericContainer.class.isAssignableFrom(field.getType());
    }
}
