/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.inject.writer;

import io.micronaut.context.AbstractBeanDefinitionReference;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanDefinitionReference;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes the bean definition class file to disk.
 *
 * @author Graeme Rocher
 * @see BeanDefinitionReference
 * @since 1.0
 */
@Internal
public class BeanDefinitionReferenceWriter extends AbstractAnnotationMetadataWriter {

    /**
     * Suffix for reference classes.
     */
    public static final String REF_SUFFIX = "Class";

    private final String beanTypeName;
    private final String beanDefinitionName;
    private final String beanDefinitionClassInternalName;
    private final String beanDefinitionReferenceClassName;
    private boolean contextScope = false;
    private boolean requiresMethodProcessing;

    /**
     * @param beanTypeName       The bean type name
     * @param beanDefinitionName The bean definition name
     * @param annotationMetadata The annotation metadata
     */
    public BeanDefinitionReferenceWriter(String beanTypeName, String beanDefinitionName, AnnotationMetadata annotationMetadata) {
        super(beanDefinitionName + REF_SUFFIX, annotationMetadata);
        this.beanTypeName = beanTypeName;
        this.beanDefinitionName = beanDefinitionName;
        this.beanDefinitionReferenceClassName = beanDefinitionName + REF_SUFFIX;
        this.beanDefinitionClassInternalName = getInternalName(beanDefinitionName) + REF_SUFFIX;
    }

    /**
     * Accept an {@link ClassWriterOutputVisitor} to write all generated classes.
     *
     * @param outputVisitor The {@link ClassWriterOutputVisitor}
     * @throws IOException If an error occurs
     */
    @Override
    public void accept(ClassWriterOutputVisitor outputVisitor) throws IOException {
        if (annotationMetadataWriter != null) {
            annotationMetadataWriter.accept(outputVisitor);
        }
        try (OutputStream outputStream = outputVisitor.visitClass(getBeanDefinitionQualifiedClassName())) {
            ClassWriter classWriter = generateClassBytes();
            outputStream.write(classWriter.toByteArray());
        }
        outputVisitor.visitServiceDescriptor(
            BeanDefinitionReference.class,
            beanDefinitionReferenceClassName
        );
    }

    /**
     * Set whether the bean should be in context scope.
     *
     * @param contextScope The context scope
     */
    public void setContextScope(boolean contextScope) {
        this.contextScope = contextScope;
    }

    /**
     * Sets whether the {@link BeanDefinition#requiresMethodProcessing()} returns true.
     *
     * @param shouldPreProcess True if they should be pre-processed
     */
    public void setRequiresMethodProcessing(boolean shouldPreProcess) {
        this.requiresMethodProcessing = shouldPreProcess;
    }

    /**
     * Obtains the class name of the bean definition to be written. Java Annotation Processors need
     * this information to create a JavaFileObject using a Filer.
     *
     * @return the class name of the bean definition to be written
     */
    public String getBeanDefinitionQualifiedClassName() {
        String newClassName = beanDefinitionName;
        if (newClassName.endsWith("[]")) {
            newClassName = newClassName.substring(0, newClassName.length() - 2);
        }
        return newClassName + REF_SUFFIX;
    }

    private ClassWriter generateClassBytes() {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        Type superType = Type.getType(AbstractBeanDefinitionReference.class);
        startClass(classWriter, beanDefinitionClassInternalName, superType);
        Type beanDefinitionType = getTypeReference(beanDefinitionName);
        writeAnnotationMetadataStaticInitializer(classWriter);

        GeneratorAdapter cv = startConstructor(classWriter);

        // ALOAD 0
        cv.loadThis();
        // LDC "..class name.."
        cv.push(beanTypeName);
        cv.push(beanDefinitionName);

        // INVOKESPECIAL AbstractBeanDefinitionReference.<init> (Ljava/lang/String;)V
        invokeConstructor(cv, AbstractBeanDefinitionReference.class, String.class, String.class);

        // RETURN
        cv.visitInsn(RETURN);
        // MAXSTACK = 2
        // MAXLOCALS = 1
        cv.visitMaxs(2, 1);

        // start method: BeanDefinition load()
        GeneratorAdapter loadMethod = startPublicMethodZeroArgs(classWriter, BeanDefinition.class, "load");

        // return new BeanDefinition()
        loadMethod.newInstance(beanDefinitionType);
        loadMethod.dup();
        loadMethod.invokeConstructor(beanDefinitionType, METHOD_DEFAULT_CONSTRUCTOR);

        // RETURN
        loadMethod.returnValue();
        loadMethod.visitMaxs(2, 1);

        // start method: boolean isContextScope()
        if (contextScope) {
            GeneratorAdapter isContextScopeMethod = startPublicMethodZeroArgs(classWriter, boolean.class, "isContextScope");
            isContextScopeMethod.push(true);
            isContextScopeMethod.returnValue();
            isContextScopeMethod.visitMaxs(1, 1);
        }

        // start method: Class getBeanDefinitionType()
        GeneratorAdapter getBeanDefinitionType = startPublicMethodZeroArgs(classWriter, Class.class, "getBeanDefinitionType");
        getBeanDefinitionType.push(beanDefinitionType);
        getBeanDefinitionType.returnValue();
        getBeanDefinitionType.visitMaxs(2, 1);

        // start method: Class getBeanType()
        GeneratorAdapter getBeanType = startPublicMethodZeroArgs(classWriter, Class.class, "getBeanType");
        getBeanType.push(getTypeReference(beanTypeName));
        getBeanType.returnValue();
        getBeanType.visitMaxs(2, 1);

        //noinspection Duplicates
        if (requiresMethodProcessing) {
            GeneratorAdapter requiresMethodProcessing = startPublicMethod(classWriter, "requiresMethodProcessing", boolean.class.getName());
            requiresMethodProcessing.push(true);
            requiresMethodProcessing.visitInsn(IRETURN);
            requiresMethodProcessing.visitMaxs(1, 1);
            requiresMethodProcessing.visitEnd();
        }

        writeGetAnnotationMetadataMethod(classWriter);

        return classWriter;
    }

}
