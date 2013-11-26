package de.tuberlin.uebb.jbop.optimizer.utils;

import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.ISTORE;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.loop.ForLoop;
import de.tuberlin.uebb.jbop.optimizer.loop.ForLoopBody;
import de.tuberlin.uebb.jbop.optimizer.loop.ForLoopFooter;

/**
 * The Class LoopMatcher.
 * 
 * @author Christopher Ewest
 */
public final class LoopMatcher {
  
  private LoopMatcher() {
    //
  }
  
  public static Loop getLoop(final AbstractInsnNode node) {
    if (node == null) {
      return null;
    }
    
    final int opcode = node.getOpcode();
    if (opcode != ISTORE) {
      return null;
    }
    
    final AbstractInsnNode next = node.getNext();
    
    if (NodeHelper.isGoto(next)) {
      return getTypeOneLoop(node, next);
    }
    return getTypeTwoLoop(node, next);
  }
  
  public static ForLoop toForLoop(final Loop loop) {
    if (loop == null) {
      return null;
    }
    
    if (!loop.isPlain()) {
      return null;
    }
    
    final VarInsnNode varNode = (VarInsnNode) loop.getCounter();
    final AbstractInsnNode endValue = loop.getEndValue();
    final JumpInsnNode ifNode = (JumpInsnNode) loop.getIfNode();
    final IincInsnNode iInc = (IincInsnNode) loop.getIInc();
    final ForLoopFooter footer = new ForLoopFooter(varNode, endValue, ifNode, iInc);
    
    final ForLoopBody body = new ForLoopBody(loop.getBody());
    final Number start = NodeHelper.getNumberValue(loop.getStartValue());
    
    final ForLoop forLoop = new ForLoop(body, footer, start);
    
    return forLoop;
    
  }
  
  private static JumpInsnNode reverse(final JumpInsnNode ifNode) {
    switch (ifNode.getOpcode()) {
      case IF_ICMPEQ:
        return new JumpInsnNode(IF_ICMPNE, ifNode.label);
      case IF_ICMPGE:
        return new JumpInsnNode(IF_ICMPLT, ifNode.label);
      case IF_ICMPGT:
        return new JumpInsnNode(IF_ICMPLE, ifNode.label);
      case IF_ICMPLE:
        return new JumpInsnNode(IF_ICMPGT, ifNode.label);
      case IF_ICMPLT:
        return new JumpInsnNode(IF_ICMPGE, ifNode.label);
      case IF_ICMPNE:
        return new JumpInsnNode(IF_ICMPEQ, ifNode.label);
      default:
        return null;
    }
  }
  
  private static AbstractInsnNode getStoreOfLoopForIf(final AbstractInsnNode node) {
    if (node == null) {
      return null;
    }
    
    if (!NodeHelper.isIf(node)) {
      return null;
    }
    final AbstractInsnNode previous = ((JumpInsnNode) node).label.getPrevious();
    if (!NodeHelper.isGoto(previous)) {
      return null;
    }
    
    final AbstractInsnNode previous2 = previous.getPrevious();
    if (previous2 instanceof IincInsnNode) {
      return ((JumpInsnNode) previous).label.getPrevious();
    }
    return previous2;
  }
  
  public static boolean isIfOfLoop(final AbstractInsnNode node) {
    return isStoreOfLoop(getStoreOfLoopForIf(node));
  }
  
  private static AbstractInsnNode getStoreOfLoopForIInc(final AbstractInsnNode node) {
    if (node == null) {
      return null;
    }
    
    if (!(node instanceof IincInsnNode)) {
      return null;
    }
    final AbstractInsnNode next = node.getNext();
    if (NodeHelper.isGoto(next)) {
      return ((JumpInsnNode) next).label.getPrevious();
    }
    if (next instanceof LabelNode) {
      final AbstractInsnNode gotoNode = findGotoForLabel(next, true);
      if (gotoNode == null) {
        return null;
      }
      return gotoNode.getPrevious();
    }
    
    return null;
  }
  
  public static boolean isGotoOfLoop(final AbstractInsnNode node) {
    return isStoreOfLoop(getStoreOfLoopForGoto(node));
  }
  
  private static AbstractInsnNode getStoreOfLoopForGoto(final AbstractInsnNode node) {
    if (node == null) {
      return null;
    }
    
    if (!NodeHelper.isGoto(node)) {
      return null;
    }
    if (node.getPrevious() instanceof IincInsnNode) {
      return ((JumpInsnNode) node).label.getPrevious();
    }
    return node.getPrevious();
  }
  
  public static boolean isIIncOfLoop(final AbstractInsnNode node) {
    return isStoreOfLoop(getStoreOfLoopForIInc(node));
  }
  
  public static boolean isStoreOfLoop(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    
    final int opcode = node.getOpcode();
    if (opcode != ISTORE) {
      return false;
    }
    
    final AbstractInsnNode next = node.getNext();
    if (NodeHelper.isGoto(next)) {
      return getTypeOneLoop(node, next) != null;
    }
    if (next instanceof LabelNode) {
      return getTypeTwoLoop(node, next) != null;
    }
    return false;
  }
  
