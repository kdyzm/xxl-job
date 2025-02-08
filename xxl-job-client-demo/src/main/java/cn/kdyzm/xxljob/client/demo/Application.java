package cn.kdyzm.xxljob.client.demo;

import cn.kdyzm.framework.core.ApplicationStarter;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author kdyzm
 * @date 2023/12/7
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ApplicationStarter.run(Application.class, args);
    }
}
