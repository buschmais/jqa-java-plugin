package com.buschmais.jqassistant.plugin.java.test.scanner;

import java.io.IOException;
import java.util.List;

import com.buschmais.jqassistant.plugin.java.api.model.ClassFileDescriptor;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.pojo.Pojo;

import org.junit.jupiter.api.Test;

import static com.buschmais.jqassistant.core.report.api.model.Result.Status.SUCCESS;
import static com.buschmais.jqassistant.plugin.java.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Verifies functionality related to byte code and java versions.
 */
public class ByteCodeVersionIT extends AbstractJavaPluginIT {

    @Test
    public void byteCodeVersion() throws IOException {
        scanClasses(Pojo.class);
        store.beginTransaction();
        List<ClassFileDescriptor> types = query("MATCH (t:Type) WHERE t.name='Pojo' RETURN t").getColumn("t");
        assertThat(types.size(), equalTo(1));
        ClassFileDescriptor pojo = types.get(0);
        assertThat(pojo.getByteCodeVersion(), equalTo(52)); // Java 8
        store.commitTransaction();

    }

    @Test
    public void javaVersion() throws Exception {
        scanClasses(Pojo.class);
        assertThat(applyConcept("java:JavaVersion").getStatus(), equalTo(SUCCESS));
        store.beginTransaction();
        List<ClassFileDescriptor> types = query("MATCH (t:Type) WHERE t.name='Pojo' and t.javaVersion='Java 8' RETURN t").getColumn("t");
        assertThat(types.size(), equalTo(1));
        ClassFileDescriptor pojo = types.get(0);
        assertThat(pojo, typeDescriptor(Pojo.class));
        store.commitTransaction();
    }
}
