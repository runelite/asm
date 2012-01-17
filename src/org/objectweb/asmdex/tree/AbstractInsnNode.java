/* Software Name : AsmDex
 * Version : 1.0
 *
 * Copyright © 2012 France Télécom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.objectweb.asmdex.tree;

import java.util.List;
import java.util.Map;

import org.objectweb.asmdex.MethodVisitor;

/**
 * A node that represents a bytecode instruction. <i>An instruction can appear
 * at most once in at most one {@link InsnList} at a time</i>.
 * 
 * @author Eric Bruneton
 * @author Julien Névo (constant changes to adapt the class to AsmDex)
 */
public abstract class AbstractInsnNode {

    /**
     * The type of {@link InsnNode} instructions.
     */
    public static final int INSN = 0;

    /**
     * The type of {@link IntInsnNode} instructions.
     */
    public static final int INT_INSN = 1;

    /**
     * The type of {@link VarInsnNode} instructions.
     */
    public static final int VAR_INSN = 2;

    /**
     * The type of {@link VarInsnLongNode} instructions.
     */
    public static final int VAR_INSN_LONG = 3;
    
    /**
     * The type of {@link TypeInsnNode} instructions.
     */
    public static final int TYPE_INSN = 4;

    /**
     * The type of {@link FieldInsnNode} instructions.
     */
    public static final int FIELD_INSN = 5;

    /**
     * The type of {@link MethodInsnNode} instructions.
     */
    public static final int METHOD_INSN = 6;

    /**
     * The type of {@link JumpInsnNode} instructions.
     */
    public static final int JUMP_INSN = 7;

    /**
     * The type of {@link LabelNode} "instructions".
     */
    public static final int LABEL_INSN = 8;

    /**
     * The type of {@link StringInsnNode} instructions.
     */
    public static final int STRING_INSN = 9;

    /**
     * The type of {@link OperationInsnNode} instructions.
     */
    public static final int OPERATION_INSN = 10;

    /**
     * The type of {@link TableSwitchInsnNode} instructions.
     */
    public static final int TABLESWITCH_INSN = 11;

    /**
     * The type of {@link LookupSwitchInsnNode} instructions.
     */
    public static final int LOOKUPSWITCH_INSN = 12;

    /**
     * The type of {@link MultiANewArrayInsnNode} instructions.
     */
    public static final int MULTIANEWARRAY_INSN = 13;

    /**
     * The type of {@link ArrayLengthInsnNode} "instructions".
     */
    public static final int ARRAY_LENGTH_INSN = 14;
    
    /**
     * The type of {@link ArrayOperationInsnNode} "instructions".
     */
    public static final int ARRAY_OPERATION_INSN = 15;

    /**
     * The type of {@link FillArrayDataInsnNode} "instructions".
     */
    public static final int FILL_ARRAY_DATA_INSN = 16;
    
    /**
     * The type of {@link LineNumberNode} "instructions".
     */
    public static final int LINE = 17;

    /**
     * The opcode of this instruction.
     */
    protected int opcode;

    /**
     * Previous instruction in the list to which this instruction belongs.
     */
    AbstractInsnNode prev;

    /**
     * Next instruction in the list to which this instruction belongs.
     */
    AbstractInsnNode next;

    /**
     * Index of this instruction in the list to which it belongs. The value of
     * this field is correct only when {@link InsnList#cache} is not null. A
     * value of -1 indicates that this instruction does not belong to any
     * {@link InsnList}.
     */
    int index;

    /**
     * Constructs a new {@link AbstractInsnNode}.
     * 
     * @param opcode the opcode of the instruction to be constructed.
     */
    protected AbstractInsnNode(final int opcode) {
        this.opcode = opcode;
        this.index = -1;
    }

    /**
     * Returns the opcode of this instruction.
     * 
     * @return the opcode of this instruction.
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * Returns the type of this instruction.
     * 
     * @return the type of this instruction, i.e. one the constants defined in
     *         this class.
     */
    public abstract int getType();

    /**
     * Returns the previous instruction in the list to which this instruction
     * belongs, if any.
     * 
     * @return the previous instruction in the list to which this instruction
     *         belongs, if any. May be <tt>null</tt>.
     */
    public AbstractInsnNode getPrevious() {
        return prev;
    }

    /**
     * Returns the next instruction in the list to which this instruction
     * belongs, if any.
     * 
     * @return the next instruction in the list to which this instruction
     *         belongs, if any. May be <tt>null</tt>.
     */
    public AbstractInsnNode getNext() {
        return next;
    }

    /**
     * Makes the given code visitor visit this instruction.
     * 
     * @param mv a code visitor.
     */
    public abstract void accept(final MethodVisitor mv);

    /**
     * Returns a copy of this instruction.
     * 
     * @param labels a map from LabelNodes to cloned LabelNodes.
     * @return a copy of this instruction. The returned instruction does not
     *         belong to any {@link InsnList}.
     */
    public abstract AbstractInsnNode clone(final Map<LabelNode, LabelNode> labels);

    /**
     * Returns the clone of the given label.
     * 
     * @param label a label.
     * @param map a map from LabelNodes to cloned LabelNodes.
     * @return the clone of the given label.
     */
    static LabelNode clone(final LabelNode label, final Map<LabelNode, LabelNode> map) {
        return (LabelNode) map.get(label);
    }

    /**
     * Returns the clones of the given labels.
     * 
     * @param labels a list of labels.
     * @param map a map from LabelNodes to cloned LabelNodes.
     * @return the clones of the given labels.
     */
    static LabelNode[] clone(final List<LabelNode> labels, final Map<LabelNode, LabelNode> map) {
        LabelNode[] clones = new LabelNode[labels.size()];
        for (int i = 0; i < clones.length; ++i) {
            clones[i] = (LabelNode) map.get(labels.get(i));
        }
        return clones;
    }
}
