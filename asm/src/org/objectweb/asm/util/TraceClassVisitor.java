/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2004 INRIA, France Telecom
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

package org.objectweb.asm.util;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.signature.ClassSignatureVisitor;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.TypeSignatureVisitor;


/**
 * A {@link ClassVisitor} that prints a disassembled view of the classes it
 * visits. This class visitor can be used alone (see the
 * {@link #main main} method) to disassemble a class. It can also be used in
 * the middle of class visitor chain to trace the class that is visited at a
 * given point in this chain. This may be uselful for debugging purposes.
 * <p>
 * The trace printed when visiting the <tt>Hello</tt> class is the following:
 * <p>
 * <blockquote>
 * <pre>
 * // class version 49.0 (49)
 * // access flags 33
 * public class Hello {
 * 
 *  // compiled from: Hello.java
 * 
 *   // access flags 1
 *   public <init> ()V
 *     ALOAD 0
 *     INVOKESPECIAL java/lang/Object <init> ()V
 *     RETURN
 *     MAXSTACK = 1
 *     MAXLOCALS = 1
 * 
 *   // access flags 9
 *   public static main ([Ljava/lang/String;)V
 *     GETSTATIC java/lang/System out Ljava/io/PrintStream;
 *     LDC "hello"
 *     INVOKEVIRTUAL java/io/PrintStream println (Ljava/lang/String;)V
 *     RETURN
 *     MAXSTACK = 2
 *     MAXLOCALS = 1
 * }
 * </pre>
 * </blockquote>
 * where <tt>Hello</tt> is defined by:
 * <p>
 * <blockquote>
 * <pre>
 * public class Hello {
 *
 *   public static void main (String[] args) {
 *     System.out.println("hello");
 *   }
 * }
 * </pre>
 * </blockquote>
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */

