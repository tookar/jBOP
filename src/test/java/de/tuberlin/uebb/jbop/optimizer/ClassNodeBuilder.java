/*
 * Copyright (C) 2013 uebb.tu-berlin.de.
 * 
 * This file is part of JBOP (Java Bytecode OPtimizer).
 * 
 * JBOP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JBOP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JBOP. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tuberlin.uebb.jbop.optimizer;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DCMPG;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.IASTORE;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.JSR;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.SALOAD;
import static org.objectweb.asm.Opcodes.SASTORE;
import static org.objectweb.asm.Opcodes.SIPUSH;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.access.ClassAccessor;
import de.tuberlin.uebb.jbop.access.ClassDescriptor;
import de.tuberlin.uebb.jbop.access.ConstructorBuilder;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Base class for {@link IOptimizer}-Tests.
 * 
 * This is a fluent-api for creating ClassNodes.
 * 
 * @author Christopher Ewest
 */
public final class ClassNodeBuilder {
  
  /** The class node. */
  private final ClassNode classNode;
  
  private MethodNode lastMethod;
  private MethodNode lastConstructor;
  private FieldNode lastField;
  
  private Object lastElement;
  
  /** The lastConstructor. */
  private Class<?> buildedClass;
  
  private int constructorVarIndex = 1;
  
  private int lastConstructorVarIndex = 1;
  
  private int methodVarIndex = 1;
  
  private int lastMethodVarIndex;
  
  private Type lastVarElementType;
  
  private final boolean isInterface;
  
  private ClassNodeBuilder(final String className, final String superClass, final String constructorDesc,
      final String superConstructorDesc, final boolean isInterface) {
    this.isInterface = isInterface;
    classNode = new ClassNode(Opcodes.ASM5);
    classNode.access = ACC_PUBLIC;
    classNode.name = className.replace(".", "/");
    if (superClass != null) {
      classNode.superName = superClass.replace(".", "/");
    }
    classNode.version = Opcodes.V1_7;
    if (!isInterface) {
      addConstructor(constructorDesc, superConstructorDesc);
    } else {
      classNode.access |= ACC_INTERFACE;
      classNode.access |= ACC_ABSTRACT;
    }
    lastElement = null;
  }
  
  /**
   * Creates a new ClassNode with the given name and a default Constructor.
   * 
   * @param className
   *          the class name
   * @return the abstract optimizer test
   */
  public static ClassNodeBuilder createClass(final String className) {
    return createClass(className, "()V", Type.getInternalName(Object.class), "()V");
  }
  
  /**
   * Creates a new ClassNode with the given name and a default Constructor.
   * 
   * @param className
   *          the class name
   * @return the abstract optimizer test
   */
  public static ClassNodeBuilder createInterface(final String className) {
    return new ClassNodeBuilder(className, Type.getInternalName(Object.class), null, null, true);
  }
  
  /**
   * Creates a new ClassNode with the given name , superClass and Constructor.
   * 
   * @param className
   *          the class name
   * @param constructorDesc
   *          the constructor desc
   * @param superClass
   *          the super class
   * @param superConstructorDesc
   *          the super constructor desc
   * @return the abstract optimizer test
   */
  public static ClassNodeBuilder createClass(final String className, final String constructorDesc,
      final String superClass, final String superConstructorDesc) {
    final ClassNodeBuilder builder = new ClassNodeBuilder(className, superClass, constructorDesc, superConstructorDesc,
        false);
    return builder;
  }
  
