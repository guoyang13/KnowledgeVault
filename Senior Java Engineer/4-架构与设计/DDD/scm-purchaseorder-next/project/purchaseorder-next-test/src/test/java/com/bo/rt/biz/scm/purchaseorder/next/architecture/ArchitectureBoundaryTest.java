package com.bo.rt.biz.scm.purchaseorder.next.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 架构边界测试占位。
 *
 * <p>后续建议补充 ArchUnit 规则，约束 domain 不依赖 infrastructure、interfaces 和外部协议对象。</p>
 */
public class ArchitectureBoundaryTest {

    /**
     * 校验领域层依赖边界。
     *
     * <p>当前仅保留测试位置，后续接入 ArchUnit 后补充真实断言。</p>
     */
    public void domainShouldNotDependOnOuterLayers() throws IOException {
        Path projectRoot = locateProjectRoot();
        Path domainSource = projectRoot.resolve("purchaseorder-next-domain/src/main/java");
        List<String> forbiddenImports = List.of(
                ".application.",
                ".infrastructure.",
                ".interfaces.",
                ".api."
        );
        try (var files = Files.walk(domainSource)) {
            List<Path> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsForbiddenImport(path, forbiddenImports))
                    .toList();
            if (!violations.isEmpty()) {
                throw new AssertionError("领域层存在向外依赖: " + violations);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new ArchitectureBoundaryTest().domainShouldNotDependOnOuterLayers();
        System.out.println("Architecture boundary passed.");
    }

    private Path locateProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (Files.isDirectory(current.resolve("purchaseorder-next-domain"))) {
            return current;
        }
        if (current.getParent() != null
                && Files.isDirectory(current.getParent().resolve("purchaseorder-next-domain"))) {
            return current.getParent();
        }
        throw new IllegalStateException("无法定位伪代码工程根目录");
    }

    private boolean containsForbiddenImport(Path source, List<String> forbiddenImports) {
        try {
            String content = Files.readString(source);
            return content.lines()
                    .filter(line -> line.startsWith("import "))
                    .anyMatch(line -> forbiddenImports.stream().anyMatch(line::contains));
        } catch (IOException exception) {
            throw new IllegalStateException("读取源码失败: " + source, exception);
        }
    }
}
