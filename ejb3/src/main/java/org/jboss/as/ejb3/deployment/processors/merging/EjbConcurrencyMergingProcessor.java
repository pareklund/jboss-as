/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.AccessTimeoutMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodsMetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class that can merge {@link javax.ejb.Lock} and {@link javax.ejb.AccessTimeout} metadata
 *
 * @author Stuart Douglas
 */
public class EjbConcurrencyMergingProcessor extends AbstractMergingProcessor<SessionBeanComponentDescription> {


    public EjbConcurrencyMergingProcessor() {
        super(SessionBeanComponentDescription.class);
    }

    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription componentConfiguration) {

        //handle lock annotations

        final RuntimeAnnotationInformation<LockType> lockData = MethodAnnotationAggregator.<Lock, LockType>runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, Lock.class);
        for (Map.Entry<String, List<LockType>> entry : lockData.getClassAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                componentConfiguration.setBeanLevelLockType(entry.getKey(), entry.getValue().get(0));
            }
        }
        for (Map.Entry<Method, List<LockType>> entry : lockData.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                componentConfiguration.setLockType(entry.getValue().get(0), MethodIdentifier.getIdentifierForMethod(entry.getKey()));
            }
        }

        final RuntimeAnnotationInformation<AccessTimeout> accessTimeout = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, AccessTimeout.class);
        for (Map.Entry<String, List<AccessTimeout>> entry : accessTimeout.getClassAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                componentConfiguration.setBeanLevelAccessTimeout(entry.getKey(), entry.getValue().get(0));
            }
        }
        for (Map.Entry<Method, List<AccessTimeout>> entry : accessTimeout.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                componentConfiguration.setAccessTimeout(entry.getValue().get(0), MethodIdentifier.getIdentifierForMethod(entry.getKey()));
            }
        }

    }

    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        if (componentConfiguration.getDescriptorData() == null) {
            return;
        }
        SessionBeanMetaData sessionBeanMetaData = componentConfiguration.getDescriptorData();
        if (sessionBeanMetaData instanceof SessionBean31MetaData) {
            SessionBean31MetaData descriptor = (SessionBean31MetaData) sessionBeanMetaData;

            //handle lock
            if (descriptor.getLockType() != null) {
                componentConfiguration.setBeanLevelLockType(componentConfiguration.getEJBClassName(), descriptor.getLockType());
            }

            //handle access timeout
            if (descriptor.getAccessTimeout() != null) {
                componentConfiguration.setBeanLevelAccessTimeout(componentConfiguration.getEJBClassName(), getAccessTimeout(descriptor.getAccessTimeout()));
            }

            final ConcurrentMethodsMetaData methods = descriptor.getConcurrentMethods();
            if (methods != null) {
                for (final ConcurrentMethodMetaData method : methods) {
                    final Method realMethod = resolveMethod(deploymentReflectionIndex, componentClass, method.getMethod());
                    final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifierForMethod(realMethod);
                    if (method.getLockType() != null) {
                        componentConfiguration.setLockType(method.getLockType(), methodIdentifier);
                    }
                    if (method.getAccessTimeout() != null) {
                        componentConfiguration.setAccessTimeout(getAccessTimeout(method.getAccessTimeout()), methodIdentifier);
                    }

                }
            }


        }
    }

    private AccessTimeout getAccessTimeout(final AccessTimeoutMetaData accessTimeout) {
        return new AccessTimeout() {
            @Override
            public long value() {
                return accessTimeout.getTimeout();
            }

            @Override
            public TimeUnit unit() {
                return accessTimeout.getUnit();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return AccessTimeout.class;
            }
        };
    }


    private Method resolveMethod(final DeploymentReflectionIndex index, final Class<?> componentClass, final NamedMethodMetaData methodData) throws DeploymentUnitProcessingException {
        if (componentClass == null) {
            throw new DeploymentUnitProcessingException("Could not find method" + methodData.getMethodName() + "with parameter types" + methodData.getMethodParams() + " referenced in ejb-jar.xml");
        }
        final ClassReflectionIndex<?> classIndex = index.getClassIndex(componentClass);

        if (methodData.getMethodParams() == null) {
            final Collection<Method> methods = classIndex.getAllMethods(methodData.getMethodName());
            if (methods.isEmpty()) {
                return resolveMethod(index, (Class<Object>) componentClass.getSuperclass(), methodData);
            } else if (methods.size() > 1) {
                throw new DeploymentUnitProcessingException("More than one method " + methodData.getMethodName() + "found on class" + componentClass.getName() + " referenced in ejb-jar.xml. Specify the parameter types to resolve the ambiguity");
            }
            return methods.iterator().next();
        } else {
            final Collection<Method> methods = classIndex.getAllMethods(methodData.getMethodName(), methodData.getMethodParams().size());
            for (final Method method : methods) {
                boolean match = true;
                for (int i = 0; i < method.getParameterTypes().length; ++i) {
                    if (!method.getParameterTypes()[i].getName().equals(methodData.getMethodParams().get(i))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return method;
                }
            }
        }
        return resolveMethod(index, (Class<Object>) componentClass.getSuperclass(), methodData);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
