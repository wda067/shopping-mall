package com.shop.facade;

import com.shop.dto.request.OrderCreateRequest;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.OrderResponse;
import com.shop.exception.OrderError;
import com.shop.exception.OrderLockFailed;
import com.shop.service.OrderService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final RedissonClient redissonClient;
    private final OrderService orderService;

    public CommonResponse<OrderResponse> orderWithRedisson(String email, OrderCreateRequest request) {
        String key = "lock:product:" + request.getProductId();
        RLock lock = redissonClient.getLock(key);

        int maxAttempts = 50;                 //최대 재시도 횟수
        long backoffMs = 50;                  //재시도 간격
        long waitMsPerTry = 500;              //tryLock 대기
        long leaseMs = 5_000;                 //락 임대시간
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean locked = false;
            try {
                locked = lock.tryLock(waitMsPerTry, leaseMs, TimeUnit.MILLISECONDS);
                if (!locked) {
                    Thread.sleep(backoffMs);
                    continue;  //재시도
                }
                //락 구간 (DB 트랜잭션 내부 로직만)
                return orderService.order(email, request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OrderError();
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    try {
                        lock.unlock();
                    } catch (IllegalMonitorStateException ignore) {
                    }
                }
            }
        }

        throw new OrderLockFailed();  //충분히 재시도했는데도 실패
    }
}
