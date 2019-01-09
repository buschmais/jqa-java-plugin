package com.buschmais.jqassistant.plugin.java.impl.scanner.visitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import com.buschmais.jqassistant.plugin.java.api.model.AnnotationValueDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.FieldDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.MethodDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.ParameterDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.TypeDescriptor;
import com.buschmais.jqassistant.plugin.java.api.model.VariableDescriptor;
import com.buschmais.jqassistant.plugin.java.api.scanner.SignatureHelper;
import com.buschmais.jqassistant.plugin.java.api.scanner.TypeCache;
import com.buschmais.jqassistant.plugin.java.api.scanner.TypeCache.CachedType;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodVisitor extends org.objectweb.asm.MethodVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorHelper.class);

    /**
     * Annotation indicating a synthetic parameter of a method.
     */
    private static final String JAVA_LANG_SYNTHETIC = "java.lang.Synthetic";
    private static final String THIS = "this";

    private TypeCache.CachedType containingType;
    private MethodDescriptor methodDescriptor;
    private VisitorHelper visitorHelper;
    private DependentTypeSignatureVisitor dependentTypeSignatureVisitor;
    private int syntheticParameters = 0;
    private int cyclomaticComplexity = 1;
    private Integer lineNumber = null;
    private Integer firstLineNumber = null;
    private Integer lastLineNumber = null;
    private Set<Integer> effectiveLines = new HashSet<>();
    private ThrowableContext throwableContext = new ThrowableContext();

    protected MethodVisitor(TypeCache.CachedType containingType, MethodDescriptor methodDescriptor, VisitorHelper visitorHelper,
            DependentTypeSignatureVisitor dependentTypeSignatureVisitor) {
        super(Opcodes.ASM5);
        this.containingType = containingType;
        this.methodDescriptor = methodDescriptor;
        this.visitorHelper = visitorHelper;
        this.dependentTypeSignatureVisitor = dependentTypeSignatureVisitor;
    }


    @Override
    public org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
        String annotationType = SignatureHelper.getType(desc);
        if (JAVA_LANG_SYNTHETIC.equals(annotationType)) {
            // Ignore synthetic parameters add the start of the signature, i.e.
            // determine the number of synthetic parameters
            syntheticParameters = Math.max(syntheticParameters, parameter + 1);
            return null;
        }
        ParameterDescriptor parameterDescriptor = visitorHelper.getParameterDescriptor(methodDescriptor, parameter - syntheticParameters);
        if (parameterDescriptor == null) {
            LOGGER.warn("Cannot find parameter with index " + (parameter - syntheticParameters) + " in method signature "
                    + containingType.getTypeDescriptor().getFullQualifiedName() + "#" + methodDescriptor.getSignature());
            return null;
        }
        AnnotationValueDescriptor annotationDescriptor = visitorHelper.addAnnotation(containingType, parameterDescriptor, SignatureHelper.getType(desc));
        return new AnnotationVisitor(containingType, annotationDescriptor, visitorHelper);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        String objectType = SignatureHelper.getObjectType(type);
        CachedType cachedType = visitorHelper.resolveType(objectType, containingType);

        if (Opcodes.NEW == opcode) {
            try {
                Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(objectType);

                if (Throwable.class.isAssignableFrom(aClass)) {
                    System.out.println("Ist ne Exception");
                    throwableContext.candidate = aClass;
                }
                System.out.println(aClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
        String fieldSignature = SignatureHelper.getFieldSignature(name, desc);
        TypeCache.CachedType targetType = visitorHelper.resolveType(SignatureHelper.getObjectType(owner), containingType);
        FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(targetType, fieldSignature);
        switch (opcode) {
        case Opcodes.GETFIELD:
        case Opcodes.GETSTATIC:
            visitorHelper.addReads(methodDescriptor, lineNumber, fieldDescriptor);
            break;
        case Opcodes.PUTFIELD:
        case Opcodes.PUTSTATIC:
            visitorHelper.addWrites(methodDescriptor, lineNumber, fieldDescriptor);
            break;
        }
    }


    @Override
    public void visitInsn(int opcode) {
        System.out.println("OPCODE="+opcode);
        if (Opcodes.ATHROW == opcode) {
            if (throwableContext.candidate == null) {
                throw new IllegalStateException("No candidate found for ATHROW");
            }

            throwableContext.push(throwableContext.candidate);
            throwableContext.candidate = null;
        }

        if (Opcodes.AASTORE == opcode) {
            throwableContext.candidate = null;
            throwableContext.throwables.poll();
        }

        super.visitInsn(opcode);
    }



    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, boolean itf) {
        String methodSignature = SignatureHelper.getMethodSignature(name, desc);
        TypeCache.CachedType targetType = visitorHelper.resolveType(SignatureHelper.getObjectType(owner), containingType);
        MethodDescriptor invokedMethodDescriptor = visitorHelper.getMethodDescriptor(targetType, methodSignature);
        visitorHelper.addInvokes(methodDescriptor, lineNumber, invokedMethodDescriptor);
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        if (cst instanceof Type) {
            visitorHelper.resolveType(SignatureHelper.getType((Type) cst), containingType);
        }
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        visitorHelper.resolveType(SignatureHelper.getType(desc), containingType);
    }

    @Override
    public void visitLocalVariable(final String name, final String desc, final String signature, final Label start, final Label end, final int index) {
        if (visitorHelper.getClassModelConfiguration().isMethodDeclaresVariable() && !THIS.equals(name)) {
            final VariableDescriptor variableDescriptor = visitorHelper.getVariableDescriptor(name, SignatureHelper.getFieldSignature(name, desc));
            if (signature == null) {
                TypeDescriptor type = visitorHelper.resolveType(SignatureHelper.getType((desc)), containingType).getTypeDescriptor();
                variableDescriptor.setType(type);
            } else {
                new SignatureReader(signature).accept(new AbstractTypeSignatureVisitor(containingType, visitorHelper) {

                    @Override
                    public SignatureVisitor visitArrayType() {
                        return dependentTypeSignatureVisitor;
                    }

                    @Override
                    public SignatureVisitor visitTypeArgument(char wildcard) {
                        return dependentTypeSignatureVisitor;
                    }

                    @Override
                    public void visitEnd(TypeDescriptor resolvedTypeDescriptor) {
                        variableDescriptor.setType(resolvedTypeDescriptor);
                    }
                });
            }
            methodDescriptor.getVariables().add(variableDescriptor);
        }
    }

    @Override
    public org.objectweb.asm.AnnotationVisitor visitAnnotationDefault() {
        return new AnnotationDefaultVisitor(containingType, this.methodDescriptor, visitorHelper);
    }

    @Override
    public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
        if (type != null) {
            String fullQualifiedName = SignatureHelper.getObjectType(type);
            visitorHelper.resolveType(fullQualifiedName, containingType);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        AnnotationValueDescriptor annotationDescriptor = visitorHelper.addAnnotation(containingType, methodDescriptor, SignatureHelper.getType(desc));
        return new AnnotationVisitor(containingType, annotationDescriptor, visitorHelper);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (this.lineNumber == null) {
            this.firstLineNumber = line;
            this.lastLineNumber = line;
        } else {
            this.firstLineNumber = Math.min(line, this.firstLineNumber);
            this.lastLineNumber = Math.max(line, this.lastLineNumber);
        }
        this.lineNumber = line;
        this.effectiveLines.add(line);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        cyclomaticComplexity++;
    }

    @Override
    public void visitEnd() {
        methodDescriptor.setCyclomaticComplexity(cyclomaticComplexity);
        if (firstLineNumber != null) {
            methodDescriptor.setFirstLineNumber(firstLineNumber);
        }
        if (lastLineNumber != null) {
            methodDescriptor.setLastLineNumber(lastLineNumber);
        }
        if (!effectiveLines.isEmpty()) {
            methodDescriptor.setEffectiveLineCount(effectiveLines.size());
        }

        throwableContext.reset();
    }


    private static class ThrowableContext {
        private Queue<Class<?>> throwables = new LinkedList<>();
        Class candidate;

        void reset() {
            candidate = null;
            throwables.clear();;
        }

        public void push(Class<?> klass) {
            throwables.add(klass);
        }
    }
}
