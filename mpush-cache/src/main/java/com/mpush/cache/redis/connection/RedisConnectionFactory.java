/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mpush.cache.redis.connection;

import com.mpush.tools.config.data.RedisNode;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Connection factory creating <a href="http://github.com/xetorthio/redis">redis</a> based connections.
 *
 * @author Costin Leau
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class RedisConnectionFactory {

    private final static Logger log = LoggerFactory.getLogger(RedisConnectionFactory.class);

    private String hostName = "localhost";
    private int port = RedisURI.DEFAULT_REDIS_PORT;
    private long timeout = RedisURI.DEFAULT_TIMEOUT;
    private String password;

    private String sentinelMaster;
    private List<RedisNode> redisServers;
    private boolean isCluster = false;
    private boolean isSentinel = false;
    private int dbIndex = 0;

    private AbstractRedisClient redisClient;
    private GenericObjectPool<StatefulConnection<String, String>> pool;
    private GenericObjectPool<StatefulConnection<String, String>> pubsubPool;
    private GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

    /**
     * Constructs a new <code>redisConnectionFactory</code> instance with default settings (default connection pooling, no
     * shard information).
     */
    public RedisConnectionFactory() {
    }

    /**
     * Returns a redis instance to be used as a Redis connection. The instance can be newly created or retrieved from a
     * pool.
     */
    protected StatefulConnection<String, String> fetchRedisConnection() {
        try {
            if (pool != null) {
                return pool.borrowObject();
            }

            if (isCluster) {
                createRedisClusterPool();
            } else {
                createPool();
            }

            return pool.borrowObject();

        } catch (Exception ex) {
            throw new RuntimeException("Cannot get redis connection", ex);
        }
    }

    protected StatefulConnection<String, String> fetchRedisPubSubConnection() {
        try {
            if (pubsubPool != null) {
                return pubsubPool.borrowObject();
            }

            if (isCluster) {
                createRedisClusterPool();
            } else {
                createPool();
            }

            return pubsubPool.borrowObject();

        } catch (Exception ex) {
            throw new RuntimeException("Cannot get redis connection", ex);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void init() {

        if (isCluster) {
            if (redisClient == null) {
                List<RedisURI> nodeList = new ArrayList<>();
                for (RedisNode node : redisServers) {
                    RedisURI.Builder builder = RedisURI.builder().redis(node.getHost(), node.getPort());

                    builder = builderOptions(builder);
                    nodeList.add(builder.build());
                }

                redisClient = RedisClusterClient.create(nodeList);
            }

            this.pool = createRedisClusterPool();

        } else {
            if (redisClient == null) {
                RedisURI.Builder builder = RedisURI.builder().redis(hostName, port);

                builder =  builderOptions(builder);
                redisClient = RedisClient.create(builder.build());
            }

            this.pool = createPool();
        }
    }

    private RedisURI.Builder builderOptions(RedisURI.Builder builder){
        if (StringUtils.isNotEmpty(password)) {
            builder = builder.withPassword(password);
        }

        if (timeout > 0) {
            builder = builder.withTimeout(Duration.ofSeconds(timeout));
        }

        if (dbIndex > 0) {
            builder = builder.withDatabase(dbIndex);
        }

        return builder;
    }

    private GenericObjectPool<StatefulConnection<String, String>> createPool() {
        if (isSentinel) {
            return createRedisSentinelPool();
        }
        return createRedisPool();
    }

    /**
     * Creates {@link GenericObjectPool}.
     *
     * @return
     * @since 1.4
     */
    protected GenericObjectPool<StatefulConnection<String, String>> createRedisClusterPool() {
        if(redisClient == null) {
            List<RedisURI> nodeList = new ArrayList<>();
            for (RedisNode node : redisServers) {
                RedisURI.Builder builder = RedisURI.builder().redis(node.getHost(), node.getPort());

                builder =  builderOptions(builder);
                nodeList.add(builder.build());
            }

            redisClient = RedisClusterClient.create(nodeList);
        }

        if(pool == null) {
            pool = ConnectionPoolSupport
                    .createGenericObjectPool(() -> ((RedisClusterClient) redisClient).connect(), poolConfig);
            preparePool(pool);

        }
        if(pubsubPool == null) {
            pubsubPool = ConnectionPoolSupport
                    .createGenericObjectPool(() -> ((RedisClusterClient) redisClient).connectPubSub(), poolConfig);


            preparePool(pubsubPool);
        }
        return pool;
    }


    /**
     * Creates {@link GenericObjectPool}.
     *
     * @return
     * @since 1.4
     */
    protected GenericObjectPool<StatefulConnection<String, String>> createRedisPool() {
        if (redisClient == null) {
            RedisURI.Builder builder = RedisURI.builder().redis(hostName, port);

            builder =  builderOptions(builder);
            redisClient = RedisClient.create(builder.build());
        }

        if(pool == null) {
            pool = ConnectionPoolSupport
                    .createGenericObjectPool(() -> ((RedisClient) redisClient).connect(), poolConfig);
            preparePool(pool);
        }

        if(pubsubPool == null) {
            pubsubPool = ConnectionPoolSupport
                    .createGenericObjectPool(() -> ((RedisClient) redisClient).connectPubSub(), poolConfig);
            preparePool(pubsubPool);
        }
        return pool;
    }

    protected GenericObjectPool<StatefulConnection<String, String>> createRedisSentinelPool() {
        if (redisClient == null) {
            RedisURI.Builder builder = RedisURI.Builder.sentinel(sentinelMaster, port);
            for (RedisNode node : redisServers) {
                builder = builder.withSentinel(node.getHost(), node.getPort());
            }
            builder =  builderOptions(builder);
            redisClient = RedisClient.create(builder.build());
        }

        if(pool == null) {
            pool = ConnectionPoolSupport
                    .createGenericObjectPool(() -> ((RedisClient) redisClient).connect(), poolConfig);
            preparePool(pool);
        }

        if(pubsubPool == null) {
            pubsubPool = ConnectionPoolSupport
                    .createGenericObjectPool(() -> ((RedisClient) redisClient).connectPubSub(), poolConfig);
            preparePool(pubsubPool);
        }
        return pool;
    }

    private void preparePool(GenericObjectPool<StatefulConnection<String, String>> pool){
        try {
            pool.preparePool();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() {
        if (pool != null) {
            try {
                pool.close();
            } catch (Exception ex) {
                log.warn("Cannot properly close redis pool", ex);
            }
            pool = null;
        }

        if (pubsubPool != null) {
            try {
                pubsubPool.close();
            } catch (Exception ex) {
                log.warn("Cannot properly close redis pool", ex);
            }
            pubsubPool = null;
        }

        if (redisClient != null) {
            try {
                redisClient.shutdown();
            } catch (Exception ex) {
                log.warn("Cannot properly close redis clusterClient", ex);
            }
            redisClient = null;
        }

    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.redis.connection.RedisConnectionFactory#getConnection()
     */
    public StatefulRedisConnection getRedisConnection() {
        return (StatefulRedisConnection) fetchRedisConnection();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.redis.connection.RedisConnectionFactory#getClusterConnection()
     */
    public StatefulRedisClusterConnection getClusterConnection() {
        return (StatefulRedisClusterConnection) fetchRedisConnection();
    }


    /*
     * (non-Javadoc)
     * @see org.springframework.data.redis.connection.RedisConnectionFactory#getConnection()
     */
    public StatefulRedisPubSubConnection getRedisPubSubConnection() {
        return (StatefulRedisPubSubConnection) fetchRedisPubSubConnection();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.redis.connection.RedisConnectionFactory#getClusterConnection()
     */
    public StatefulRedisClusterPubSubConnection getClusterPubSubConnection() {
        return (StatefulRedisClusterPubSubConnection) fetchRedisPubSubConnection();
    }

    public boolean isCluster() {
        return isCluster;
    }

    /**
     * Returns the Redis hostName.
     *
     * @return Returns the hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets the Redis hostName.
     *
     * @param hostName The hostName to set.
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Returns the password used for authenticating with the Redis server.
     *
     * @return password for authentication
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password used for authenticating with the Redis server.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the port used to connect to the Redis instance.
     *
     * @return Redis port.
     */
    public int getPort() {
        return port;

    }

    /**
     * Sets the port used to connect to the Redis instance.
     *
     * @param port Redis port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the redisClient.
     *
     * @return Returns the redisClient
     */
    public RedisClient getRedisClient() {
        return (RedisClient)redisClient;
    }

    /**
     * Sets the shard info for this factory.
     *
     * @param redisClient The redisClient to set.
     */
    public void setRedisClient(RedisClient redisClient) {
        this.redisClient = redisClient;
    }


    /**
     * Returns the clusterClient.
     *
     * @return Returns the clusterClient
     */
    public RedisClusterClient getClusterClient() {
        return (RedisClusterClient)redisClient;
    }

    /**
     * Sets the shard info for this factory.
     *
     * @param clusterClient The clusterClient to set.
     */
    public void setClusterClient(RedisClusterClient clusterClient) {
        this.redisClient = clusterClient;
    }

    /**
     * Returns the timeout.
     *
     * @return Returns the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @param timeout The timeout to set.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns the poolConfig.
     *
     * @return Returns the poolConfig
     */
    public GenericObjectPoolConfig getPoolConfig() {
        return poolConfig;
    }

    /**
     * Sets the pool configuration for this factory.
     *
     * @param poolConfig The poolConfig to set.
     */
    public void setPoolConfig(GenericObjectPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

    /**
     * Returns the index of the database.
     *
     * @return Returns the database index
     */
    public int getDatabase() {
        return dbIndex;
    }

    /**
     * Sets the index of the database used by this connection factory. Default is 0.
     *
     * @param index database index
     */
    public void setDatabase(int index) {
        this.dbIndex = index;
    }

    public void setCluster(boolean cluster) {
        isCluster = cluster;
    }

    public void setSentinel(boolean sentinel) {
        isSentinel = sentinel;
    }

    public void setSentinelMaster(String sentinelMaster) {
        this.sentinelMaster = sentinelMaster;
    }

    public void setRedisServers(List<RedisNode> redisServers) {
        if (redisServers == null || redisServers.isEmpty()) {
            throw new IllegalArgumentException("redis server node can not be empty, please check your conf.");
        }
        this.redisServers = redisServers;
        this.hostName = redisServers.get(0).getHost();
        this.port = redisServers.get(0).getPort();
    }

    public GenericObjectPool<StatefulConnection<String, String>> getPool() {
        return pool;
    }

    public GenericObjectPool<StatefulConnection<String, String>> getPubsubPool() {
        return pubsubPool;
    }
}
