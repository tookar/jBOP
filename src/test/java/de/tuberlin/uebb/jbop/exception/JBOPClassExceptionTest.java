package de.tuberlin.uebb.jbop.exception;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test creation of {@link JBOPClassException}.
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
