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

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
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
  private ClassNode classNode;
  
  private MethodNode lastMethod;
  private FieldNode lastField;
  
  private Object lastElement;
  
  /** The constructor. */
  private MethodNode constructor;
  private Class<?> buildedClass;
  
  private int constructorVarIndex = 1;
  
  private int lastConstructorVarIndex;
  
  private int methodVarIndex = 1;
  
  private int lastMethodVarIndex;
  
  private Type lastVarElementType;
  
  private ClassNodeBuilder() {
    //
  }
  
  /**
   * creates a new ClassNode with the given name and a default constructor.
   * 
   * @param className
   *          the class name
   * @return the abstract optimizer test
   */
  public static ClassNodeBuilder createClass(final String className) {
    final ClassNodeBuilder builder = new ClassNodeBuilder();
    builder.classNode = new ClassNode(Opcodes.ASM4);
    builder.classNode.access = ACC_PUBLIC;
    builder.classNode.name = className.replace(".", "/");
    builder.classNode.superName = Type.getInternalName(Object.class);
    builder.classNode.version = Opcodes.V1_7;
    builder.addMethod("<init>", "()V").addInsn(new VarInsnNode(Opcodes.ALOAD, 0))
        .addInsn(new MethodInsnNode(Opcodes.INVOKESPECIAL, builder.classNode.superName, "<init>", "()V"))
        .addInsn(new InsnNode(Opcodes.RETURN));
    builder.constructor = builder.lastMethod;
    return builder;
  }
  
  /**
   * Creates a getter for the last added field.
   * 
   * @return the class node builder
   */
  public ClassNodeBuilder withGetter() {
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
    addMethod(methodName, "()V");
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
    lastMethod = method;
    lastElement = method;
    return this;
  }
  
  private int getEffectiveModifier(final int... modifiers) {
    int modifier = 0;
    if ((modifiers == null) || (modifiers.length == 0)) {
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
    if ((values != null) && (values.length > 0)) {
      annotationNode.values = Arrays.asList(values);
    }
    if (lastElement instanceof MethodNode) {
      lastMethod.visibleAnnotations.add(annotationNode);
    } else if (lastElement instanceof FieldNode) {
      lastField.visibleAnnotations.add(annotationNode);
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
    } else {
      lastField.access = effectiveModifier;
    }
    return this;
  }
  
  /**
   * Initializes a classField in the default Constructor
   * (eg: if fieldType of field "field" is "TestObject", field = new TestObject() is called).
   * 
   * Numbers and Strings are used to assign "field", every other value leads to new Object.
   * 
   * @param object
   *          the object to Use.
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder initWith(final Object object) {
    final AbstractInsnNode returnNode = constructor.instructions.getLast();
    if ((object instanceof String) || (object instanceof Number) || (object instanceof Boolean)
        || (object instanceof Character)) {
      constructor.instructions.insertBefore(returnNode, new VarInsnNode(Opcodes.ALOAD, 0));
      final AbstractInsnNode numberNode = NodeHelper.getInsnNodeFor(object);
      constructor.instructions.insertBefore(returnNode, numberNode);
      if (lastField.desc.startsWith("L") && (object instanceof Number)) {
        constructor.instructions.insertBefore(returnNode, ConstructorBuilder.getBoxingNode(lastField));
      }
      constructor.instructions.insertBefore(returnNode, new FieldInsnNode(Opcodes.PUTFIELD, classNode.name,
          lastField.name, lastField.desc));
    } else {
      constructor.instructions.insertBefore(returnNode, new VarInsnNode(ALOAD, 0));
      constructor.instructions.insertBefore(returnNode, newObject(lastField.desc));
      constructor.instructions.insertBefore(returnNode, new FieldInsnNode(Opcodes.PUTFIELD, classNode.name,
          lastField.name, lastField.desc));
      
    }
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
    list.add(new MethodInsnNode(INVOKESPECIAL, fieldDesc, "<init>", "()V"));
    return list;
  }
  
  /**
   * Creates Instruction to instantiate a new Object of type "desc".
   * 
   * @param desc
   *          the desc
   * @return the insn list
   */
  public ClassNodeBuilder addNewObject(final String desc) {
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
    final Type elementType = Type.getType(lastField.desc).getElementType();
    if (elementType.getDescriptor().startsWith("L")) {
      initArrayInternal(Opcodes.AASTORE, (Object[]) values);
      return this;
    }
    final Type type = toPrimitive(elementType);
    initArrayInternal(type.getOpcode(Opcodes.IASTORE), (Object[]) values);
    return this;
  }
  
  public ClassNodeBuilder initMultiArrayWith(final Object value, final int... indexes) {
    final AbstractInsnNode returnNode = constructor.instructions.getLast();
    constructor.instructions.insertBefore(returnNode, new VarInsnNode(Opcodes.ALOAD, 0));
    constructor.instructions.insertBefore(returnNode, new FieldInsnNode(Opcodes.GETFIELD, classNode.name,
        lastField.name, lastField.desc));
    for (int i = 0; i < (indexes.length - 1); ++i) {
      constructor.instructions.insertBefore(returnNode, NodeHelper.getInsnNodeFor(indexes[i]));
      constructor.instructions.insertBefore(returnNode, new InsnNode(Opcodes.AALOAD));
    }
    constructor.instructions.insertBefore(returnNode, NodeHelper.getInsnNodeFor(indexes[indexes.length - 1]));
    constructor.instructions.insertBefore(returnNode, NodeHelper.getInsnNodeFor(value));
    final Type elementType = Type.getType(lastField.desc).getElementType();
    if (elementType.getDescriptor().startsWith("L")) {
      constructor.instructions.insertBefore(returnNode, new InsnNode(Opcodes.AASTORE));
    } else {
      constructor.instructions.insertBefore(returnNode, new InsnNode(toPrimitive(Type.getType(value.getClass()))
          .getOpcode(Opcodes.IASTORE)));
      
    }
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
   * Initializes a classField of type Array in the default Constructor.
   * The array is initialized with the given Strings.
   * 
   * @param values
   *          the values
   * @return the abstract optimizer test
   * @see AbstractOptimizerTest#initArrayInConstructor(String, int)
   */
  public ClassNodeBuilder initArrayWith(final String... values) {
    initArrayInternal(Opcodes.AASTORE, (Object[]) values);
    return this;
  }
  
  private void initArrayInternal(final int opcode, final Object... values) {
    final AbstractInsnNode returnNode = constructor.instructions.getLast();
    final int length = values.length;
    initArray(length);
    int index = 0;
    for (final Object number : values) {
      constructor.instructions.insertBefore(returnNode, new VarInsnNode(Opcodes.ALOAD, lastConstructorVarIndex));
      final AbstractInsnNode indexNode = NodeHelper.getInsnNodeFor(index++);
      constructor.instructions.insertBefore(returnNode, indexNode);
      final AbstractInsnNode numberNode = NodeHelper.getInsnNodeFor(number);
      constructor.instructions.insertBefore(returnNode, numberNode);
      if ((number instanceof Number) && (opcode == Opcodes.ASTORE)) {
        constructor.instructions.insertBefore(returnNode, ConstructorBuilder.getBoxingNode(lastField));
      }
      constructor.instructions.insertBefore(returnNode, new InsnNode(opcode));
    }
  }
  
  /**
   * Initializes a classField of type Array in the default Constructor with the given length.
   * (eg: if fieldType of field "field" is "double[]", field = new double[length] is called).
   * 
   * @param length
   *          the length
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder initArray(final int... length) {
    final AbstractInsnNode returnNode = constructor.instructions.getLast();
    constructor.instructions.insertBefore(returnNode, new VarInsnNode(Opcodes.ALOAD, 0));
    final AbstractInsnNode node;
    if (length.length == 1) {
      final Type elementType = Type.getType(lastField.desc).getElementType();
      constructor.instructions.insertBefore(returnNode, NodeHelper.getInsnNodeFor(Integer.valueOf(length[0])));
      if (elementType.getDescriptor().startsWith("L")) {
        node = new TypeInsnNode(Opcodes.ANEWARRAY, elementType.getInternalName());
      } else {
        node = new IntInsnNode(Opcodes.NEWARRAY, getSort(elementType));
      }
    } else {
      for (final int currentLength : length) {
        constructor.instructions.insertBefore(returnNode, NodeHelper.getInsnNodeFor(Integer.valueOf(currentLength)));
      }
      node = new MultiANewArrayInsnNode(lastField.desc, length.length);
    }
    constructor.instructions.insertBefore(returnNode, node);
    constructor.instructions.insertBefore(returnNode, new VarInsnNode(Opcodes.ASTORE, constructorVarIndex));
    constructor.instructions.insertBefore(returnNode, new VarInsnNode(Opcodes.ALOAD, constructorVarIndex));
    lastConstructorVarIndex = constructorVarIndex;
    constructorVarIndex++;
    constructor.instructions.insertBefore(returnNode, new FieldInsnNode(Opcodes.PUTFIELD, classNode.name,
        lastField.name, lastField.desc));
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
    addInsn(new VarInsnNode(Opcodes.ALOAD, lastMethodVarIndex));
    for (int i = 0; i < (indexes.length - 1); ++i) {
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
   * Adds the given node to the constructor.
   * 
   * @param node
   *          the node
   * @return the class node builder
   */
  public ClassNodeBuilder addToConstructor(final AbstractInsnNode node) {
    final AbstractInsnNode returnNode = constructor.instructions.getLast();
    constructor.instructions.insertBefore(returnNode, node);
    return this;
  }
  
  /**
   * Adds the given nodes to the constructor.
   * 
   * @param node
   *          the node
   * @return the class node builder
   */
  public ClassNodeBuilder addToConstructor(final InsnList nodes) {
    final AbstractInsnNode returnNode = constructor.instructions.getLast();
    constructor.instructions.insertBefore(returnNode, nodes);
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
    lastMethod.instructions.add(node);
    return this;
  }
  
  /**
   * adds a the given instructions to the method that was created last via {@link #addMethod(String, String)}.
   * 
   * @param node
   *          the node
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addInsn(final InsnList nodes) {
    lastMethod.instructions.add(nodes);
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
   * @param field
   *          the field
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addGetField(final ClassNodeBuilder builder, final String field) {
    
    final FieldNode fieldNode = builder.getField(field);
    
    final FieldInsnNode node = new FieldInsnNode(Opcodes.GETFIELD, builder.getClassNode().name, fieldNode.name,
        fieldNode.desc);
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
    final FieldNode fieldNode = getField(field);
    final FieldInsnNode node = new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, field, fieldNode.desc);
    return addInsn(node);
  }
  
  /**
   * adds a FieldInsnNode for the given Field.
   * 
   * @param field
   *          the field
   * @return the abstract optimizer test
   */
  public ClassNodeBuilder addPutField(final ClassNodeBuilder builder, final String field) {
    final FieldNode fieldNode = builder.getField(field);
    final FieldInsnNode node = new FieldInsnNode(Opcodes.PUTFIELD, builder.classNode.name, field, fieldNode.desc);
    return addInsn(node);
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
    return addInsn(new InsnNode(Type.getType(fieldNode.desc).getElementType().getOpcode(Opcodes.IALOAD)));
  }
  
  /**
   * Inits the real class.
   * 
   * @param className
   *          the class name
   * @param methodName
   *          the method name
   * @return the class node builder
   * @throws Exception
   *           the exception
   */
  public static ClassNodeBuilder initRealClass(final String className, final String methodName) throws Exception {
    final ClassNodeBuilder builder = new ClassNodeBuilder();
    builder.initClassNode(className);
    builder.initMethodNode(methodName);
    return builder;
  }
  
  private void initClassNode(final String className) throws Exception {
    classNode = new ClassNode(Opcodes.ASM4);
    new ClassReader(className).accept(classNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
  }
  
  private void initMethodNode(final String methodName) {
    lastMethod = getMethod(methodName);
  }
  
  /**
   * get the MethodNode with the given Name.
   * 
   * @param name
   *          the name
   * @return the method
   */
  public MethodNode getMethod(final String name) {
    for (final MethodNode method : classNode.methods) {
      if (name.equals(method.name)) {
        return method;
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
  public Class<?> getBuildedClass() {
    return buildedClass;
  }
  
  /**
   * Instantiate the classObject.
   * 
   * @return the object
   * @throws Exception
   *           the exception
   */
  public Object instance() throws Exception {
    if (buildedClass == null) {
      toClass();
    }
    return ConstructorUtils.invokeConstructor(buildedClass);
  }
  
  /**
   * Gets the class node.
   * 
   * @return the class node
   */
  public ClassNode getClassNode() {
    return classNode;
  }
  
}
