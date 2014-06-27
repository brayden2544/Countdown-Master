package com.rurri.countdown.di;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import com.typesafe.config.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class RedisModule extends AbstractModule {

    @Inject
    private Config config;

    @Override
    protected void configure() {
        RedisConnectionPoolInterceptor pooler = new RedisConnectionPoolInterceptor();
        requestInjection(pooler);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(JedisPooled.class),pooler);
    }

    @Provides
    public Jedis provideJedis() {
        Jedis jedis = new Jedis(config.getString("redis.host"));
        return jedis;
    }

    @Provides
    @Singleton
    public JedisPool provideJedisPool(Config config) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        JedisPool pool = new JedisPool(poolConfig, config.getString("redis.host"));
        return pool;
    }

}
