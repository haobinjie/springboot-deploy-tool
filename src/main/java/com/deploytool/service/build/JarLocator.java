package com.deploytool.service.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 打包产物识别：扫描 target 目录，找到可部署的 jar。
 * 优先选择非 sources/javadoc 的普通 jar，返回体积最大者（SpringBoot fat-jar 通常最大）。
 */
public final class JarLocator {

    private JarLocator() {
    }

    public static Optional<Path> locate(Path projectDir) {
        Path target = projectDir.resolve("target");
        if (!Files.isDirectory(target)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(target)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".jar")
                                && !n.endsWith("-sources.jar")
                                && !n.endsWith("-javadoc.jar");
                    })
                    .max(Comparator.comparingLong(JarLocator::sizeSafe));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static long sizeSafe(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0L;
        }
    }
}
