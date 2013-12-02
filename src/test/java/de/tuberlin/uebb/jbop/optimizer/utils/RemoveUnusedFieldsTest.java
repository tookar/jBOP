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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JBOP. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tuberlin.uebb.jbop.optimizer.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;

/**
 * Tests for the Class {@link RemoveUnusedFields}.
 * 
 * @author Christopher Ewest
 */
public class RemoveUnusedFieldsTest {
  
  private ClassNodeBuilder classBuilder;
  private ClassNode classNode;
  
  /**
   * Test the removal of all constructors and unused fields.
   */
  @Test
  public void test() {
    // INIT
    final ClassNodeBuilder otherClass = ClassNodeBuilder
        .createClass("de.tuberlin.uebb.jbop.optimizer.utils.OtherClass").//
        addField("field4", "D").withModifiers(ACC_PUBLIC);
    classBuilder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.TestClass").//
        addField("field1", "D", 1.0).withGetter().//
        addField("field2", "D").withSetter().//
        addField("field3", "D").withGetterAndSetter().//
        addField("field4", "D", 1.0).//
        addField("field5", "D", 1.0).withModifiers(ACC_PUBLIC).//
        addField("field6", "D", 1.0).withModifiers(ACC_PRIVATE, ACC_SYNTHETIC).//
        addField("field7", "D").initWith(2.0).//
        addMethod("otherField", "D").//
        addNewObject(otherClass).addGetField(otherClass, "field4").addReturn();
    classNode = classBuilder.getClassNode();
    
    // RUN
    assertEquals(6, classNode.methods.size());
    assertEquals(7, classNode.fields.size());
    RemoveUnusedFields.removeUnusedFields(classNode);
    
    // ASSERT
    assertEquals(5, classNode.fields.size());
    assertEquals("field1", classNode.fields.get(0).name);
    assertEquals("field2", classNode.fields.get(1).name);
    assertEquals("field3", classNode.fields.get(2).name);
    assertEquals("field5", classNode.fields.get(3).name);
    assertEquals("field6", classNode.fields.get(4).name);
    assertNull(classBuilder.getMethod("<init>"));
  }
  
}
