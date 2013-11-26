package de.tuberlin.uebb.jbop.optimizer.utils;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

public class Loop {
  
  private final AbstractInsnNode ifNode;
  private final AbstractInsnNode startValue;
  private final AbstractInsnNode endValue;
  private final AbstractInsnNode iinc;
  private final AbstractInsnNode firstOfBody;
  private final AbstractInsnNode endOfLoop;
  private final AbstractInsnNode counter;
  
  Loop(final AbstractInsnNode ifNode, final AbstractInsnNode startValue, final AbstractInsnNode endValue,
      final AbstractInsnNode iinc, final AbstractInsnNode firstOfBody, final AbstractInsnNode endOfLoop,
      final AbstractInsnNode counter) {
    super();
    this.ifNode = ifNode;
    this.startValue = startValue;
    this.endValue = endValue;
    this.iinc = iinc;
    this.firstOfBody = firstOfBody;
    this.endOfLoop = endOfLoop;
    this.counter = counter;
  }
  
  protected AbstractInsnNode getIfNode() {
    return ifNode;
  }
  
  protected AbstractInsnNode getStartValue() {
    return startValue;
  }
  
  protected AbstractInsnNode getEndValue() {
    return endValue;
  }
  
  protected boolean isPlain() {
    return NodeHelper.isNumberNode(getStartValue()) && NodeHelper.isNumberNode(getEndValue());
  }
  
  protected List<AbstractInsnNode> getBody() {
    final List<AbstractInsnNode> body = new ArrayList<>();
    AbstractInsnNode currentNode = firstOfBody;
    final AbstractInsnNode endNode = getIInc();
    while (currentNode != endNode) {
      body.add(currentNode);
      currentNode = currentNode.getNext();
    }
    return body;
  }
  
  protected AbstractInsnNode getIInc() {
    return iinc;
  }
  
  public AbstractInsnNode getEndOfLoop() {
    return endOfLoop;
  }
  
  public AbstractInsnNode getCounter() {
    return counter;
  }
  
}
