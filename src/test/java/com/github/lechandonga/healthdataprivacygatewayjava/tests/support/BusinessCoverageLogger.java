package com.github.lechandonga.healthdataprivacygatewayjava.tests.support;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.util.ArrayList;
import java.util.List;

public class BusinessCoverageLogger implements BeforeAllCallback, BeforeEachCallback,
        AfterEachCallback, AfterAllCallback, TestExecutionExceptionHandler {

    private static final String PREFIX = "\n========== 业务覆盖日志 ==========";
    private static final String SUFFIX = "==================================";

    private final List<String> results = new ArrayList<>();
    private String business;

    @Override
    public void beforeAll(ExtensionContext context) {
        results.clear();
        business = context.getRequiredTestClass().getAnnotation(BusinessCoverage.class) == null
                ? "未标注业务"
                : context.getRequiredTestClass().getAnnotation(BusinessCoverage.class).value();
        System.out.println(PREFIX);
        System.out.println("业务模块: " + business);
        System.out.println("测试类: " + context.getRequiredTestClass().getSimpleName());
        System.out.println("用例清单:");
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        System.out.println("  - [运行中] " + context.getDisplayName());
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        results.add("  - [失败] " + context.getDisplayName() + " -> " + throwExceptionMessage(throwable));
        throw throwable;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (context.getExecutionException().isEmpty()) {
            results.add("  - [通过] " + context.getDisplayName());
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        results.forEach(System.out::println);
        long passed = results.stream().filter(r -> r.contains("[通过]")).count();
        long failed = results.stream().filter(r -> r.contains("[失败]")).count();
        System.out.println("汇总: 业务[" + business + "] 用例总数=" + results.size()
                + " 通过=" + passed + " 失败=" + failed
                + (failed == 0 ? " 结果=全部通过" : " 结果=存在失败"));
        System.out.println(SUFFIX);
    }

    private static String throwExceptionMessage(Throwable t) {
        String msg = t.getMessage();
        return msg == null ? t.getClass().getSimpleName() : msg.replace("\n", " ");
    }
}
