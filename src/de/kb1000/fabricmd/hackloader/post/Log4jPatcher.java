/*
 * Copyright 2021 kb1000
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.kb1000.fabricmd.hackloader.post;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class Log4jPatcher {
    private static final String JMX_SERVER_CLASS = "org/apache/logging/log4j/core/jmx/Server";

    public static void patch() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (final InputStream is = contextClassLoader.getResourceAsStream("org/apache/logging/log4j/core/LoggerContext.class")) {
            if (is == null) {
                throw new NoClassDefFoundError("org.apache.logging.log4j.core.LoggerContext");
            }

            final ClassReader classReader = new ClassReader(is);
            final ClassWriter classWriter = new ClassWriter(0);
            classReader.accept(new ClassVisitor(ASM9, classWriter) {
                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    if (name.equals("propertyChangeListeners"))
                        return null;
                    return super.visitField(access, name, descriptor, signature, value);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (name.equals("firePropertyChangeEvent") || name.equals("addPropertyChangeListener") || name.equals("removePropertyChangeListener")) {
                        return null;
                    }
                    final MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                    if (name.equals("<init>")) {
                        return new MethodVisitor(ASM9, new MethodNode(access, name, descriptor, signature, exceptions)) {
                            private final MethodNode methodNode = ((MethodNode) mv);

                            @Override
                            public void visitEnd() {
                                super.visitEnd();
                                for (AbstractInsnNode insnNode : methodNode.instructions) {
                                    if (insnNode instanceof FieldInsnNode) {
                                        FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                                        // Do not check the type of the list here, it may change in the future
                                        // If it does not match, things will fail hard (with a visible exception)
                                        if (fieldInsnNode.name.equals("propertyChangeListeners") && fieldInsnNode.getOpcode() == PUTFIELD && fieldInsnNode.getPrevious() instanceof MethodInsnNode) {
                                            MethodInsnNode methodInsnNode = (MethodInsnNode) fieldInsnNode.getPrevious();
                                            if (methodInsnNode.name.equals("<init>") && methodInsnNode.desc.equals("()V") && methodInsnNode.getOpcode() == INVOKESPECIAL && methodInsnNode.getPrevious() instanceof InsnNode) {
                                                InsnNode dupInsnNode = (InsnNode) methodInsnNode.getPrevious();
                                                if (dupInsnNode.getOpcode() == DUP && dupInsnNode.getPrevious() instanceof TypeInsnNode) {
                                                    TypeInsnNode typeInsnNode = (TypeInsnNode) dupInsnNode.getPrevious();
                                                    if (typeInsnNode.getOpcode() == NEW && typeInsnNode.getPrevious() instanceof VarInsnNode) {
                                                        VarInsnNode varInsnNode = (VarInsnNode) typeInsnNode.getPrevious();
                                                        if (varInsnNode.var == 0 && varInsnNode.getOpcode() == ALOAD) {
                                                            methodNode.instructions.remove(fieldInsnNode);
                                                            methodNode.instructions.remove(methodInsnNode);
                                                            methodNode.instructions.remove(dupInsnNode);
                                                            methodNode.instructions.remove(typeInsnNode);
                                                            methodNode.instructions.remove(varInsnNode);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                methodNode.accept(methodVisitor);
                            }
                        };
                    }

                    // TODO: drop firePropertyChangeEvent calls
                    return new MethodVisitor(ASM9, new MethodNode(access, name, descriptor, signature, exceptions)) {
                        private final MethodNode methodNode = ((MethodNode) mv);

                        @Override
                        public void visitEnd() {
                            super.visitEnd();
                            ListIterator<AbstractInsnNode> it = methodNode.instructions.iterator();
                            while (it.hasNext()) {
                                AbstractInsnNode insnNode = it.next();
                                if (insnNode instanceof MethodInsnNode) {
                                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                                    if (methodInsnNode.name.equals("firePropertyChangeEvent") && methodInsnNode.desc.equals("(Ljava/beans/PropertyChangeEvent;)V") && methodInsnNode.getOpcode() == INVOKESPECIAL && methodInsnNode.getPrevious() instanceof MethodInsnNode) {
                                        MethodInsnNode initMethodInsnNode = (MethodInsnNode) methodInsnNode.getPrevious();
                                        if (initMethodInsnNode.name.equals("<init>") && Type.getMethodType(initMethodInsnNode.desc).getArgumentTypes().length == 4 && initMethodInsnNode.getOpcode() == INVOKESPECIAL && initMethodInsnNode.getPrevious() instanceof VarInsnNode) {
                                            VarInsnNode newVarInsnNode = (VarInsnNode) initMethodInsnNode.getPrevious();
                                            if (newVarInsnNode.getOpcode() == ALOAD && newVarInsnNode.getPrevious() instanceof VarInsnNode) {
                                                VarInsnNode oldVarInsnNode = (VarInsnNode) newVarInsnNode.getPrevious();
                                                if (oldVarInsnNode.getOpcode() == ALOAD && oldVarInsnNode.getPrevious() instanceof LdcInsnNode) {
                                                    LdcInsnNode ldcInsnNode = (LdcInsnNode) oldVarInsnNode.getPrevious();
                                                    if (ldcInsnNode.getPrevious() instanceof VarInsnNode) {
                                                        VarInsnNode thisVarInsnNode = (VarInsnNode) ldcInsnNode.getPrevious();
                                                        if (thisVarInsnNode.var == 0 && thisVarInsnNode.getOpcode() == ALOAD && thisVarInsnNode.getPrevious() instanceof InsnNode) {
                                                            InsnNode dupInsnNode = (InsnNode) thisVarInsnNode.getPrevious();
                                                            if (dupInsnNode.getOpcode() == DUP && dupInsnNode.getPrevious() instanceof TypeInsnNode) {
                                                                TypeInsnNode newInsnNode = (TypeInsnNode) dupInsnNode.getPrevious();
                                                                if (newInsnNode.getOpcode() == NEW && newInsnNode.desc.equals("java/beans/PropertyChangeEvent") && newInsnNode.getPrevious() instanceof VarInsnNode) {
                                                                    VarInsnNode firstThisVarInsnNode = (VarInsnNode) newInsnNode.getPrevious();
                                                                    if (firstThisVarInsnNode.var == 0 && firstThisVarInsnNode.getOpcode() == ALOAD) {
                                                                        methodNode.instructions.remove(methodInsnNode);
                                                                        methodNode.instructions.remove(initMethodInsnNode);
                                                                        methodNode.instructions.remove(newVarInsnNode);
                                                                        methodNode.instructions.remove(oldVarInsnNode);
                                                                        methodNode.instructions.remove(ldcInsnNode);
                                                                        methodNode.instructions.remove(thisVarInsnNode);
                                                                        methodNode.instructions.remove(dupInsnNode);
                                                                        methodNode.instructions.remove(newInsnNode);
                                                                        methodNode.instructions.remove(firstThisVarInsnNode);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (methodInsnNode.owner.equals(JMX_SERVER_CLASS) && methodInsnNode.name.equals("reregisterMBeansAfterReconfigure") && methodInsnNode.desc.equals("()V") && methodInsnNode.getOpcode() == INVOKESTATIC) {
                                        // Simply removing the instruction does not work, as that would leave an empty try block behind.
                                        it.set(new InsnNode(NOP));
                                    } else if (methodInsnNode.owner.equals(JMX_SERVER_CLASS) && methodInsnNode.name.equals("unregisterLoggerContext") && methodInsnNode.desc.equals("(Ljava/lang/String;)V") && methodInsnNode.getOpcode() == INVOKESTATIC) {
                                        // There's a String (from getName()) left on the stack, so POP is used.
                                        it.set(new InsnNode(POP));
                                    }
                                }
                            }
                            methodNode.accept(methodVisitor);
                        }
                    };
                }
            }, 0);
            Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, ByteBuffer.class, ProtectionDomain.class);
            defineClassMethod.setAccessible(true);
            defineClassMethod.invoke(contextClassLoader, "org.apache.logging.log4j.core.LoggerContext", ByteBuffer.wrap(classWriter.toByteArray()), null);
        }
    }
}
