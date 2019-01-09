package com.buschmais.jqassistant.plugin.java.test.scanner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.plugin.java.api.model.AnnotationValueDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.FieldDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.MethodDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.TypeDescriptor;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.annotation.AnnotatedType;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.annotation.Annotation;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.annotation.AnnotationWithDefaultValue;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.annotation.Enumeration;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.annotation.NestedAnnotation;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.throwing.ThrowingClass;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.throwing.ThrowingClass2;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.throwing.ThrowingClass3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scala.reflect.internal.Trees.If;

import static com.buschmais.jqassistant.plugin.java.test.matcher.AnnotationValueDescriptorMatcher.annotationValueDescriptor;
import static com.buschmais.jqassistant.plugin.java.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
import static com.buschmais.jqassistant.plugin.java.test.matcher.MethodDescriptorMatcher.constructorDescriptor;
import static com.buschmais.jqassistant.plugin.java.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static com.buschmais.jqassistant.plugin.java.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static com.buschmais.jqassistant.plugin.java.test.matcher.ValueDescriptorMatcher.valueDescriptor;
import static com.buschmais.jqassistant.plugin.java.test.set.scanner.annotation.Enumeration.DEFAULT;
import static com.buschmais.jqassistant.plugin.java.test.set.scanner.annotation.Enumeration.NON_DEFAULT;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

public class EffectiveThrowablesIT extends AbstractJavaPluginIT {

    @Test
    void name() throws Exception {
        scanClasses(ThrowingClass.class);
        store.beginTransaction();

        store.commitTransaction();
    }

    @Test
    void bane2() throws IOException {
        scanClasses(ThrowingClass2.class);
    }

    @Test
    void bane3() throws IOException {
        scanClasses(ThrowingClass3.class);
    }
}
