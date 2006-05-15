/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2005 INRIA, France Telecom
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
package org.objectweb.asm.tree;

import java.util.Iterator;

import org.objectweb.asm.MethodVisitor;

/**
 * A doubly linked list of {@link AbstractInsnNode} objects. <i>This
 * implementation is not thread safe</i>.
 */
public class InsnList {

    /**
     * Indicates if preconditions of methods of this class must be checked.
     * <i>Checking preconditions causes the {@link #indexOf indexOf},
     * {@link #set set}, {@link #insert(AbstractInsnNode, AbstractInsnNode)},
     * {@link #insert(AbstractInsnNode, InsnList)}, {@link #remove remove} and
     * {@link #clear} methods to execute in O(n) time instead of O(1)</i>.
     */
    public static boolean check;

    /**
     * The number of instructions in this list.
     */
    private int size;

    /**
     * The first instruction in this list. May be <tt>null</tt>.
     */
    private AbstractInsnNode first;

    /**
     * The last instruction in this list. May be <tt>null</tt>.
     */
    private AbstractInsnNode last;

    /**
     * A cache of the instructions of this list. This cache is used to improve
     * the performance of the {@link #get} method.
     */
    private AbstractInsnNode[] cache;

    /**
     * Returns the number of instructions in this list.
     * 
     * @return the number of instructions in this list.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the first instruction in this list.
     * 
     * @return the first instruction in this list, or <tt>null</tt> if the
     *         list is empty.
     */
    public AbstractInsnNode getFirst() {
        return first;
    }

    /**
     * Returns the last instruction in this list.
     * 
     * @return the last instruction in this list, or <tt>null</tt> if the list
     *         is empty.
     */
    public AbstractInsnNode getLast() {
        return last;
    }

    /**
     * Returns the instruction whose index is given. This method builds a cache
     * of the instructions in this list to avoid scanning the whole list each
     * time it is called. Once the cache is built, this method run in constant
     * time. This cache is invalidated by all the methods that modify the list.
     * 
     * @param index the index of the instruction that must be returned.
     * @return the instruction whose index is given.
     * @throws IndexOutOfBoundsException if (index < 0 || index >= size()).
     */
    public AbstractInsnNode get(final int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        if (cache == null) {
            cache = toArray();
        }
        return cache[index];
    }

    /**
     * Returns <tt>true</tt> if the given instruction belongs to this list.
     * This method always scans the instructions of this list until it finds the
     * given instruction or reaches the end of the list.
     * 
     * @param insn an instruction.
     * @return <tt>true</tt> if the given instruction belongs to this list.
     */
    public boolean contains(final AbstractInsnNode insn) {
        AbstractInsnNode i = first;
        while (i != null && i != insn) {
            i = i.next;
        }
        return i != null;
    }

    /**
     * Returns the index of the given instruction in this list. This method
     * builds a cache of the instruction indexes to avoid scanning the whole
     * list each time it is called. Once the cache is built, this method run in
     * constant time. The cache is invalidated by all the methods that modify
     * the list.
     * 
     * @param insn an instruction <i>of this list</i>.
     * @return the index of the given instruction in this list. <i>The result of
     *         this method is undefined if the given instruction does not belong
     *         to this list</i>. Use {@link #contains contains} to test if an
     *         instruction belongs to an instruction list or not.
     * @throws IllegalArgumentException if {@link #check} is <tt>true</tt> and
     *         if insn does not belong to this list.
     */
    public int indexOf(final AbstractInsnNode insn) {
        if (check && !contains(insn)) {
            throw new IllegalArgumentException();
        }
        if (cache == null) {
            cache = toArray();
        }
        return insn.index;
    }

    /**
     * Makes the given visitor visit all of the instructions in this list.
     * 
     * @param mv the method visitor that must visit the instructions.
     */
    public void accept(final MethodVisitor mv) {
        AbstractInsnNode insn = first;
        while (insn != null) {
            insn.accept(mv);
            insn = insn.next;
        }
    }

