/*
 * (C) Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   ohun@live.cn (夜色)
 */

package com.mpush.cache.redis.manager;

import com.google.common.collect.Lists;
import com.mpush.api.spi.common.*;
import com.mpush.cache.redis.connection.RedisConnectionFactory;
import com.mpush.tools.Jsons;
import com.mpush.tools.Utils;
import com.mpush.tools.config.IConfig;
import com.mpush.tools.log.Logs;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.sync.RedisClusterPubSubCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * redis 对外封装接口
 */
public final class RedisManager implements CacheManager {
    public static final RedisManager instance = new RedisManager();

    private final RedisConnectionFactory factory = new RedisConnectionFactory();

    public void init() {
        Logs.CACHE.info("begin init redis...");
        factory.setPassword(IConfig.mp.redis.password);
        factory.setPoolConfig(IConfig.mp.redis.getPoolConfig(GenericObjectPoolConfig.class));
        factory.setRedisServers(IConfig.mp.redis.nodes);
        factory.setCluster(IConfig.mp.redis.isCluster());
        if (IConfig.mp.redis.isSentinel()) {
            factory.setSentinel(true);
            factory.setSentinelMaster(IConfig.mp.redis.sentinelMaster);
        }
        factory.init();
        test();
        Logs.CACHE.info("init redis success...");
    }

    private <R> R call(Function<RedisCommands<String, String>, R> function, R d) {
        StatefulRedisConnection connection = null;
        try{
            connection = factory.getRedisConnection();
            return function.apply((RedisCommands<String, String>)connection.sync());
        } catch (Exception e) {
            Logs.CACHE.error("redis ex", e);
            throw new RuntimeException(e);
        } finally {
            if(connection != null){
                factory.getNormalPool().returnObject(connection);
            }
        }
    }

    private void call(Consumer<RedisCommands<String, String>> consumer) {
        StatefulRedisConnection connection = null;
        try {
            connection = factory.getRedisConnection();
            consumer.accept(connection.sync());
        } catch (Exception e) {
            Logs.CACHE.error("redis ex", e);
            throw new RuntimeException(e);
        } finally {
            if(connection != null){
                factory.getNormalPool().returnObject(connection);
            }
        }
    }

    private <R> R callCluster(Function<RedisClusterCommands<String, String>, R> function, R d) {
        StatefulRedisClusterConnection connection = null;
        try {
            connection = factory.getClusterConnection();
            return function.apply((RedisClusterCommands<String, String>)connection.sync());
        } catch (Exception e) {
            Logs.CACHE.error("redis ex", e);
            throw new RuntimeException(e);
        } finally {
            if(connection != null){
                factory.getNormalPool().returnObject(connection);
            }
        }
    }

    private void callCluster(Consumer<RedisClusterCommands<String, String>> consumer) {
        StatefulRedisClusterConnection connection = null;
        try {
            connection = factory.getClusterConnection();
            consumer.accept(connection.sync());
        } catch (Exception e) {
            Logs.CACHE.error("redis ex", e);
            throw new RuntimeException(e);
        } finally {
            if(connection != null){
                factory.getNormalPool().returnObject(connection);
            }
        }
    }

    private <R> R callPubSub(Function<RedisPubSubCommands<String, String>, R> function, R d) {
        StatefulRedisPubSubConnection connection = null;
        try{
            connection = factory.getRedisPubSubConnection();
            return function.apply((RedisPubSubCommands<String, String>)connection.sync());
        } catch (Exception e) {
            Logs.CACHE.error("redis ex", e);
            throw new RuntimeException(e);
        } finally {
            if(connection != null){
                factory.getPubsubPool().returnObject(connection);
            }
        }
    }

    private void callPubSub(Consumer<RedisPubSubCommands<String, String>> consumer) {
        StatefulRedisPubSubConnection connection = null;
        try {
            connection = factory.getRedisPubSubConnection();
            consumer.accept(connection.sync());
        } catch (Exception e) {
            Logs.CACHE.error("redis ex", e);
            throw new RuntimeException(e);
        } finally {
            if(connection != null){
                factory.getPubsubPool().returnObject(connection);
            }
        }
    }

    private <R> R callClusterPubSub(Function<RedisClusterPubSubCommands<String, String>, R> function, R d) {
        StatefulRedisClusterPubSubConnection connection = null;
        try {
            connection = factory.getClusterPubSubConnection();
            return function.apply((RedisClusterPubSubCommands<String, String>)connection.sync());
        } catch (Exception e) {
            Logs.CACHE.error("redis ex", e);
            throw new RuntimeException(e);
        } finally {
            if(connection != null){
                factory.getPubsubPool().returnObject(connection);
            }
        }
    }

