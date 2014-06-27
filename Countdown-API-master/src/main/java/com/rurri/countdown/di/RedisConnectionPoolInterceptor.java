package com.rurri.countdown.di;

import com.google.inject.Inject;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisConnectionPoolInterceptor implements MethodInterceptor
{

    @Inject
    private JedisPool jedisPool;

    public Object invoke(MethodInvocation invocation) throws Throwable {
        Jedis jedis = jedisPool.getResource();
        try {
            Object args[] = invocation.getArguments();
            Class params[] = invocation.getMethod().getParameterTypes();

            for (int i=0; i < params.length; i++) {
                if (params[i].isAssignableFrom(Jedis.class)) {
                    args[i] = jedis;
                }
            }
            return invocation.proceed();
        } finally {
            jedisPool.returnResource(jedis);
        }
    }
}
