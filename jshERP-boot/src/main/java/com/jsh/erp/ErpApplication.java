package com.jsh.erp;

import com.jsh.erp.utils.ComputerInfo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@SpringBootApplication
@MapperScan("com.jsh.erp.datasource.mappers")
@ServletComponentScan
@EnableScheduling
@EnableAsync
public class ErpApplication{
    private static final int REQUIRED_JAVA_FEATURE = 17;

    public static void main(String[] args) throws IOException {
        requireJava17();
        ConfigurableApplicationContext context = SpringApplication.run(ErpApplication.class, args);
        Environment environment = context.getBean(Environment.class);
        System.out.println("启动成功，后端服务API地址：http://" + ComputerInfo.getIpAddr() + ":"
                + environment.getProperty("server.port") + "/jshERP-boot/swagger-ui/index.html");
        System.out.println("您还需启动前端服务，启动命令：yarn run serve 或 npm run serve，测试用户：jsh，密码：123456");
    }

    private static void requireJava17() {
        int currentJavaFeature = Runtime.version().feature();
        if (currentJavaFeature != REQUIRED_JAVA_FEATURE) {
            throw new IllegalStateException("jshERP-boot 运行时固定为 JDK 17，当前检测到: " + System.getProperty("java.version"));
        }
    }
}
