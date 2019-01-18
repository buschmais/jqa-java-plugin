package com.buschmais.jqassistant.plugin.java.test.scanner;

import java.io.IOException;

import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.throwing.ThrowingClass;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.throwing.ThrowingClass2;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.throwing.ThrowingClass3;

import org.junit.jupiter.api.Test;

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