public class TraceClassVisitor extends TraceAbstractVisitor
  implements ClassVisitor
{

  /**
   * The print writer to be used to print the class.
   */

  protected final PrintWriter pw;

  /**
   * Prints a disassembled view of the given class to the standard output.
   * <p>
   * Usage: TraceClassVisitor [-debug]
   * &lt;fully qualified class name or class file name &gt;
   *
   * @param args the command line arguments.
   *
   * @throws Exception if the class cannot be found, or if an IO exception
   *      occurs.
   */

  public static void main (final String[] args) throws Exception {
    int i = 0;
    boolean skipDebug = true;

    boolean ok = true;
    if (args.length < 1 || args.length > 2) {
      ok = false;
    }
    if (ok && args[0].equals("-debug")) {
      i = 1;
      skipDebug = false;
      if (args.length != 2) {
        ok = false;
      }
    }
    if (!ok) {
      System.err.println("Prints a disassembled view of the given class.");
      System.err.println("Usage: TraceClassVisitor [-debug] " +
                         "<fully qualified class name or class file name>");
      System.exit(-1);
    }
    ClassReader cr;
    if (args[i].endsWith(".class")) {
      cr = new ClassReader(new FileInputStream(args[i]));
    } else {
      cr = new ClassReader(args[i]);
    }
    cr.accept(
      new TraceClassVisitor(new PrintWriter(System.out)),
      getDefaultAttributes(),
      skipDebug);
  }

  /**
   * Constructs a new {@link TraceClassVisitor}.
   *
   * @param pw the print writer to be used to print the class.
   */

  public TraceClassVisitor (final PrintWriter pw) {
    this.pw = pw;
  }

  // --------------------------------------------------------------------------
  // Implementation of the ClassVisitor interface
  // --------------------------------------------------------------------------

  public void visit (
    final int version,
    final int access,
    final String name,
    final String signature,
    final String superName,
    final String[] interfaces)
  {
    int major = version & 0xFFFF;
    int minor = version >>> 16;
    buf.setLength(0);
    buf.append("// class version ")
      .append(major)
      .append('.')
      .append(minor)
      .append(" (")
      .append(version)
      .append(")\n");
    if ((access & Opcodes.ACC_DEPRECATED) != 0) {
      buf.append("// DEPRECATED\n");
    }
    buf.append("// access flags ").append(access).append('\n');
    appendAccess(access & ~Opcodes.ACC_SUPER);
    if ((access & Opcodes.ACC_ANNOTATION) != 0) {
      buf.append("@interface ");
    } else if ((access & Opcodes.ACC_INTERFACE) != 0) {
        buf.append("interface ");
    } else if ((access & Opcodes.ACC_ENUM) != 0) {
      buf.append("enum ");
    } else {
      buf.append("class ");
    }
    appendDescriptor(INTERNAL_NAME, name);
    
    if( signature!=null) {
      ClassSignatureDecompiler classSignatureDecompiler = new ClassSignatureDecompiler( access, name);

      SignatureReader r = new SignatureReader( signature);
      r.acceptClass( classSignatureDecompiler);
      
      buf.append( classSignatureDecompiler.toString());
      
    } else {
      buf.append(' ');
      if (superName != null && !superName.equals("java/lang/Object")) {
        buf.append("extends ");
        appendDescriptor(INTERNAL_NAME, superName);
        buf.append(' ');
      }
      if (interfaces != null && interfaces.length > 0) {
        buf.append("implements ");
        for (int i = 0; i < interfaces.length; ++i) {
          appendDescriptor(INTERNAL_NAME, interfaces[i]);
          buf.append(' ');
        }
      }
//      if (signature != null) {
//        buf.append("/* ");
//        appendDescriptor(CLASS_SIGNATURE, signature);
//        buf.append(" */ {\n\n");
//      }
    }
    buf.append(" {\n\n");

    text.add(buf.toString());
  }

  public void visitSource (final String file, final String debug) {
    buf.setLength(0);
    if (file != null) {
      buf.append(tab).append("// compiled from: ").append(file).append('\n');
    }
    if (debug != null) {
      buf.append(tab).append("// debug info: ").append(debug).append('\n');
    }
    if (buf.length() > 0) {
      text.add(buf.toString());
    }
  }

  public void visitOuterClass (
    final String owner,
    final String name,
    final String desc)
  {
    buf.setLength(0);
    buf.append(tab).append("OUTERCLASS ");
    appendDescriptor(INTERNAL_NAME, owner);
    buf.append(' ').append(name).append(' ');
    appendDescriptor(METHOD_DESCRIPTOR, desc);
    buf.append('\n');
    text.add(buf.toString());
  }

  public AnnotationVisitor visitAnnotation (
    final String desc,
    final boolean visible)
  {
    text.add("\n");
    return super.visitAnnotation(desc, visible);
  }

  public void visitAttribute (final Attribute attr) {
    text.add("\n");
    super.visitAttribute(attr);
  }

  public void visitInnerClass (
    final String name,
    final String outerName,
    final String innerName,
    final int access)
  {
    buf.setLength(0);
    buf.append(tab).append("INNERCLASS ");
    appendDescriptor(INTERNAL_NAME, name);
    buf.append(' ');
    appendDescriptor(INTERNAL_NAME, outerName);
    buf.append(' ');
    appendDescriptor(INTERNAL_NAME, innerName);
    buf.append(' ').append(access & ~Opcodes.ACC_SUPER);
    if ((access & Opcodes.ACC_ENUM) != 0) {
      buf.append("enum ");
    }
    buf.append('\n');
    text.add(buf.toString());
  }

  public FieldVisitor visitField (
    final int access,
    final String name,
    final String desc,
    final String signature,
    final Object value)
  {
    buf.setLength(0);
    buf.append('\n');
    if ((access & Opcodes.ACC_DEPRECATED) != 0) {
      buf.append(tab).append("// DEPRECATED\n");
    }
    buf.append(tab).append("// access flags ").append(access).append('\n');
    buf.append(tab);
    appendAccess(access);
    if ((access & Opcodes.ACC_ENUM) != 0) {
      buf.append("enum ");
    }
    appendDescriptor(FIELD_DESCRIPTOR, desc);
    buf.append(' ').append(name);
    if (value != null) {
      buf.append(" = ");
      if (value instanceof String) {
        buf.append("\"").append(value).append("\"");
      } else {
        buf.append(value);
      }
    }
    if (signature != null) {
      buf.append("// ");
      appendDescriptor(FIELD_SIGNATURE, signature);
    }
    buf.append('\n');
    text.add(buf.toString());

    TraceFieldVisitor tav = createTraceFieldVisitor();
    text.add(tav.getText());
    return tav;
  }

  public MethodVisitor visitMethod (
    final int access,
    final String name,
    final String desc,
    final String signature,
    final String[] exceptions)
  {
    buf.setLength(0);
    buf.append('\n');
    if ((access & Opcodes.ACC_DEPRECATED) != 0) {
      buf.append(tab).append("// DEPRECATED\n");
    }
    buf.append(tab).append("// access flags ").append(access).append('\n');
    buf.append(tab);
    appendAccess(access);
    if ((access & Opcodes.ACC_NATIVE) != 0) {
      buf.append("native ");
    }
    if ((access & Opcodes.ACC_VARARGS) != 0) {
      buf.append("varargs ");
    }
    if ((access & Opcodes.ACC_BRIDGE) != 0) {
      buf.append("bridge ");
    }
    buf.append(name).append(' ');
    appendDescriptor(METHOD_DESCRIPTOR, desc);
    if (exceptions != null && exceptions.length > 0) {
      buf.append(" throws ");
      for (int i = 0; i < exceptions.length; ++i) {
        appendDescriptor(INTERNAL_NAME, exceptions[i]);
        buf.append(' ');
      }
    }
    if (signature != null) {
      buf.append("// ");
      appendDescriptor(METHOD_SIGNATURE, signature);
    }
    buf.append('\n');
    text.add(buf.toString());

    TraceMethodVisitor tcv = createTraceMethodVisitor();
    text.add(tcv.getText());
    return tcv;
  }

  public void visitEnd () {
    text.add("}\n");

    printList(pw, text);
    pw.flush();
  }

  // --------------------------------------------------------------------------
  // Utility methods
  // --------------------------------------------------------------------------

  protected TraceFieldVisitor createTraceFieldVisitor () {
    return new TraceFieldVisitor();
  }

  protected TraceMethodVisitor createTraceMethodVisitor () {
    return new TraceMethodVisitor();
  }
  
  /**
   * Appends a string representation of the given access modifiers to {@link
   * #buf buf}.
   *
   * @param access some access modifiers.
   */

  private void appendAccess (final int access) {
    if ((access & Opcodes.ACC_PUBLIC) != 0) {
      buf.append("public ");
    }
    if ((access & Opcodes.ACC_PRIVATE) != 0) {
      buf.append("private ");
    }
    if ((access & Opcodes.ACC_PROTECTED) != 0) {
      buf.append("protected ");
    }
    if ((access & Opcodes.ACC_FINAL) != 0) {
      buf.append("final ");
    }
    if ((access & Opcodes.ACC_STATIC) != 0) {
      buf.append("static ");
    }
    if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
      buf.append("synchronized ");
    }
    if ((access & Opcodes.ACC_VOLATILE) != 0) {
      buf.append("volatile ");
    }
    if ((access & Opcodes.ACC_TRANSIENT) != 0) {
      buf.append("transient ");
    }
    // if ((access & Constants.ACC_NATIVE) != 0) {
    //   buf.append("native ");
    // }
    if ((access & Opcodes.ACC_ABSTRACT) != 0) {
      buf.append("abstract ");
    }
    if ((access & Opcodes.ACC_STRICT) != 0) {
      buf.append("strictfp ");
    }
  }


  /**
   * <code>ClassSignatureVisitor</code> implementation that constructs 
   * Java class declaration based on information from sigature value. 
   */
  public static final class ClassSignatureDecompiler implements ClassSignatureVisitor {
    private List formalParams = new ArrayList();
    
    private TypeDecompiler superClass;
    private List interfaces = new ArrayList();

    private boolean isInterface;
    private String className;

    
    public ClassSignatureDecompiler( int access, String name) {
      this.isInterface = ( access | Opcodes.ACC_INTERFACE)!=0;
      this.className = name;
    }

    public void visitFormalTypeParameter( String name) {
      formalParams.add( new FormalParam( name));
    }

    public TypeSignatureVisitor visitClassBound() {
      FormalParam param = ( FormalParam) formalParams.get( formalParams.size()-1);
      TypeDecompiler boundDecompiler = new TypeDecompiler();
      param.classBound = boundDecompiler;
      return boundDecompiler;
    }

    public TypeSignatureVisitor visitInterfaceBound() {
      FormalParam param = ( FormalParam) formalParams.get( formalParams.size()-1);
      TypeDecompiler boundDecompiler = new TypeDecompiler();
      param.interfaceBounds.add( boundDecompiler);
      return boundDecompiler;
    }

    
    public TypeSignatureVisitor visitSuperclass() {
      superClass = new TypeDecompiler();
      return superClass;
    }

    public TypeSignatureVisitor visitInterface() {
      TypeSignatureVisitor i = new TypeDecompiler();
      interfaces.add( i);
      return i;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      
      if( formalParams.size()>0) {
        sb.append( "<");
        String d = "";
        for( Iterator it = formalParams.iterator(); it.hasNext();) {
          sb.append( d).append( it.next().toString());
          d = ", ";
        }
        sb.append( ">");
      }
      
      if( superClass!=null && !"java/lang/Object".equals( superClass.type)) {
        sb.append( " extends ").append( superClass.toString());
      }
      if( this.interfaces.size()>0) {
        // TODO should use "extends" for interfaces
        // sb.append( isInterface ? " extends " : " implements ");
        sb.append( " implements ");
        String d = "";
        for( Iterator it = this.interfaces.iterator(); it.hasNext();) {
          sb.append( d).append( it.next().toString());
          d = ", ";
        }
      }
      return sb.toString();
    }
  }

  
  public static final class TypeDecompiler extends TypeSignatureAdapter {
    public String type;
    public List arguments = new ArrayList();
    
    public void visitClassType( String name) {
      type = name;
    }
    
    public TypeSignatureVisitor visitTypeArgument( char tag) {
      TypeArgumentDecompiler argument = new TypeArgumentDecompiler(tag);
      arguments.add(argument);
      return argument;
    }
    
    public String toString() {
      StringBuffer sb = new StringBuffer( type.replace( '/', '.'));
      if( arguments.size()>0) {
        sb.append( "<");
        String d = "";
        for( Iterator it = arguments.iterator(); it.hasNext();) {
          sb.append( d).append( it.next().toString());
          d = ", ";
        }
        sb.append( ">");
      }
      return sb.toString();
    }

  }
  
  
  public static final class TypeArgumentDecompiler extends TypeSignatureAdapter {
    public char tag;
    public String typeVariable;

    public TypeArgumentDecompiler( char tag) {
      this.tag = tag;
    }

    public void visitTypeVariable( String name) {
      typeVariable = name;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      if( tag!=' ') {
        // TODO expand tag
        sb.append( "[").append( tag).append( "]");
      }
      if( typeVariable!=null) {
        sb.append( typeVariable);
      }
      return sb.toString();
    }
    
  }
  
  
  public static abstract class TypeSignatureAdapter implements TypeSignatureVisitor {

    public void visitClassType( String name) {
      throw new IllegalStateException( getClass().getName());
    }

    public void visitInnerClassType( String name) {
      throw new IllegalStateException( getClass().getName());
    }

    public void visitBaseType( char descriptor) {
      throw new IllegalStateException( getClass().getName());
    }

    public void visitTypeVariable( String name) {
      throw new IllegalStateException( getClass().getName());
    }

    public TypeSignatureVisitor visitArrayType() {
      throw new IllegalStateException( getClass().getName());
    }

    public TypeSignatureVisitor visitTypeArgument( char tag) {
      throw new IllegalStateException( getClass().getName());
    }

    public void visitEnd() {
      // throw new IllegalStateException();
    }

  }

  
  public static class FormalParam {
    public String name;
    public TypeDecompiler classBound;
    public List interfaceBounds = new ArrayList();

    public FormalParam( String name) {
      this.name = name;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append( name);
      if( classBound!=null && !"java/lang/Object".equals( classBound.type)) {
        sb.append( " extends ").append( classBound.toString());
      }
      if( interfaceBounds.size()>0) {
        sb.append( " implements");
        for( Iterator it2 = interfaceBounds.iterator(); it2.hasNext();) {
          TypeDecompiler boundDecompiler = ( TypeDecompiler) it2.next();
          sb.append( " ").append( boundDecompiler.toString());
        }
      }
      return sb.toString();
    }
  }

}
