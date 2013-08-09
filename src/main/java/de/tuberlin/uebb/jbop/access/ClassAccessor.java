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
package de.tuberlin.uebb.jdae.access;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import de.tuberlin.uebb.jdae.exception.JBOPClassException;
import de.tuberlin.uebb.jdae.optimizer.utils.rename.ClassRenamer;

/**
 * The Class ClassAccessor.
 * 
 * This UtilityClass is used to access / load / write classfiles.
 * 
 * @author Christopher Ewest
 */
public final class ClassAccessor {
  
  private ClassAccessor() {
    //
  }
  
  private static final Path tmpDir;
  static {
    try {
      tmpDir = Files.createTempDirectory("jBOP");
    } catch (final Exception e) {
      throw new RuntimeException("Temporary directory for created classfiles couldn't be created.", e);
    }
  }
  
  private static final URLClassLoader classLoader;
  static {
    final URI uri = tmpDir.toUri();
    final URL url;
    try {
      url = uri.toURL();
    } catch (final MalformedURLException e) {
      throw new RuntimeException("Temporary directory for created classfiles couldn't be used as classpath.", e);
    }
    classLoader = new URLClassLoader(new URL[] {
      url
    });
  }
  
  /**
   * Gets the {@link ClassDescriptor} for Class clazz.
   * 
   * @param clazz
   *          the clazz
   * @return the ClassDescriptor
   * @throws JBOPClassException
   *           if the classFile of clazz couldn't be accessed.
   */
  public static ClassDescriptor getClassDescriptor(final Class<?> clazz) throws JBOPClassException {
    final byte[] classBytes = toBytes(clazz);
    final String classFileName = toPath(clazz).toString();
    return new ClassDescriptor(clazz.getName(), classBytes, classFileName);
  }
  
  /**
   * To class path.
   * 
   * @param clazz
   *          the clazz
   * @return the string
   */
  static String toClassPath(final Class<?> clazz) {
    return toClassPath(Type.getInternalName(clazz));
  }
  
  /**
   * To file.
   * 
   * @param clazz
   *          the clazz
   * @return the file
   * @throws JDAEClassException
   *           the jDAE class exception
   */
  static Path toPath(final Class<?> clazz) throws JBOPClassException {
    final CodeSource cs = clazz.getProtectionDomain().getCodeSource();
    final URL resource = cs.getLocation();
    if (resource == null) {
      throw new JBOPClassException("The Classfile for Class<" + clazz.getName() + "> couldn't be determined.", null);
    }
    String filename = resource.getFile();
    final String path = toClassPath(clazz);
    if (filename.endsWith(".jar")) {
      filename = filename + "!" + path;
    } else {
      filename = filename + "/" + path;
    }
    return Paths.get(filename);
  }
  
  /**
   * Takes an internal Classname ({@link Type#getInternalName()}) and returns
   * a resource path to the corresponding classFile.
   * 
   * @param internalName
   *          the internal name
   * @return the resource-Path
   */
  static String toClassPath(final String internalName) {
    return "/" + internalName + ".class";
  }
  
  /**
   * Stores a classfile for the given Classdescriptor.
   * 
   * The Class for the File could be loaded with the ClassLoader proposed by this
   * Class ({@link #getClassloader()}) or any suitable ClassLoader using the returned
   * Path of the Classfile.
   * 
   * @param classDescriptor
   *          the class descriptor
   * @throws JBOPClassException
   *           the jBOP class exception
   * @return the path of the written File
   */
  public static Path store(final ClassDescriptor classDescriptor) throws JBOPClassException {
    
    final Path packageDir = Paths.get(tmpDir.toString(), classDescriptor.getPackageDir());
    final Path classFile = Paths.get(packageDir.toString(), classDescriptor.getSimpleName() + ".class");
    try {
      Files.createDirectories(packageDir);
      Files.write(classFile, classDescriptor.getClassData(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      return classFile;
    } catch (final IOException e) {
      throw new JBOPClassException("Data of Class " + classDescriptor.getName() + " could not be written to file.", e);
    }
  }
  
  /**
   * Rename the given Class.
   * 
   * Exactly the suffix <code>suffix</code> is added to the Classname
   * and every internal reference is updated to the new name.
   * 
   * @param classDescriptor
   *          the classDescriptor
   * @param suffix
   *          the suffix
   * @return the new ClassDescriptor with the renamed class
   */
  public static ClassDescriptor rename(final ClassDescriptor classDescriptor, final String suffix) {
    final String newName = classDescriptor.getName() + suffix;
    final ClassReader classReader = new ClassReader(classDescriptor.getClassData());
    final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    final ClassRenamer renamer = new ClassRenamer(writer, newName.replace(".", "/"));
    classReader.accept(renamer, 0);
    final byte[] renamedClassBytes = renamer.toByteArray();
    
    final ClassDescriptor out = new ClassDescriptor(newName, renamedClassBytes, StringUtils.removeEnd(
        classDescriptor.getFile(), ".class")
        + suffix + ".class");
    return out;
  }
  
  /**
   * Return the bytes of the corresponding class.
   * 
   * @param input
   *          the input Object (may be a class-Object)
   * @return the byte[] of the class
   * @throws JBOPClassException
   *           if the Classfile for the given Object cannot be accessed
   *           (e.g. if the class is synthetic)
   */
  public static byte[] toBytes(final Object input) throws JBOPClassException {
    if (input == null) {
      throw new JBOPClassException("Nullvalue for input is not allowed.", null);
    }
    Path file;
    Class<?> clazz;
    if (input instanceof Class) {
      clazz = (Class<?>) input;
    } else {
      clazz = input.getClass();
    }
    file = toPath(clazz);
    final String filename = file.toString();
    if (StringUtils.contains(filename, "!")) {
      file = getPathInJar(filename);
    }
    try {
      return Files.readAllBytes(file);
    } catch (final IOException e) {
      throw new JBOPClassException("The content of the Classfile (" + filename + ") couldn't be read.", e);
    }
  }
  
  private static Path getPathInJar(final String filename) throws JBOPClassException {
    Path file;
    final String[] fileParts = StringUtils.split(filename, "!");
    final String zipFile = fileParts[0];
    final String classFile = fileParts[1];
    FileSystem fileSystem;
    try {
      fileSystem = FileSystems.newFileSystem(Paths.get(zipFile), ClassAccessor.class.getClassLoader());
    } catch (final IOException e) {
      throw new JBOPClassException("The jar containing the class (" + zipFile + ": " + classFile
          + ") couldn't be accessed.", e);
    }
    
    file = fileSystem.getPath(classFile);
    return file;
  }
  
  /**
   * returns the used tmp dir.
   */
  public static Path getTmpdir() {
    return tmpDir;
  }
  
  /**
   * Returns the Classloader for created Classes.
   */
  public static URLClassLoader getClassloader() {
    return classLoader;
  }
  
}
