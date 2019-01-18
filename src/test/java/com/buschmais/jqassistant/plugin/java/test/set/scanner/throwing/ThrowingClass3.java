package com.buschmais.jqassistant.plugin.java.test.set.scanner.throwing;

public class ThrowingClass3 {
    public int value;

    public void throwingMethod() {
        try {
            if (value > 0) {
                throw new IllegalStateException();
            }
        } catch (IllegalStateException e) {
            throw new RuntimeException();
        }
    }

}
