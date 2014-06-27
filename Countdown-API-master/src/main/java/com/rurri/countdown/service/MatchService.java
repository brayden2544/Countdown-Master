package com.rurri.countdown.service;

import ch.hsr.geohash.GeoHash;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import com.rurri.countdown.model.MatchQueuePointer;

import com.rurri.countdown.model.User;
import com.rurri.countdown.repository.UserRepository;
import com.typesafe.config.Config;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;


import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class MatchService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    JedisPool jedisPool;

    @Inject
    private Config config;

    @Inject
    private UserRepository userRepository;

    @Inject
    private CacheService cacheService;

    public Collection<User> getNextMatches(User user, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            String key = user.getUserId() + "-" + user.getFiveDigitGeohash() + "-MatchQueue";
            Set<String> cacheResults = jedis.zrevrange(key, 0, count - 1);
            if (cacheResults == null || cacheResults.size() == 0) {
                logger.debug("No Cache Results, Setting up match cache");
                setupUserMatchCache(user);
                cacheResults = jedis.zrevrange(key, 0, count - 1);
            }
            List<Long> idResults = new ArrayList<Long>(cacheResults.size());
            boolean expandUserCache = false;
            for (String result : cacheResults) {
                if (StringUtils.isNumeric(result)) {
                    idResults.add(Long.parseLong(result));
                } else {
                    expandUserCache = true;
                }
            }
            if (expandUserCache) {
                logger.debug("Found pointer in results. Expanding cache for later");
                expandMatchResults(user);
            }
            logger.debug("About to return " + idResults.size() + " results.");
            return userRepository.getUsers(idResults);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public void setupUserMatchCache(User user) {
        Jedis jedis = jedisPool.getResource();
        try {
            Pipeline pipeline = jedis.pipelined();
            logger.debug("Looking up Exclude List");
            List<String> excludeList = userRepository.getExludeList(user.getUserId());
            excludeList.add(Long.toString(user.getUserId()));
            String key = user.getUserId() + "-Skips";
            String matchKey = user.getUserId() + "-" + user.getFiveDigitGeohash() + "-MatchQueue";
            String queueListKey = user.getUserId() + "-QueueList";
            pipeline.sadd(key,excludeList.toArray(new String[excludeList.size()]));
            pipeline.expire(key, config.getInt("redis.user.ttl"));
            pipeline.sadd(queueListKey, matchKey);
            pipeline.expire(queueListKey, config.getInt("redis.user.ttl"));
            pipeline.sync();

            GeoHash geoHash =  GeoHash.fromLongValue(GeoHash.fromGeohashString(user.getGeoHash()).longValue(), 25);
            List<String> geoHashes = new ArrayList<String>(9);

            for(GeoHash adjacent : geoHash.getAdjacent()) {
                geoHashes.add(adjacent.toBase32());
            }
            geoHashes.add(user.getFiveDigitGeohash());

            Collection<MatchQueuePointer> matchQueuePointers = userRepository.getMatchQueuePointers(user, geoHashes);

            logger.debug("Expanding matching queue pointers");
            expandMatchResults(user, matchQueuePointers);

            jedis.expire(matchKey, config.getInt("redis.user.ttl"));


        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error While fetching more match results for user", e);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public void expandMatchResults(User user) {
        Jedis jedis = jedisPool.getResource();
        try {
            logger.debug("Checking to see if we have any results to expand.");
            String key = user.getUserId() + "-" + user.getFiveDigitGeohash() + "-MatchQueue";
            Set<String> cacheResults = jedis.zrevrange(key, 0, 50);

            List<MatchQueuePointer> pointerList = new ArrayList<MatchQueuePointer>();
            List<MatchQueuePointer> pointerListTop = new ArrayList<MatchQueuePointer>();

            ObjectMapper jacksonMapper = new ObjectMapper();

            boolean nonPointerResult = false;

            Pipeline pipeline = jedis.pipelined();
            for (String result : cacheResults) {
                if (!StringUtils.isNumeric(result)) {
                    logger.debug("Found a pointer for more results: " + result);
                    MatchQueuePointer queuePointer = jacksonMapper.readValue(result, MatchQueuePointer.class);
                    pointerList.add(queuePointer);
                    pipeline.zrem(key, result);
                    if (!nonPointerResult) {
                        pointerListTop.add(queuePointer);
                    }
                } else {
                    logger.debug("Found a non-pointer result in the cache");
                    nonPointerResult = true;
                }
            }
            pipeline.sync();

            if (pointerListTop.size() > 0) {
                userRepository.saveMatchQueuePointers(pointerListTop);
            }

            expandMatchResults(user, pointerList);

            
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error While fetching more match results for user", e);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    private void expandMatchResults(User user, Collection<MatchQueuePointer> pointerList) throws InterruptedException, ExecutionException, JsonProcessingException {
        Jedis jedis = jedisPool.getResource();
        try {
            logger.debug("Expanding match results for " + pointerList.size() + " items.");
            String key = user.getUserId() + "-" + user.getFiveDigitGeohash() + "-MatchQueue";
            Map<String, Future<QueryResult>> futureQueryResults = new HashMap<String, Future<QueryResult>>();
            ObjectMapper jacksonMapper = new ObjectMapper();

            for(MatchQueuePointer queuePointer : pointerList) {
                logger.debug("Creating future query for: " + queuePointer.getGeoHash());
                futureQueryResults.put(queuePointer.getGeoHash(), userRepository.getUserResultsByRegion(user, queuePointer.getGeoHash(), queuePointer.getLastEvaluatedKey(), 20));
            }

            Pipeline pipeline = jedis.pipelined();
            Map<String, Integer> mappedResults = new HashMap<String, Integer>();
            for(MatchQueuePointer queuePointer : pointerList) {
                logger.debug("Getting future result for: " + queuePointer.getGeoHash());
                QueryResult queryResult = futureQueryResults.get(queuePointer.getGeoHash()).get();
                Integer lastScore = null;
                for(Map<String, AttributeValue> resultRow : queryResult.getItems()) {
                    lastScore = (Integer.parseInt(resultRow.get("g-pts").getN()) % User.FEMALE_BITMASK);
                    mappedResults.put(resultRow.get("uid").getN(), lastScore);
                }
                if (lastScore != null && queryResult.getLastEvaluatedKey() != null) {
                    MatchQueuePointer matchQueuePointer = new MatchQueuePointer();
                    matchQueuePointer.setUserId(user.getUserId());
                    matchQueuePointer.setGeoHash(queuePointer.getGeoHash());
                    matchQueuePointer.setLastEvaluatedKey(queryResult.getLastEvaluatedKey());
                    pipeline.zadd(key, lastScore-1 , jacksonMapper.writeValueAsString(matchQueuePointer));
                }
            }
            cacheService.addPotentialMatchesToCache(user, user.getFiveDigitGeohash(), mappedResults);
            pipeline.sync();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error While fetching more match results for user", e);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }
}