    /**
     * Returns an iterator over the instructions in this list.
     * 
     * @return an iterator over the instructions in this list.
     */
    public Iterator iterator() {
        return new Iterator() {

            AbstractInsnNode insn = first;

            public boolean hasNext() {
                return insn != null;
            }

            public Object next() {
                Object result = insn;
                insn = insn.next;
                return result;
            }

            public void remove() {
                InsnList.this.remove(insn.prev);
            }
        };
    }

    /**
     * Returns an array containing all of the instructions in this list.
     * 
     * @return an array containing all of the instructions in this list.
     */
    public AbstractInsnNode[] toArray() {
        int i = 0;
        AbstractInsnNode elem = first;
        AbstractInsnNode[] insns = new AbstractInsnNode[size];
        while (elem != null) {
            insns[i] = elem;
            elem.index = i++;
            elem = elem.next;
        }
        return insns;
    }

    /**
     * Replaces an instruction of this list with another instruction.
     * 
     * @param i an instruction <i>of this list</i>.
     * @param insn another instruction, <i>which must not belong to any
     *        {@link InsnList}</i>.
     * @throws IllegalArgumentException if {@link #check} is <tt>true</tt>,
     *         and if i does not belong to this list or if insn belongs to an
     *         instruction list.
     */
    public void set(final AbstractInsnNode i, final AbstractInsnNode insn) {
        if (check && !(contains(i) && insn.index == -1)) {
            throw new IllegalArgumentException();
        }
        AbstractInsnNode next = i.next;
        insn.next = next;
        if (next != null) {
            next.prev = insn;
        } else {
            last = insn;
        }
        AbstractInsnNode prev = i.prev;
        insn.prev = prev;
        if (prev != null) {
            prev.next = insn;
        } else {
            first = insn;
        }
        if (cache != null) {
            int index = i.index;
            cache[index] = insn;
            insn.index = index;
        } else {
            insn.index = 0; // insn now belongs to an InsnList
        }
        i.index = -1; // i no longer belongs to an InsnList
        i.prev = null;
        i.next = null;
    }

    /**
     * Adds the given instruction to the end of this list.
     * 
     * @param insn an instruction, <i>which must not belong to any
     *        {@link InsnList}</i>.
     * @throws IllegalArgumentException if {@link #check} is <tt>true</tt>,
     *         and if insn belongs to an instruction list.
     */
    public void add(final AbstractInsnNode insn) {
        if (check && insn.index != -1) {
            throw new IllegalArgumentException();
        }
        ++size;
        if (last == null) {
            first = insn;
            last = insn;
        } else {
            last.next = insn;
            insn.prev = last;
        }
        last = insn;
        cache = null;
        insn.index = 0; // insn now belongs to an InsnList
    }

    /**
     * Adds the given instructions to the end of this list.
     * 
     * @param insns an instruction list, which is cleared during the process.
     * @throws IllegalArgumentException if {@link #check} is <tt>true</tt>,
     *         and if insn == this.
     */
    public void add(final InsnList insns) {
        if (check && insns == this) {
            throw new IllegalArgumentException();
        }
        if (insns.size == 0) {
            return;
        }
        size += insns.size;
        if (last == null) {
            first = insns.first;
            last = insns.last;
        } else {
            AbstractInsnNode elem = insns.first;
            last.next = elem;
            elem.prev = last;
            last = insns.last;
        }
        cache = null;
        insns.removeAll(false);
    }

    /**
     * Inserts the given instruction at the begining of this list.
     * 
     * @param insn an instruction, <i>which must not belong to any
     *        {@link InsnList}</i>.
     * @throws IllegalArgumentException if {@link #check} is <tt>true</tt>,
     *         and if insn belongs to an instruction list.
     */
    public void insert(final AbstractInsnNode insn) {
        if (check && insn.index != -1) {
            throw new IllegalArgumentException();
        }
        ++size;
        if (first == null) {
            first = insn;
            last = insn;
        } else {
            first.prev = insn;
            insn.next = first;
        }
        first = insn;
        cache = null;
        insn.index = 0; // insn now belongs to an InsnList
    }

