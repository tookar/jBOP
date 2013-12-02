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
package de.tuberlin.uebb.jbop.exception;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test creation of {@link JBOPClassException}.
 * 
 * @author Christopher Ewest
 */
@RunWith(MockitoJUnitRunner.class)
public class JBOPClassExceptionTest {
  
  @Mock
  private Throwable cause;
  
  private final String message = "Message";
  
  /**
   * Test creation of {@link JBOPClassException}.
   */
  @Test
  public void testJBOPClassException() {
    final JBOPClassException jBOPClassException = new JBOPClassException(message, cause);
    try {
      throw jBOPClassException;
    } catch (final Throwable jce) {
      assertEquals(jce.getClass().getSimpleName() + ": " + message, ExceptionUtils.getMessage(jce));
      assertEquals(cause, ExceptionUtils.getRootCause(jce));
    }
  }
  
}