  /**
   * Appends a Constructor with the given descriptors.
   * 
   * @param constructorDesc
   *          the constructor desc
   * @param superConstructorDesc
   *          the super constructor desc
   * @return the class node builder
   */
  public ClassNodeBuilder addConstructor(final String constructorDesc, final String superConstructorDesc) {
    if (isInterface) {
      return this;
    }
    addMethod("<init>", constructorDesc);//
    lastConstructorVarIndex = 1;
    final InsnList list = new InsnList();
    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
    final Type methodType = Type.getMethodType(superConstructorDesc);
    for (final Type parameterType : methodType.getArgumentTypes()) {
      list.add(new VarInsnNode(parameterType.getOpcode(ILOAD), lastConstructorVarIndex));
      lastConstructorVarIndex += parameterType.getSize();
    }
    list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, classNode.superName, "<init>", superConstructorDesc));
    list.add(new InsnNode(Opcodes.RETURN));
    addToConstructor(list);
    return this;
  }
  
  /**
   * Creates a getter for the last added field.
   * 
   * @return the class node builder
   */
  public ClassNodeBuilder withGetter() {
    if (isInterface) {
      return this;
    }
    final String name = lastField.name;
    final String desc = lastField.desc;
    addMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1), "()" + desc);
    final Type type = Type.getType(desc);
    addInsn(new VarInsnNode(Opcodes.ALOAD, 0));
    addInsn(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, name, desc));
    addInsn(new InsnNode(type.getOpcode(Opcodes.IRETURN)));
    return this;
  }
  
  /**
   * Creates a setter for the last added field.
   * 
   * @return the class node builder
   */
  public ClassNodeBuilder withSetter() {
    if (isInterface) {
      return this;
    }
    final String name = lastField.name;
    final String desc = lastField.desc;
    addMethod("set" + Character.toUpperCase(name.charAt(0)) + name.substring(1), "(" + desc + ")V");
    final Type type = Type.getType(desc);
    addInsn(new VarInsnNode(Opcodes.ALOAD, 0));
    addInsn(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), 1));
    addInsn(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, name, desc));
    addInsn(new InsnNode(Opcodes.RETURN));
    return this;
  }
  
  /**
   * Creates a getter and setter for the last added field.
   * 
   * @return the class node builder
   */
  public ClassNodeBuilder withGetterAndSetter() {
    if (isInterface) {
      return this;
    }
    withGetter().withSetter();
    return this;
  }
  
  /**
   * adds a new empty void-method containing only the returnstatement.
   * 
   * @param methodName
   *          the method name
   * @return the class node builder
   */
  public ClassNodeBuilder addEmptyMethod(final String methodName) {
    if ("<init>".equals(methodName)) {
      return this;
    }
    addMethod(methodName, "()V");
    if (isInterface) {
      return this;
    }
    addInsn(new InsnNode(Opcodes.RETURN));
    return this;
  }
  
  /**
   * adds a new method (public accessor) with the given name and descriptor to the classNode.
   * 
   * @param methodName
   *          the method name
   * @param descriptor
   *          the descriptor
   * @param modifiers
   *          the modifiers
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addMethod(final String methodName, final String descriptor, final int... modifiers) {
    methodVarIndex = 1;
    final int modifier = getEffectiveModifier(modifiers);
    final MethodNode method = new MethodNode(modifier, methodName, descriptor, null, new String[] {});
    method.exceptions = new ArrayList<>();
    method.invisibleAnnotations = new ArrayList<>();
    method.visibleAnnotations = new ArrayList<>();
    classNode.methods.add(method);
    setLastMethod(method);
    if (isInterface) {
      method.access |= ACC_ABSTRACT;
    }
    return this;
  }
  
  private int getEffectiveModifier(final int... modifiers) {
    int modifier = 0;
    if (modifiers == null || modifiers.length == 0) {
      modifier = Opcodes.ACC_PUBLIC;
    } else {
      for (int i = 0; i < modifiers.length; ++i) {
        modifier |= modifiers[i];
      }
    }
    return modifier;
  }
  
  /**
   * Adds the given annotation to the last created element.
   * The values have to be key-value pairs
   * 
   * @param annotationClass
   *          the annotation class
   * @param values
   *          the values
   * @return the class node builder
   */
  public ClassNodeBuilder withAnnotation(final Class<?> annotationClass, final Object... values) {
    final AnnotationNode annotationNode = new AnnotationNode(Type.getDescriptor(annotationClass));
    if (values != null && values.length > 0) {
      annotationNode.values = Arrays.asList(values);
    }
    if (lastElement instanceof MethodNode) {
      lastMethod.visibleAnnotations.add(annotationNode);
    } else if (lastElement instanceof FieldNode) {
      lastField.visibleAnnotations.add(annotationNode);
    } else {
      classNode.visibleAnnotations.add(annotationNode);
    }
    return this;
  }
  
  /**
   * Adds a new Field with the given name and descriptor to the classNode.
   * for primitive Types and String a default value can be given via value.
   * 
   * @param fieldName
   *          the field name
   * @param descriptor
   *          the descriptor
   * @param value
   *          the value
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addField(final String fieldName, final String descriptor, final Object... value) {
    if (isInterface) {
      return this;
    }
    final FieldNode fieldNode = new FieldNode(Opcodes.ACC_PRIVATE, fieldName, descriptor, null, value == null ? null
        : value.length > 0 ? value[0] : null);
    classNode.fields.add(fieldNode);
    lastField = fieldNode;
    lastElement = fieldNode;
    fieldNode.invisibleAnnotations = new ArrayList<>();
    fieldNode.visibleAnnotations = new ArrayList<>();
    return this;
  }
  
  /**
   * Sets the access modifier of the last added element.
   * 
   * @param modifiers
   *          the modifiers
   * @return the class node builder
   */
  public ClassNodeBuilder withModifiers(final int... modifiers) {
    final int effectiveModifier = getEffectiveModifier(modifiers);
    if (lastElement instanceof MethodNode) {
      lastMethod.access = effectiveModifier;
    } else if (lastElement instanceof FieldNode) {
      lastField.access = effectiveModifier;
    } else {
      classNode.access = effectiveModifier;
    }
    return this;
  }
  
  /**
   * Initializes a classField in the default lastConstructor
   * (eg: if fieldType of field "field" is "TestObject", field = new TestObject() is called).
   * 
   * Numbers and Strings are used to assign "field", every other value leads to new Object.
   * 
   * @param object
   *          the object to Use.
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder initWith(final Object object) {
    if (isInterface) {
      return this;
    }
    final InsnList list = new InsnList();
    if (object instanceof String || object instanceof Number || object instanceof Boolean
        || object instanceof Character) {
      list.add(new VarInsnNode(Opcodes.ALOAD, 0));
      final AbstractInsnNode numberNode = NodeHelper.getInsnNodeFor(object);
      list.add(numberNode);
      if (lastField.desc.startsWith("L") && object instanceof Number) {
        list.add(ConstructorBuilder.getBoxingNode(lastField));
      }
      list.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, lastField.name, lastField.desc));
    } else {
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(newObject(lastField.desc));
      list.add(new MethodInsnNode(INVOKESPECIAL, StringUtils.removeEnd(StringUtils.removeStart(lastField.desc, "L"),
          ";"), "<init>", "()V"));
      list.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, lastField.name, lastField.desc));
      
    }
    addToConstructor(list);
    return this;
  }
  
  /**
   * Creates Instruction to instantiate a new Object of type "desc".
   * 
   * @param desc
   *          the desc
   * @return the insn list
   */
  private InsnList newObject(final String desc) {
    final InsnList list = new InsnList();
    final String fieldDesc = StringUtils.removeEnd(StringUtils.removeStart(desc, "L"), ";");
    final TypeInsnNode node = new TypeInsnNode(Opcodes.NEW, fieldDesc);
    list.add(node);
    list.add(new InsnNode(DUP));
    return list;
  }
  
  /**
   * Creates Instruction to instantiate a new Object of the given type.
   * 
   * @param builder
   *          the builder
   * @return this ClassNodeBuilder
   */
  public ClassNodeBuilder addNewObject(final ClassNodeBuilder builder) {
    if (isInterface) {
      return this;
    }
    return addNewObject(builder.getDesc());
  }
  
  /**
   * Creates Instruction to instantiate a new Object of type "desc".
   * 
   * @param desc
   *          the desc
   * @return the insn list
   */
  public ClassNodeBuilder addNewObject(final String desc) {
    if (isInterface) {
      return this;
    }
    final InsnList newObject = newObject(desc);
    return addInsn(newObject);
  }
  
  /**
   * Initializes a classField of type Array in the default Constructor.
   * The array is initialized with the given Numbers.
   * 
   * @param values
   *          the values
   * @return the abstract optimizer test
   * @see AbstractOptimizerTest#initArrayInConstructor(String, int)
   */
  public ClassNodeBuilder initArrayWith(final Number... values) {
    if (isInterface) {
      return this;
    }
    final Type elementType = Type.getType(lastField.desc).getElementType();
    if (elementType.getDescriptor().startsWith("L")) {
      initArrayInternal(Opcodes.AASTORE, (Object[]) values);
      return this;
    }
    final Type type = toPrimitive(elementType);
    initArrayInternal(type.getOpcode(Opcodes.IASTORE), (Object[]) values);
    return this;
  }
  
  /**
   * Inits the multi array with.
   * 
   * @param value
   *          the value
   * @param indexes
   *          the indexes
   * @return the class node builder
   */
  public ClassNodeBuilder initMultiArrayWith(final Object value, final int... indexes) {
    if (isInterface) {
      return this;
    }
    final InsnList list = new InsnList();
    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
    list.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, lastField.name, lastField.desc));
    for (int i = 0; i < indexes.length - 1; ++i) {
      list.add(NodeHelper.getInsnNodeFor(indexes[i]));
      list.add(new InsnNode(Opcodes.AALOAD));
    }
    list.add(NodeHelper.getInsnNodeFor(indexes[indexes.length - 1]));
    list.add(NodeHelper.getInsnNodeFor(value));
    final Type elementType = Type.getType(lastField.desc).getElementType();
    if (elementType.getDescriptor().startsWith("L")) {
      list.add(new InsnNode(Opcodes.AASTORE));
    } else {
      list.add(new InsnNode(toPrimitive(Type.getType(value.getClass())).getOpcode(Opcodes.IASTORE)));
      
    }
    addToConstructor(list);
    return this;
  }
  
  private Type toPrimitive(final Type type) {
    if (Type.getType(Double.class).equals(type)) {
      return Type.DOUBLE_TYPE;
    }
    if (Type.getType(Float.class).equals(type)) {
      return Type.FLOAT_TYPE;
    }
    if (Type.getType(Long.class).equals(type)) {
      return Type.LONG_TYPE;
    }
    if (Type.getType(Integer.class).equals(type)) {
      return Type.INT_TYPE;
    }
    if (Type.getType(Short.class).equals(type)) {
      return Type.SHORT_TYPE;
    }
    if (Type.getType(Byte.class).equals(type)) {
      return Type.BYTE_TYPE;
    }
    if (Type.getType(Character.class).equals(type)) {
      return Type.CHAR_TYPE;
    }
    if (Type.getType(Boolean.class).equals(type)) {
      return Type.BOOLEAN_TYPE;
    }
    return type;
  }
  
  /**
   * Initializes a classField of type Array in the default lastConstructor.
   * The array is initialized with the given Strings.
   * 
   * @param values
   *          the values
   * @return the abstract optimizer test
   * @see AbstractOptimizerTest#initArrayInConstructor(String, int)
   */
  public ClassNodeBuilder initArrayWith(final String... values) {
    if (isInterface) {
      return this;
    }
    initArrayInternal(Opcodes.AASTORE, (Object[]) values);
    return this;
  }
  
  private void initArrayInternal(final int opcode, final Object... values) {
    final InsnList list = new InsnList();
    final int length = values.length;
    initArray(length);
    int index = 0;
    for (final Object number : values) {
      list.add(new VarInsnNode(Opcodes.ALOAD, lastConstructorVarIndex));
      final AbstractInsnNode indexNode = NodeHelper.getInsnNodeFor(index++);
      list.add(indexNode);
      final AbstractInsnNode numberNode = NodeHelper.getInsnNodeFor(number);
      list.add(numberNode);
      if (number instanceof Number && opcode == Opcodes.ASTORE) {
        list.add(ConstructorBuilder.getBoxingNode(lastField));
      }
      list.add(new InsnNode(opcode));
    }
    addToConstructor(list);
  }
  
  /**
   * Initializes a classField of type Array in the default lastConstructor with the given length.
   * (eg: if fieldType of field "field" is "double[]", field = new double[length] is called).
   * 
   * @param length
   *          the length
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder initArray(final int... length) {
    if (isInterface) {
      return this;
    }
    final InsnList list = new InsnList();
    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
    final AbstractInsnNode node;
    if (length.length == 1) {
      final Type elementType = Type.getType(lastField.desc).getElementType();
      list.add(NodeHelper.getInsnNodeFor(Integer.valueOf(length[0])));
      if (elementType.getDescriptor().startsWith("L")) {
        node = new TypeInsnNode(Opcodes.ANEWARRAY, elementType.getInternalName());
      } else {
        node = new IntInsnNode(Opcodes.NEWARRAY, getSort(elementType));
      }
    } else {
      for (final int currentLength : length) {
        list.add(NodeHelper.getInsnNodeFor(Integer.valueOf(currentLength)));
      }
      node = new MultiANewArrayInsnNode(lastField.desc, length.length);
    }
    list.add(node);
    list.add(new VarInsnNode(Opcodes.ASTORE, constructorVarIndex));
    list.add(new VarInsnNode(Opcodes.ALOAD, constructorVarIndex));
    lastConstructorVarIndex = constructorVarIndex;
    constructorVarIndex++;
    list.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, lastField.name, lastField.desc));
    addToConstructor(list);
    return this;
  }
  
  /**
   * Adds the array to the last created method.
   * 
   * @param desc
   *          the desc
   * @param length
   *          the length
   * @return the class node builder
   */
  public ClassNodeBuilder addArray(final String desc, final int... length) {
    if (isInterface) {
      return this;
    }
    final Type elementType = Type.getType(desc).getElementType();
    if (length.length == 1) {
      
      addInsn(NodeHelper.getInsnNodeFor(Integer.valueOf(length[0])));
      if (elementType.getDescriptor().startsWith("L")) {
        addInsn(new TypeInsnNode(Opcodes.ANEWARRAY, elementType.getInternalName()));
      } else {
        addInsn(new IntInsnNode(Opcodes.NEWARRAY, getSort(elementType)));
      }
    } else {
      for (final int currentLength : length) {
        addInsn(NodeHelper.getInsnNodeFor(Integer.valueOf(currentLength)));
      }
      addInsn(new MultiANewArrayInsnNode(desc, length.length));
    }
    addInsn(new VarInsnNode(Opcodes.ASTORE, methodVarIndex));
    lastMethodVarIndex = methodVarIndex;
    lastVarElementType = elementType;
    methodVarIndex++;
    return this;
  }
  
  /**
   * Stores the given value of type Number or String in the array created before at index indexes...
   * 
   * @param value
   *          the value
   * @param indexes
   *          the indexes
   * @return the class node builder
   */
  public ClassNodeBuilder withValue(final Object value, final int... indexes) {
    if (isInterface) {
      return this;
    }
    addInsn(new VarInsnNode(Opcodes.ALOAD, lastMethodVarIndex));
    for (int i = 0; i < indexes.length - 1; ++i) {
      addInsn(NodeHelper.getInsnNodeFor(indexes[i]));
      addInsn(new InsnNode(Opcodes.AALOAD));
    }
    addInsn(NodeHelper.getInsnNodeFor(indexes[indexes.length - 1]));
    addInsn(NodeHelper.getInsnNodeFor(value));
    if (lastVarElementType.getDescriptor().startsWith("L")) {
      addInsn(new InsnNode(Opcodes.AASTORE));
    } else {
      addInsn(new InsnNode(toPrimitive(Type.getType(value.getClass())).getOpcode(Opcodes.IASTORE)));
    }
    return this;
  }
  
  private int getSort(final Type type) {
    final int sort = type.getSort();
    switch (sort) {
      case Type.INT:
        return Opcodes.T_INT;
      case Type.FLOAT:
        return Opcodes.T_FLOAT;
      case Type.LONG:
        return Opcodes.T_LONG;
      case Type.DOUBLE:
        return Opcodes.T_DOUBLE;
      case Type.SHORT:
        return Opcodes.T_SHORT;
      case Type.CHAR:
        return Opcodes.T_CHAR;
      case Type.BOOLEAN:
        return Opcodes.T_BOOLEAN;
      case Type.BYTE:
        return Opcodes.T_BYTE;
      default:
        return -1;
    }
  }
  
  /**
   * Adds the given node to the lastConstructor.
   * 
   * @param node
   *          the node
   * @return the class node builder
   */
  public ClassNodeBuilder addToConstructor(final AbstractInsnNode node) {
    if (isInterface) {
      return this;
    }
    final AbstractInsnNode returnNode = lastConstructor.instructions.getLast();
    if (returnNode == null) {
      lastConstructor.instructions.add(node);
    } else {
      lastConstructor.instructions.insertBefore(returnNode, node);
    }
    return this;
  }
  
  /**
   * Adds the given nodes to the lastConstructor.
   * 
   * @param nodes
   *          the nodes
   * @return the class node builder
   */
  public ClassNodeBuilder addToConstructor(final InsnList nodes) {
    if (isInterface) {
      return this;
    }
    final AbstractInsnNode returnNode = lastConstructor.instructions.getLast();
    if (returnNode == null) {
      lastConstructor.instructions.add(nodes);
    } else {
      lastConstructor.instructions.insertBefore(returnNode, nodes);
    }
    return this;
  }
  
  /**
   * adds a the given instruction to the method that was created last via {@link #addMethod(String, String)}.
   * 
   * @param node
   *          the node
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addInsn(final AbstractInsnNode node) {
    if (isInterface) {
      return this;
    }
    if ("<init>".equals(lastMethod.name)) {
      addToConstructor(node);
    } else {
      lastMethod.instructions.add(node);
    }
    return this;
  }
  
  /**
   * adds a the given instructions to the method that was created last via {@link #addMethod(String, String)}.
   * 
   * @param nodes
   *          the nodes
   * @return this ClassNodeBuilder
   */
  public ClassNodeBuilder addInsn(final InsnList nodes) {
    if (isInterface) {
      return this;
    }
    if (lastMethod == null) {
      addToConstructor(nodes);
    } else {
      lastMethod.instructions.add(nodes);
    }
    return this;
  }
  
  /**
   * adds a FieldInsnNode for the given Field.
   * 
   * @param field
   *          the field
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addGetClassField(final String field) {
    addInsn(new VarInsnNode(Opcodes.ALOAD, 0));
    return addGetField(this, field);
  }
  
  /**
   * adds a FieldInsnNode for the given Field.
   * 
   * @param builder
   *          the builder
   * @param field
   *          the field
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addGetField(final ClassNodeBuilder builder, final String field) {
    
    final FieldNode fieldNode = builder.getField(field);
    
    return addGetField(builder.getClassNode().name, fieldNode.name, fieldNode.desc);
  }
  
  /**
   * adds a FieldInsnNode for the given Field.
   * 
   * @param builder
   *          the builder
   * @param field
   *          the field
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addGetField(final String owner, final String field, final String desc) {
    
    final FieldInsnNode node = new FieldInsnNode(Opcodes.GETFIELD, owner, field, desc);
    return addInsn(node);
  }
  
  /**
   * adds a FieldInsnNode for the given Field.
   * 
   * @param field
   *          the field
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addPutClassField(final String field) {
    return addPutField(this, field);
  }
  
  /**
   * adds a FieldInsnNode for the given Field.
   * 
   * @param builder
   *          the builder
   * @param field
   *          the field
   * @return this ClassNodeBuilder
   */
  public ClassNodeBuilder addPutField(final ClassNodeBuilder builder, final String field) {
    final FieldNode fieldNode = builder.getField(field);
    return addPutField(builder.classNode.name, field, fieldNode.desc);
  }
  
  /**
   * Adds the put field.
   * 
   * @param owner
   *          the owner
   * @param name
   *          the name
   * @param desc
   *          the desc
   * @return the class node builder
   */
  public ClassNodeBuilder addPutField(final String owner, final String name, final String desc) {
    return addInsn(new FieldInsnNode(PUTFIELD, owner, name, desc));
  }
  
  /**
   * Load field array value by getting the field and than loading the value.
   * 
   * @param field
   *          the field
   * @param index
   *          the index
   * @return the class node builder
   */
  public ClassNodeBuilder loadFieldArrayValue(final String field, final int index) {
    addGetClassField(field);
    return addArrayLoad(field, index);
  }
  
  /**
   * Adds the array load for the specific index.
   * 
   * @param field
   *          the field
   * @param index
   *          the index
   * @return the class node builder
   */
  public ClassNodeBuilder addArrayLoad(final String field, final int index) {
    final FieldNode fieldNode = getField(field);
    addInsn(NodeHelper.getInsnNodeFor(index));
    Type elementType = Type.getType(fieldNode.desc).getElementType();
    if (fieldNode.desc.startsWith("[[")) {
      elementType = Type.getType(Object.class);
    }
    return addInsn(new InsnNode(elementType.getOpcode(Opcodes.IALOAD)));
  }
  
  /**
   * get the first MethodNode with the given Name.
   * 
   * @param name
   *          the name
   * @return the method
   */
  public MethodNode getMethod(final String name) {
    return getMethod(name, null);
  }
  
  /**
   * gets the lastConstructor with the given descriptor.
   * 
   * @param desc
   *          the desc
   * @return the constructor
   */
  public MethodNode getConstructor(final String desc) {
    return getMethod("<init>", desc);
  }
  
  /**
   * get the MethodNode with the given Name and descriptor.
   * 
   * @param name
   *          the name
   * @param desc
   *          the methoddescriptor
   * @return the method
   */
  public MethodNode getMethod(final String name, final String desc) {
    for (final MethodNode method : classNode.methods) {
      if (name.equals(method.name)) {
        if (StringUtils.isBlank(desc)) {
          return method;
        }
        if (method.desc.equals(desc)) {
          return method;
        }
      }
    }
    return null;
  }
  
  /**
   * get the FieldNode with the given name.
   * 
   * @param name
   *          the name
   * @return the field
   */
  public FieldNode getField(final String name) {
    if (isInterface) {
      return null;
    }
    for (final FieldNode field : classNode.fields) {
      if (name.equals(field.name)) {
        return field;
      }
    }
    return null;
  }
  
  /**
   * Create the ClassObject.
   * 
   * @return the abstract optimizer test
   * @throws Exception
   *           the exception
   */
  public ClassNodeBuilder toClass() throws Exception {
    final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    classNode.accept(writer);
    final ClassDescriptor descriptor = new ClassDescriptor(classNode.name.replace("/", "."), writer.toByteArray(),
        "NoFile");
    ClassAccessor.store(descriptor);
    buildedClass = ClassAccessor.getClassloader().loadClass(descriptor.getName());
    return this;
  }
  
  /**
   * Gets the builded class.
   * 
   * @return the builded class
   */
  public Class<?> getBuildedClass() throws Exception {
    if (buildedClass == null) {
      toClass();
    }
    return buildedClass;
  }
  
  /**
   * Instantiate the classObject.
   * 
   * @param params
   *          the params
   * @return the object
   * @throws Exception
   *           the exception
   */
  public Object instance(final Object... params) throws Exception {
    if (buildedClass == null) {
      toClass();
    }
    return ConstructorUtils.invokeConstructor(buildedClass, params);
  }
  
  /**
   * Gets the class node.
   * 
   * @return the class node
   */
  public ClassNode getClassNode() {
    return classNode;
  }
  
  /**
   * Select the Method as being "active".
   * 
   * @param name
   *          the name
   * @param desc
   *          the desc
   * @return the class node builder
   */
  public ClassNodeBuilder selectMethod(final String name, final String desc) {
    setLastMethod(getMethod(name, desc));
    return this;
  }
  
  /**
   * Select the given lastConstructor as being "active".
   * 
   * @param desc
   *          the desc
   * @return the class node builder
   */
  public ClassNodeBuilder selectConstructor(final String desc) {
    setLastMethod(getMethod("<init>", desc));
    return this;
  }
  
  private void setLastMethod(final MethodNode method) {
    if ("<init>".equals(method.name)) {
      lastConstructor = method;
      lastMethod = method;
    } else {
      lastMethod = method;
    }
    lastElement = method;
  }
  
  /**
   * Loads the given method-local variable.
   * 
   * @param index
   *          the index
   * @return the class node builder
   */
  public ClassNodeBuilder load(final int index) {
    if (index == 0 && (lastMethod.access & ACC_STATIC) == 0) {
      addInsn(new VarInsnNode(ALOAD, 0));
      return this;
    }
    addInsn(new VarInsnNode(getType(index).getOpcode(ILOAD), getVarIndex(index)));
    return this;
  }
  
  /**
   * Load the variable index of given type.
   * 
   * @param type
   *          the type
   * @param index
   *          the index
   * @return the class node builder
   */
  public ClassNodeBuilder load(final Type type, final int index) {
    addInsn(new VarInsnNode(type.getOpcode(ILOAD), index));
    return this;
  }
  
  /**
   * Stores the given method-local variable.
   * 
   * @param index
   *          the index
   * @return the class node builder
   */
  public ClassNodeBuilder store(final int index) {
    final Type type = getType(index);
    return store(type, index);
  }
  
  /**
   * Stores the given variable index of type type.
   * 
   * @param type
   *          the type
   * @param index
   *          the index
   * @return the class node builder
   */
  public ClassNodeBuilder store(final Type type, final int index) {
    addInsn(new VarInsnNode(type.getOpcode(ISTORE), getVarIndex(index)));
    return this;
  }
  
  /**
   * Adds the return statement.
   * 
   * @return the class node builder
   */
  public ClassNodeBuilder addReturn() {
    addInsn(new InsnNode(Type.getReturnType(lastMethod.desc).getOpcode(IRETURN)));
    return this;
  }
  
  /**
   * Adds the given operation.
   * 
   * @param opcode
   *          the opcode
   * @param args
   *          the args
   * @return the class node builder
   */
  public ClassNodeBuilder add(final int opcode, final Object... args) {
    if (opcode == IINC) {
      return addInsn(new IincInsnNode((Integer) args[0], (Integer) args[1]));
    }
    if (opcode >= NOP && opcode <= DCONST_1 || opcode >= POP && opcode <= DCMPG || opcode >= IALOAD && opcode <= SALOAD
        || opcode >= IASTORE && opcode <= SASTORE || opcode == ARRAYLENGTH || opcode == ATHROW) {
      return addInsn(new InsnNode(opcode));
    }
    if (opcode >= BIPUSH && opcode <= SIPUSH || opcode == NEWARRAY) {
      return addInsn(new IntInsnNode(opcode, (Integer) args[0]));
    }
    if (opcode == LDC) {
      return loadConstant(args[0]);
    }
    if (opcode >= ILOAD && opcode <= ALOAD) {
      return addInsn(new VarInsnNode(opcode, (Integer) args[0]));
    }
    if (opcode >= ISTORE && opcode <= ASTORE) {
      return addInsn(new VarInsnNode(opcode, (Integer) args[0]));
    }
    if (opcode >= IFEQ && opcode <= JSR) {
      return jump(opcode, (LabelNode) args[0]);
    }
    return this;
  }
  
  /**
   * Load constant.
   * 
   * @param arg
   *          the arg
   * @return the class node builder
   */
  public ClassNodeBuilder loadConstant(final Object arg) {
    return addInsn(new LdcInsnNode(arg));
  }
  
  /**
   * Jump.
   * 
   * @param opcode
   *          the opcode
   * @param labelNode
   *          the label node
   * @return the class node builder
   */
  public ClassNodeBuilder jump(final int opcode, final LabelNode labelNode) {
    return addInsn(new JumpInsnNode(opcode, labelNode));
  }
  
  private int getVarIndex(final int index) {
    int varIndex = shiftStaticIndex(index);
    
    final Type[] parameterTypes = Type.getArgumentTypes(lastMethod.desc);
    if (parameterTypes.length <= index) {
      return index;
    }
    for (int i = 0; i < index; ++i) {
      final Type pType = parameterTypes[i];
      varIndex += pType.getSize();
    }
    
    return varIndex;
  }
  
  private int shiftStaticIndex(final int index) {
    int varIndex = index;
    if ((lastMethod.access & ACC_STATIC) == 0) {
      varIndex--;
    }
    return varIndex;
  }
  
  private Type getType(final int index) {
    final int varIndex = shiftStaticIndex(index);
    final Type[] parameterTypes = Type.getMethodType(lastMethod.desc).getArgumentTypes();
    if (parameterTypes.length <= varIndex) {
      return Type.INT_TYPE;
    }
    final Type type = parameterTypes[varIndex];
    return type;
  }
  
  /**
   * Invoke.
   * 
   * @param opcode
   *          the opcode
   * @param method
   *          the method
   * @param args
   *          the args
   * @return the class node builder
   */
  public ClassNodeBuilder invoke(final int opcode, final String method, final Number... args) {
    add(ALOAD, 0);
    return invoke(opcode, this, method, args);
  }
  
  /**
   * Invoke.
   * 
   * @param opcode
   *          the opcode
   * @param classBuilder
   *          the class builder
   * @param method
   *          the method
   * @param args
   *          the args
   * @return the class node builder
   */
  public ClassNodeBuilder invoke(final int opcode, final ClassNodeBuilder classBuilder, final String method,
      final Number... args) {
    return invoke(opcode, classBuilder.classNode.name, method, classBuilder.getMethod(method).desc, args);
  }
  
  /**
   * Invoke.
   * 
   * @param opcode
   *          the opcode
   * @param owner
   *          the owner
   * @param method
   *          the method
   * @param desc
   *          the desc
   * @param args
   *          the args
   * @return the class node builder
   */
  public ClassNodeBuilder invoke(final int opcode, final String owner, final String method, final String desc,
      final Number... args) {
    if (args != null) {
      for (final Object arg : args) {
        add(((Number) arg).intValue());
      }
    }
    return addInsn(new MethodInsnNode(opcode, owner, method, desc));
  }
  
  /**
   * Gets the desc.
   * 
   * @return the desc
   */
  public String getDesc() {
    return classNode.name;
  }
  
  /**
   * Implement interface.
   * 
   * @param builder
   *          the builder
   * @return the class node builder
   */
  public ClassNodeBuilder implementInterface(final ClassNodeBuilder builder) {
    return implementInterface(builder.getDesc());
  }
  
  /**
   * Implement interface.
   * 
   * @param desc
   *          the desc
   * @return the class node builder
   */
  public ClassNodeBuilder implementInterface(final String desc) {
    classNode.interfaces.add(desc);
    return this;
  }
  
  /**
   * With signature.
   * 
   * @param signature
   *          the signature
   * @return the class node builder
   */
  public ClassNodeBuilder withSignature(final String signature) {
    classNode.signature = signature;
    return this;
  }
  
  /**
   * Removes the method from this class.
   */
  public ClassNodeBuilder removeMethod(final String name, final String desc) {
    final MethodNode method = getMethod(name, desc);
    classNode.methods.remove(method);
    return this;
  }
}