    private void callClusterPubSub(Consumer<RedisClusterPubSubCommands<String, String>> consumer) {
        StatefulRedisClusterPubSubConnection connection = null;
        try {
            connection = factory.getClusterPubSubConnection();
            consumer.accept(connection.sync());
        } catch (Exception e) {
            Logs.CACHE.error("redis ex", e);
            throw new RuntimeException(e);
        } finally {
            if(connection != null){
                factory.getPubsubPool().returnObject(connection);
            }
        }
    }

    public long incr(String key) {
        if (factory.isCluster()) {
            return callCluster(command -> command.incr(key), 0L);
        } else {
            return call(command -> command.incr(key), 0L);
        }
    }

    public long incrBy(String key, long delt) {
        if (factory.isCluster()) {
            return callCluster(command -> command.incrby(key, delt), 0L);
        } else {
            return call(command -> command.incrby(key, delt), 0L);
        }
    }

    /********************* k v redis start ********************************/
    /**
     * @param key
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        String value;
        if (factory.isCluster()) {
            value = callCluster(command -> command.get(key), null);
        } else {
            value = call(command -> command.get(key), null);
        }

        if (value == null) return null;
        if (clazz == String.class) return (T) value;
        return Jsons.fromJson(value, clazz);
    }

    public void set(String key, String value) {
        set(key, value, 0);
    }

    public void set(String key, Object value) {
        set(key, value, 0);
    }

    public void set(String key, Object value, int time) {
        set(key, Jsons.toJson(value), time);
    }

    /**
     * @param key
     * @param value
     * @param time  seconds
     */
    public void set(String key, String value, int time) {

        if (factory.isCluster()) {
            callCluster(command -> {
                command.set(key, value);
                if (time > 0) {
                    command.expire(key, time);
                }
            });
        } else {
            call(command -> {
                command.set(key, value);
                if (time > 0) {
                    command.expire(key, time);
                }
            });
        }
    }

    public void del(String key) {
        if (factory.isCluster()) {
            callCluster(command -> command.del(key));
        } else {
            call(command -> command.del(key));
        }
    }

    /********************* k v redis end ********************************/

    /*********************
     * hash redis start
     ********************************/
    public void hset(String key, String field, String value) {
        if (factory.isCluster()) {
            callCluster(command -> command.hset(key, field, value));
        } else {
            call(command -> command.hset(key, field, value));
        }
    }

    public void hset(String key, String field, Object value) {
        hset(key, field, Jsons.toJson(value));
    }

    @SuppressWarnings("unchecked")
    public <T> T hget(String key, String field, Class<T> clazz) {
        String value;
        if (factory.isCluster()) {
            value = callCluster(command -> command.hget(key, field), null);
        } else {
            value = call(command -> command.hget(key, field), null);
        }

        if (value == null) return null;
        if (clazz == String.class) return (T) value;
        return Jsons.fromJson(value, clazz);
    }

    public void hdel(String key, String field) {
        if (factory.isCluster()) {
            callCluster(command -> command.hdel(key, field));
        } else {
            call(command -> command.hdel(key, field));
        }
    }

    public Map<String, String> hgetAll(String key) {
        if (factory.isCluster()) {
            return callCluster(command -> command.hgetall(key), Collections.<String, String>emptyMap());
        } else {
            return call(command -> command.hgetall(key), Collections.<String, String>emptyMap());
        }
    }

    public <T> Map<String, T> hgetAll(String key, Class<T> clazz) {
        Map<String, String> result = hgetAll(key);
        if (result.isEmpty()) return Collections.emptyMap();
        Map<String, T> newMap = new HashMap<>(result.size());
        result.forEach((k, v) -> newMap.put(k, Jsons.fromJson(v, clazz)));
        return newMap;
    }

    /**
     * 返回 key 指定的哈希集中所有字段的名字。
     *
     * @param key
     * @return
     */
    public List<String> hkeys(String key) {
        if (factory.isCluster()) {
            return callCluster(command -> command.hkeys(key), Collections.<String>emptyList());
        } else {
            return call(command -> command.hkeys(key), Collections.<String>emptyList());
        }
    }