    /**
     * Inserts the given instructions at the begining of this list.
     * 
     * @param insns an instruction list, which is cleared during the process.
     * @throws IllegalArgumentException if {@link #check} is <tt>true</tt>,
     *         and if insn == this.
     */
    public void insert(final InsnList insns) {
        if (check && insns == this) {
            throw new IllegalArgumentException();
        }
        if (insns.size == 0) {
            return;
        }
        size += insns.size;
        if (first == null) {
            first = insns.first;
            last = insns.last;
        } else {
            AbstractInsnNode elem = insns.last;
            first.prev = elem;
            elem.next = first;
            first = insns.first;
        }
        cache = null;
        insns.removeAll(false);
    }

    /**
     * Inserts the given instruction after the specified instruction.
     * 
     * @param i an instruction <i>of this list</i> after which insn must be
     *        inserted.
     * @param insn the instruction to be inserted, <i>which must not belong to
     *        any {@link InsnList}</i>.
     * @throws IllegalArgumentException if {@link #check} is <tt>true</tt>,
     *         and if i does not belong to this list or if insn belongs to an
     *         instruction list.
     */
    public void insert(final AbstractInsnNode i, final AbstractInsnNode insn) {
        if (check && !(contains(i) && insn.index == -1)) {
            throw new IllegalArgumentException();
        }
        ++size;
        AbstractInsnNode next = i.next;
        if (next == null) {
            last = insn;
        } else {
            next.prev = insn;
        }
        i.next = insn;
        insn.next = next;
        insn.prev = i;
        cache = null;
        insn.index = 0; // insn now belongs to an InsnList
    }

    /**
     * Inserts the given instructions after the specified instruction.
     * 
     * @param i an instruction <i>of this list</i> after which the instructions
     *        must be inserted.
     * @param insns the instruction list to be inserted, which is cleared during
     *        the process.
     * @throws IllegalArgumentException if {@link #check} is <tt>true</tt>,
     *         and if i does not belong to this list or if insns == this.
     */
    public void insert(final AbstractInsnNode i, final InsnList insns) {
        if (check && !(contains(i) && insns != this)) {
            throw new IllegalArgumentException();
        }
        if (insns.size == 0) {
            return;
        }
        size += insns.size;
        AbstractInsnNode ifirst = insns.first;
        AbstractInsnNode ilast = insns.last;
        AbstractInsnNode next = i.next;
        if (next == null) {
            last = ilast;
        } else {
            next.prev = ilast;
        }
        i.next = ifirst;
        ilast.next = next;
        ifirst.prev = i;
        cache = null;
        insns.removeAll(false);
    }

    /**
     * Removes the given instruction from this list.
     * 
     * @param insn the instruction <i>of this list</i> that must be removed.
     * @throws IllegalArgumentException if {@link #check} is <tt>true</tt>,
     *         and if insn does not belong to this list.
     */
    public void remove(final AbstractInsnNode insn) {
        if (check && !contains(insn)) {
            throw new IllegalArgumentException();
        }
        --size;
        AbstractInsnNode next = insn.next;
        AbstractInsnNode prev = insn.prev;
        if (next == null) {
            if (prev == null) {
                first = null;
                last = null;
            } else {
                prev.next = null;
                last = prev;
            }
        } else {
            if (prev == null) {
                first = next;
                next.prev = null;
            } else {
                prev.next = next;
                next.prev = prev;
            }
        }
        cache = null;
        insn.index = -1; // insn no longer belongs to an InsnList
        insn.prev = null;
        insn.next = null;
    }

    /**
     * Removes all of the instructions of this list.
     * 
     * @param mark if the instructions must be marked as no longer belonging to
     *        any {@link InsnList}.
     */
    private void removeAll(final boolean mark) {
        if (mark) {
            AbstractInsnNode insn = first;
            while (insn != null) {
                AbstractInsnNode next = insn.next;
                insn.index = -1; // insn no longer belongs to an InsnList
                insn.prev = null;
                insn.next = null;
                insn = next;
            }
        }
        size = 0;
        first = null;
        last = null;
        cache = null;
    }

    /**
     * Removes all of the instructions of this list.
     */
    public void clear() {
        removeAll(check);
    }
}
