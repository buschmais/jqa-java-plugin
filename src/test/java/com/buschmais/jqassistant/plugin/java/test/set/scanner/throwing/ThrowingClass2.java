package com.buschmais.jqassistant.plugin.java.test.set.scanner.throwing;

import net.sourceforge.plantuml.Run;

public class ThrowingClass2 {
    public void throwingMethod() {
        try {
            throw new IllegalStateException();
        } catch (RuntimeException e) {
            throw new RuntimeException();
        }
    }

}