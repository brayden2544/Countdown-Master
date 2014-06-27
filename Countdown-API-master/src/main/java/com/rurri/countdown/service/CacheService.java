package com.rurri.countdown.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rurri.countdown.model.Like;
import com.rurri.countdown.model.Pass;
import com.rurri.countdown.model.User;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.util.*;


public class CacheService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    JedisPool jedisPool;

    @Inject
    private Config config;

    private final static String addIfExistsScript = "if redis.call('SISMEMBER', KEYS[2], KEYS[3]) == 0 then return redis.call('ZADD', KEYS[1], ARGV[1], KEYS[3]) else return 0 end";

    private static String addIfExistsScriptSha = null;

    public void cacheUser(User user) {
        ObjectMapper jacksonMapper = new ObjectMapper();
        Jedis jedis = jedisPool.getResource();
        try {
            Pipeline pipe = jedis.pipelined();
            String key = user.getUserId() + "-User";
            pipe.set(key, jacksonMapper.writeValueAsString(user));
            pipe.expire(key, config.getInt("redis.user.ttl"));
            pipe.sync();
            logger.debug(key + " Cache updated for user");
        } catch (JsonProcessingException e) {
            logger.error("Error converting user to json for caching.", e);
            throw new RuntimeException("Error converting to json", e);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public User getUser(long userId) {
        ObjectMapper jacksonMapper = new ObjectMapper();
        Jedis jedis = jedisPool.getResource();
        try {
            String key = userId + "-User";
            String userInfo = jedis.get(key);
            if (userInfo != null) {
                logger.debug(key + " Cache HIT user");
                return jacksonMapper.readValue(userInfo, User.class);
            } else {
                logger.debug(key + " Cache MISS user");
                return null;
            }
        } catch (JsonProcessingException e) {
            logger.error("Error converting user from json for caching.", e);
            throw new RuntimeException("Error converting from json", e);
        } catch (IOException e) {
            logger.error("Error converting user from json for caching.", e);
            throw new RuntimeException("Error converting from json", e);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public LinkedHashMap<Long, User> getUsers(List<Long> userIds) {
        LinkedHashMap<Long, User> users = new LinkedHashMap<Long, User>(userIds.size());
        List<Response<String>> cacheResults = new ArrayList<Response<String>>(userIds.size());
        ObjectMapper jacksonMapper = new ObjectMapper();
        Jedis jedis = jedisPool.getResource();
        try {
            //First pipeline some redis get commands for the user lookup
            Pipeline pipeline = jedis.pipelined();
            for (Long userId : userIds) {
                String key = userId + "-User";
                cacheResults.add(pipeline.get(key));
            }
            //Sync up the pipeline
            pipeline.sync();

            int i=0;
            for(Response<String> response : cacheResults) {
                if (response.get() != null) {
                    User user = jacksonMapper.readValue(response.get(), User.class);
                    users.put(user.getUserId(), user);
                } else {
                    users.put(userIds.get(i), null);
                }
                i++;
            }
            return users;
        } catch (JsonProcessingException e) {
            logger.error("Error converting user from json for caching.", e);
            throw new RuntimeException("Error converting from json", e);
        } catch (IOException e) {
            logger.error("Error converting user from json for caching.", e);
            throw new RuntimeException("Error converting from json", e);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public void cacheUsers(List<User> users) {
        ObjectMapper jacksonMapper = new ObjectMapper();
        Jedis jedis = jedisPool.getResource();
        try {
            Pipeline pipe = jedis.pipelined();
            for (User user : users) {
                String key = user.getUserId() + "-User";
                pipe.set(key, jacksonMapper.writeValueAsString(user));
                pipe.expire(key, config.getInt("redis.user.ttl"));
            }
            pipe.sync();
            logger.debug("Bulk update of user cache: " + users.size());
        } catch (JsonProcessingException e) {
            logger.error("Error converting user to json for caching.", e);
            throw new RuntimeException("Error converting to json", e);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public void cacheUserToken(String accessToken, long userId) {
        Jedis jedis = jedisPool.getResource();
        try {
            Pipeline pipe = jedis.pipelined();
            String key =  "Token-" + accessToken;
            pipe.set(key, Long.toString(userId));
            pipe.expire(key, config.getInt("redis.user.ttl"));
            pipe.sync();
            logger.debug(key + " Cache updated for user");
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public Long getUserIdByToken(String accessToken) {
        Jedis jedis = jedisPool.getResource();
        try {
            String key =  "Token-" + accessToken;
            String userId = jedis.get(key);
            if (userId != null) {
                logger.debug(key + " Cache HIT user");
                return new Long(userId);
            } else {
                logger.debug(key + " Cache MISS user");
                return null;
            }
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public void removeAccessToken(String accessToken) {
        Jedis jedis = jedisPool.getResource();
        try {
            String key =  "Token-" + accessToken;
            jedis.del(key);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public void addPassToCache(Pass pass) {
        Jedis jedis = jedisPool.getResource();
        try {
            String key =  pass.getUserId() + "-Skips" ;
            String queueListKey = pass.getUserId() + "-QueueList";
            Set<String> userQueues = jedis.smembers(queueListKey);
            Pipeline pipeline = jedis.pipelined();
            pipeline.sadd(key, Long.toString(pass.getPassedUserId()));
            for (String userQueue : userQueues) {
                pipeline.zrem(userQueue, Long.toString(pass.getPassedUserId()));
            }
            pipeline.sync();
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public void addLikeToCache(Like like) {
        Jedis jedis = jedisPool.getResource();
        try {
            String key =  like.getUserId() + "-Skips" ;
            String queueListKey = like.getUserId() + "-QueueList";
            Set<String> userQueues = jedis.smembers(queueListKey);
            Pipeline pipeline = jedis.pipelined();
            pipeline.sadd(key, Long.toString(like.getLikedUserId()));
            for (String userQueue : userQueues) {
                pipeline.zrem(userQueue, Long.toString(like.getLikedUserId()));
            }
            pipeline.sync();
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public void addPotentialMatchesToCache(User user, String geoCode, Map<String, Integer> matches) {
        Jedis jedis = jedisPool.getResource();

        try {
            if (addIfExistsScriptSha == null) {
                synchronized (addIfExistsScript) {
                    if (addIfExistsScriptSha == null) {
                        addIfExistsScriptSha =  jedis.scriptLoad(addIfExistsScript);
                    }

                }
            }

            Pipeline pipeline = jedis.pipelined();
            String skipKey =  user.getUserId() + "-Skips" ;
            String matchKey = user.getUserId() + "-"  + geoCode + "-MatchQueue";
            for (String key : matches.keySet()) {
                pipeline.evalsha(addIfExistsScriptSha, 3, new String[] {matchKey, skipKey, key, Integer.toString(matches.get(key))});
            }
            pipeline.sync();

        } finally {
            jedisPool.returnResource(jedis);
        }
    }

}