    /**
     * 返回 key 指定的哈希集中指定字段的值
     *
     * @param fields
     * @param clazz
     * @return
     */
    public <T> List<T> hmget(String key, Class<T> clazz, String... fields) {

        if (factory.isCluster()) {
            return callCluster(command -> command.hmget(key, fields), Collections.<String>emptyList())
                    .stream()
                    .map(s -> Jsons.fromJson((String)s, clazz))
                    .collect(Collectors.toList());
        } else {
            return call(command -> command.hmget(key, fields), Collections.<String>emptyList())
                    .stream()
                    .map(s -> Jsons.fromJson((String)s, clazz))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 设置 key 指定的哈希集中指定字段的值。该命令将重写所有在哈希集中存在的字段。如果 key 指定的哈希集不存在，会创建一个新的哈希集并与 key
     * 关联
     *
     * @param hash
     * @param time
     */
    public void hmset(String key, Map<String, String> hash, int time) {
        if (factory.isCluster()) {
            callCluster(command -> {
                command.hmset(key, hash);
                if (time > 0) {
                    command.expire(key, time);
                }
            });
        } else {
            call(command -> {
                command.hmset(key, hash);
                if (time > 0) {
                    command.expire(key, time);
                }
            });
        }
    }

    public void hmset(String key, Map<String, String> hash) {
        hmset(key, hash, 0);
    }

    public long hincrBy(String key, String field, long value) {
        if (factory.isCluster()) {
            return callCluster(command -> command.hincrby(key, field, value), 0L);
        } else {
            return call(command -> command.hincrby(key, field, value), 0L);
        }
    }

    /********************* hash redis end ********************************/

    /********************* list redis start ********************************/
    /**
     * 从队列的左边入队
     */
    public void lpush(String key, String... value) {
        if (factory.isCluster()) {
            callCluster(command -> command.lpush(key, value));
        } else {
            call(command -> command.lpush(key, value));
        }
    }

    public void lpush(String key, Object value) {
        lpush(key, Jsons.toJson(value));
    }

    /**
     * 从队列的右边入队
     */
    public void rpush(String key, String value) {
        if (factory.isCluster()) {
            callCluster(command -> command.lpush(key, value));
        } else {
            call(command -> command.lpush(key, value));
        }
    }

    public void rpush(String key, Object value) {
        rpush(key, Jsons.toJson(value));
    }

    /**
     * 移除并且返回 key 对应的 list 的第一个元素
     */
    @SuppressWarnings("unchecked")
    public <T> T lpop(String key, Class<T> clazz) {
        String value;

        if (factory.isCluster()) {
            value = callCluster(command -> command.lpop(key), null);
        } else {
            value = call(command -> command.lpop(key), null);
        }
        if (value == null) return null;
        if (clazz == String.class) return (T) value;
        return Jsons.fromJson(value, clazz);
    }

    /**
     * 从队列的右边出队一个元素
     */
    @SuppressWarnings("unchecked")
    public <T> T rpop(String key, Class<T> clazz) {
        String value;
        if (factory.isCluster()) {
            value = callCluster(command -> command.rpop(key), null);
        } else {
            value = call(command -> command.rpop(key), null);
        }
        if (value == null) return null;
        if (clazz == String.class) return (T) value;
        return Jsons.fromJson(value, clazz);
    }

    /**
     * 从列表中获取指定返回的元素 start 和 end
     * 偏移量都是基于0的下标，即list的第一个元素下标是0（list的表头），第二个元素下标是1，以此类推。
     * 偏移量也可以是负数，表示偏移量是从list尾部开始计数。 例如， -1 表示列表的最后一个元素，-2 是倒数第二个，以此类推。
     */
    public <T> List<T> lrange(String key, int start, int end, Class<T> clazz) {
        if (factory.isCluster()) {
            return callCluster(command -> command.lrange(key, start, end), Collections.<String>emptyList())
                    .stream()
                    .map(s -> Jsons.fromJson(s, clazz))
                    .collect(Collectors.toList());
        } else {
            return call(command -> command.lrange(key, start, end), Collections.<String>emptyList())
                    .stream()
                    .map(s -> Jsons.fromJson(s, clazz))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 返回存储在 key 里的list的长度。 如果 key 不存在，那么就被看作是空list，并且返回长度为 0。 当存储在 key
     * 里的值不是一个list的话，会返回error。
     */
    public long llen(String key) {
        if (factory.isCluster()) {
            return callCluster(command -> command.llen(key), 0L);
        } else {
            return call(command -> command.llen(key), 0L);
        }
    }

    /**
     * 移除表中所有与 value 相等的值
     *
     * @param key
     * @param value
     */
    public void lRem(String key, String value) {
        if (factory.isCluster()) {
            callCluster(command -> command.lrem(key, 0, value));
        } else {
            call(command -> command.lrem(key, 0, value));
        }
    }

    /********************* list redis end ********************************/

    /*********************
     * mq redis start
     ********************************/


    public void publish(String channel, Object message) {
        String msg = message instanceof String ? (String) message : Jsons.toJson(message);
        if (factory.isCluster()) {
            callCluster(command ->  command.publish(channel, msg));
        } else {
            call(command ->  command.publish(channel, msg));
        }
    }

    public void subscribe(final RedisPubSubListener pubsub, final String channel) {
        if (factory.isCluster()) {
            Utils.newThread(channel,
                    () -> callClusterPubSub(command -> {
                        command.getStatefulConnection().addListener(pubsub);
                        command.subscribe(channel);
                    })
            ).start();
        } else {
            Utils.newThread(channel,
                    () -> callPubSub(command -> {
                        command.getStatefulConnection().addListener(pubsub);
                        command.subscribe(channel);
                    })
            ).start();
        }
    }

    /*********************
     * set redis start
     ********************************/
    /**
     * @param key
     * @param value
     */
    public void sAdd(String key, String value) {
        if (factory.isCluster()) {
            callCluster(command -> command.sadd(key, value));
        } else {
            call(command -> command.sadd(key, value));
        }
    }

    /**
     * @param key
     * @return
     */
    public long sCard(String key) {
        if (factory.isCluster()) {
            return callCluster(command -> command.scard(key), 0L);
        } else {
            return call(command -> command.scard(key), 0L);
        }
    }

    public void sRem(String key, String value) {
        if (factory.isCluster()) {
            callCluster(command -> command.srem(key, value));
        } else {
            call(command -> command.srem(key, value));
        }
    }

    /**
     * 默认使用每页10个
     *
     * @param key
     * @param clazz
     * @return
     */
    public <T> List<T> sScan(String key, Class<T> clazz, int start) {
        List<String> list;

        if (factory.isCluster()) {
            list = call(command -> command.sscan(key, new ScanCursor("" + start, false), new ScanArgs().limit(10)).getValues(), null);
        } else {
            list = call(command -> command.sscan(key, new ScanCursor("" + start, false), new ScanArgs().limit(10)).getValues(), null);
        }
        return toList(list, clazz);
    }

    /*********************
     * sorted set
     ********************************/
    /**
     * @param key
     * @param value
     */
    public void zAdd(String key, String value) {
        if (factory.isCluster()) {
            callCluster(command -> command.zadd(key, 0, value));
        } else {
            call(command -> command.zadd(key, 0, value));
        }
    }

    /**
     * @param key
     * @return
     */
    public Long zCard(String key) {
        if (factory.isCluster()) {
            return callCluster(command -> command.zcard(key), 0L);
        } else {
            return call(command -> command.zcard(key), 0L);
        }
    }

    public void zRem(String key, String value) {
        if (factory.isCluster()) {
            callCluster(command -> command.zrem(key, value));
        } else {
            call(command -> command.zrem(key, value));
        }
    }

    /**
     * 从列表中获取指定返回的元素 start 和 end
     * 偏移量都是基于0的下标，即list的第一个元素下标是0（list的表头），第二个元素下标是1，以此类推。
     * 偏移量也可以是负数，表示偏移量是从list尾部开始计数。 例如， -1 表示列表的最后一个元素，-2 是倒数第二个，以此类推。
     */
    public <T> List<T> zrange(String key, int start, int end, Class<T> clazz) {
        List<String> value;

        if (factory.isCluster()) {
            value = callCluster(command -> command.zrange(key, start, end), null);
        } else {
            value = call(command -> command.zrange(key, start, end), null);
        }
        return toList(value, clazz);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> toList(Collection<String> value, Class<T> clazz) {
        if (value != null) {
            if (clazz == String.class) {
                return (List<T>) new ArrayList<>(value);
            }
            List<T> newValue = Lists.newArrayList();
            for (String temp : value) {
                newValue.add(Jsons.fromJson(temp, clazz));
            }
            return newValue;
        }
        return null;
    }

    public void destroy() {
        if (factory != null) factory.destroy();
    }

    public void test() {
        if (factory.isCluster()) {
            StatefulConnection cluster = factory.getClusterConnection();
            if (cluster == null) throw new RuntimeException("init redis cluster error.");
            cluster.close();
        } else {
            StatefulConnection command = factory.getRedisConnection();
            if (command == null) throw new RuntimeException("init redis error, can not get connection.");
            command.close();
        }
    }

}
