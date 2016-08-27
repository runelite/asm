package org.objectweb.asm.optimizer;

import java.io.PrintWriter;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.AnalyzerPlugin;

public class ModuleInfoBndPlugin implements AnalyzerPlugin {
  private static final String MODULE_NAME = "Module-Name";
  private static final String MODULE_VERSION = "Module-Version";
  private static final String MODULE_REQUIRES = "Module-Requires";
  private static final String MODULE_EXPORTS = "Module-Exports";
  //private static final String MODULE_PROVIDES = "Module-Provides";
  //private static final String MODULE_USES = "Module-Uses";
  
  public boolean analyzeJar(Analyzer analyzer) throws Exception {
    String moduleName = analyzer.getProperty(MODULE_NAME, analyzer.getProperty(Constants.BUNDLE_SYMBOLICNAME));
    String moduleVersion =  analyzer.getProperty(MODULE_VERSION, analyzer.getProperty(Constants.BUNDLE_VERSION));
    String requireModules = analyzer.getProperty(MODULE_REQUIRES);
    String exportPackages = analyzer.getProperty(MODULE_EXPORTS, analyzer.getProperty(Constants.EXPORT_PACKAGE));
    
    //System.out.println(moduleName);
    //System.out.println(moduleVersion);
    //System.out.println(requireModules);
    //System.out.println(exportPackages);
    
    ClassWriter writer = new ClassWriter(0);
    String moduleInternalName = moduleName.replace('.', '/');
    writer.visit(Opcodes.V1_9, Opcodes.ACC_MODULE, moduleInternalName + "/module-info", null, null, null);
    
    ModuleVisitor mv = writer.visitModule();
    
    // requires
    mv.visitRequire("java.base", Opcodes.ACC_MANDATED);
    if (requireModules != null) {
      Parameters requireParams = analyzer.parseHeader(requireModules);
      for(String requireName: requireParams.keySet()) {
        Attrs attrs = requireParams.get(requireName);
        boolean isTransitive = attrs.containsKey("transitive");
        mv.visitRequire(requireName, isTransitive? Opcodes.ACC_TRANSITIVE: 0);
      }
    }
    
    // exports
    if (exportPackages != null) {
      Parameters exportParams = analyzer.parseHeader(exportPackages);
      for(String packageName: exportParams.keySet()) {
        if (packageName.endsWith("*")) {
            throw new IllegalStateException("unsupported wildcard packages " + packageName);
        }
        mv.visitExport(packageName.replace('.', '/'), 0);
      }
    }
    
    //mv.visitUse("org/objectweb/asm/FunInterface");
    //mv.visitProvide("org/objectweb/asm/FunInterface", "org/objectweb/asm/FunImpl");
    
    mv.visitEnd();
    
    if (moduleVersion != null) {
        final String version = moduleVersion;
        writer.visitAttribute(new Attribute("Version") {
            @Override
            protected ByteVector write(ClassWriter cw, byte[] code, int len, int maxStack, int maxLocals) {
                ByteVector bv = new ByteVector();
                bv.putShort(cw.newUTF8(version));
                return bv;
            }
        });
    }
    
    writer.visitEnd();
    byte[] bytecode = writer.toByteArray();
    
    // debug
    //ClassReader reader = new ClassReader(bytecode);
    //reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
    
    Jar jar = analyzer.getJar();
    EmbeddedResource moduleInfo = new EmbeddedResource(bytecode, System.currentTimeMillis());
    jar.putResource("module-info.class", moduleInfo);
    
    return false;
  }
}
