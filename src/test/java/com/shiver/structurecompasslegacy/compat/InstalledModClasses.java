package com.shiver.structurecompasslegacy.compat;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Assumptions;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/** 对照用户指定的版本，第三方类与资源不复制进测试源码。 */
public final class InstalledModClasses {

    private InstalledModClasses() {
    }

    public static ClassNode read(String jarName, String className) throws Exception {
        String directory = System.getProperty("compatModsDir");
        Assumptions.assumeTrue(directory != null, "Supply -PcompatModsDir to verify installed mod bytecode");
        Path path = Paths.get(directory, jarName);
        try (JarFile jar = new JarFile(path.toFile());
                InputStream input = jar.getInputStream(jar.getJarEntry(className + ".class"))) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }
}
