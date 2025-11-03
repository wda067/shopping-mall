package com.shop.global.mdc;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();  //현재 스레드의 MDC 복사

        return () -> {
            try {
                if (contextMap != null) {  //새로운 스레드에 MDC 값을 복사
                    MDC.setContextMap(contextMap);
                }
                runnable.run();  //기존 비동기 작업 실행
            } finally {
                MDC.clear();
            }
        };
    }
}