  private static Loop getTypeOneLoop(final AbstractInsnNode counter, final AbstractInsnNode jump) {
    final LabelNode labelOfJump = ((JumpInsnNode) jump).label;
    final AbstractInsnNode target = labelOfJump.getNext();
    final AbstractInsnNode label = jump.getNext();
    if (!(label instanceof LabelNode)) {
      return null;
    }
    
    final AbstractInsnNode ifNode = findIf(target, label);
    if (ifNode == null) {
      return null;
    }
    
    AbstractInsnNode previous = NodeHelper.getPrevious(ifNode);
    AbstractInsnNode endValue = previous.getPrevious();
    
    final int varIndex = NodeHelper.getVarIndex(counter);
    int varIndex2 = NodeHelper.getVarIndex(previous);
    if (varIndex2 == -1) {
      previous = target;
      endValue = previous.getNext();
      varIndex2 = NodeHelper.getVarIndex(counter);
      
    }
    if (varIndex != varIndex2) {
      return null;
    }
    
    final AbstractInsnNode iinc = labelOfJump.getPrevious();
    if (endValue instanceof LabelNode) {
      endValue = NodeHelper.getInsnNodeFor(0);
    }
    final AbstractInsnNode startValue = counter.getPrevious();
    
    final AbstractInsnNode firstOfBody = ((JumpInsnNode) ifNode).label.getNext();
    return new Loop(ifNode, startValue, endValue, iinc, firstOfBody, ifNode, counter);
  }
  
  public static AbstractInsnNode findIf(final AbstractInsnNode target, final AbstractInsnNode desiredLabel) {
    if (target == null) {
      return null;
    }
    AbstractInsnNode maybeIf = target.getNext();
    while (maybeIf != null) {
      if (NodeHelper.isIf(maybeIf)) {
        if (((JumpInsnNode) maybeIf).label == desiredLabel) {
          return maybeIf;
        }
      }
      maybeIf = maybeIf.getNext();
    }
    
    return null;
  }
  
  private static AbstractInsnNode findGotoForLabel(final AbstractInsnNode label, final boolean up) {
    AbstractInsnNode currentNode;
    if (up) {
      currentNode = label.getPrevious();
    } else {
      currentNode = label.getNext();
    }
    
    while (currentNode != null) {
      
      if (NodeHelper.isGoto(currentNode) && (((JumpInsnNode) currentNode).label == label)) {
        return currentNode;
      }
      
      if (up) {
        currentNode = currentNode.getPrevious();
      } else {
        currentNode = currentNode.getNext();
      }
      
    }
    
    return null;
  }
  
  private static Loop getTypeTwoLoop(final AbstractInsnNode counter, final AbstractInsnNode label) {
    final AbstractInsnNode gotoNode = findGotoForLabel(label, false);
    if (gotoNode == null) {
      return null;
    }
    
    final AbstractInsnNode ifNode = findIf(counter.getNext(), gotoNode.getNext());
    if (ifNode == null) {
      return null;
    }
    
    AbstractInsnNode previous = ifNode.getPrevious();
    AbstractInsnNode endValue = previous.getPrevious();
    int varIndex2 = NodeHelper.getVarIndex(previous);
    if (varIndex2 == -1) {
      previous = label.getNext();
      varIndex2 = NodeHelper.getVarIndex(previous);
      endValue = previous.getNext();
    }
    final int varIndex = NodeHelper.getVarIndex(counter);
    if (varIndex != varIndex2) {
      return null;
    }
    
    final AbstractInsnNode startValue = counter.getPrevious();
    if (endValue instanceof LabelNode) {
      endValue = NodeHelper.getInsnNodeFor(0);
    }
    
    final AbstractInsnNode iinc = gotoNode.getPrevious();
    final AbstractInsnNode firstOfBody = ((JumpInsnNode) ifNode).getNext();
    return new Loop(reverse((JumpInsnNode) ifNode), startValue, endValue, iinc, firstOfBody, gotoNode.getNext(),
        counter);
  }
  
  public static Pair<AbstractInsnNode, AbstractInsnNode> getLoopBounds(final AbstractInsnNode currentNode) {
    final AbstractInsnNode storeOfLoop;
    if (NodeHelper.isGoto(currentNode)) {
      storeOfLoop = getStoreOfLoopForGoto(currentNode);
    } else if (NodeHelper.isIf(currentNode)) {
      storeOfLoop = getStoreOfLoopForIf(currentNode);
    } else if (currentNode instanceof IincInsnNode) {
      storeOfLoop = getStoreOfLoopForIInc(currentNode);
    } else {
      storeOfLoop = currentNode;
    }
    final Loop loop = getLoop(storeOfLoop);
    if (loop == null) {
      return null;
    }
    return Pair.of(loop.getCounter().getPrevious(), loop.getEndOfLoop());
    
  }
  
  public static AbstractInsnNode getEndOfLoop(final AbstractInsnNode currentNode) {
    return getLoopBounds(currentNode).getRight();
  }
  
  public static AbstractInsnNode getStartOfLoop(final AbstractInsnNode currentNode) {
    return getLoopBounds(currentNode).getLeft();
  }
  
}
