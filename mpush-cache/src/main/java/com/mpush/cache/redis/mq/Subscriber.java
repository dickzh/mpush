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

package com.mpush.cache.redis.mq;

import com.mpush.tools.log.Logs;
import io.lettuce.core.pubsub.RedisPubSubAdapter;

public final class Subscriber extends RedisPubSubAdapter<String, String> {
    private final ListenerDispatcher listenerDispatcher;

    public Subscriber(ListenerDispatcher listenerDispatcher) {
        this.listenerDispatcher = listenerDispatcher;
    }

    @Override
    public void message(String channel, String message) {
        Logs.CACHE.info("onMessage:{},{}", channel, message);
        listenerDispatcher.onMessage(channel, message);
        super.message(channel, message);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        Logs.CACHE.info("onPMessage:{},{},{}", pattern, channel, message);
        super.message(pattern, channel, message);
    }

    @Override
    public void psubscribed(String pattern, long subscribedChannels) {
        Logs.CACHE.info("onPSubscribe:{},{}", pattern, subscribedChannels);
        super.psubscribed(pattern, subscribedChannels);
    }

    @Override
    public void punsubscribed(String pattern, long subscribedChannels) {
        Logs.CACHE.info("onPUnsubscribe:{},{}", pattern, subscribedChannels);
        super.punsubscribed(pattern, subscribedChannels);
    }

    @Override
    public void subscribed(String channel, long subscribedChannels) {
        Logs.CACHE.info("onSubscribe:{},{}", channel, subscribedChannels);
        super.subscribed(channel, subscribedChannels);
    }

    @Override
    public void unsubscribed(String channel, long subscribedChannels) {
        Logs.CACHE.info("onUnsubscribe:{},{}", channel, subscribedChannels);
        super.unsubscribed(channel, subscribedChannels);
    }


}
