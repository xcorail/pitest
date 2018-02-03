package org.pitest.mutationtest.build.intercept.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.pitest.bytecode.analysis.MethodMatchers.named;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.bytecode.analysis.MethodTree;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.functional.Option;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.util.ResourceFolderByteArraySource;

public abstract class InfiniteLoopBaseTest {

  ClassByteArraySource    source = ClassloaderByteArraySource.fromContext();

  abstract InfiniteLoopFilter testee();

  void checkNotFiltered(Class<?> clazz, String method) {
    checkNotFiltered(ClassName.fromClass(clazz), method);
  }

  void checkFiltered(Class<?> clazz, String method) {
    checkFiltered(ClassName.fromClass(clazz), method);
  }

  void checkNotFiltered(ClassName clazz, String method) {
    checkNotFiltered(clazz, named(method));
  }

  void checkNotFiltered(ClassName clazz, Predicate<MethodTree> method) {
    boolean testedSomething = false;
    for (final Compiler each : Compiler.values()) {
      final Option<MethodTree> mt = parseMethodFromCompiledResource(clazz, each,
          method);
      for (final MethodTree m : mt) {
        assertThat(testee().infiniteLoopMatcher()
            .matches(m.instructions()))
                .describedAs("With " + each
                    + " compiler matched when it shouldn't " + toString(m))
                .isFalse();
        testedSomething = true;
      }

    }
    if (!testedSomething) {
      fail("No samples found for test");
    }
  }

  void checkFiltered(ClassName clazz, String method) {
    checkFiltered(clazz, named(method));
  }

  void checkFiltered(ClassName clazz, Predicate<MethodTree> method) {
    boolean testedSomething = false;
    for (final Compiler each : Compiler.values()) {
      final Option<MethodTree> mt = parseMethodFromCompiledResource(clazz, each,
          method);
      for (final MethodTree m : mt) {
        assertThat(testee().infiniteLoopMatcher()
            .matches(m.instructions()))
                .describedAs("With " + each
                    + " compiler did not match as expected " + toString(m))
                .isTrue();
        testedSomething = true;
      }
    }
    if (!testedSomething) {
      fail("No samples found for test");
    }
  }

  private String toString(MethodTree mt) {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    final TraceMethodVisitor mv = new TraceMethodVisitor(new Textifier());

    mt.rawNode().accept(mv);
    try (PrintWriter pw = new PrintWriter(bos)) {
      mv.p.print(pw);
    }

    return "Byte code is \n" + new String(bos.toByteArray());
  }

  private Option<MethodTree> parseMethodFromCompiledResource(ClassName clazz,
      Compiler compiler, Predicate<MethodTree> method) {
    final ResourceFolderByteArraySource source = new ResourceFolderByteArraySource();
    final Option<byte[]> bs = source.getBytes("loops/" + compiler.name() + "/" + clazz.getNameWithoutPackage().asJavaName());
    for (final byte[] bytes : bs) {
      final ClassTree tree = ClassTree.fromBytes(bytes);
      return tree.methods().findFirst(method);
    }
    return Option.none();
  }

  ClassTree forClass(Class<?> clazz) {
    final byte[] bs = this.source.getBytes(clazz.getName()).value();
    return ClassTree.fromBytes(bs);
  }

  Collection<MethodMutatorFactory> asList(MethodMutatorFactory ...factories ) {
    return Arrays.asList(factories);
  }


  GregorMutater createMutator(MethodMutatorFactory ...factories) {
    final Collection<MethodMutatorFactory> mutators = asList(factories);
    return new GregorMutater(this.source, m -> true, mutators);
  }

}

enum Compiler {
  eclipse, javac
}