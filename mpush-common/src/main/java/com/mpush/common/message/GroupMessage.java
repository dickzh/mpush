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

package com.mpush.common.message;

import com.mpush.api.connection.Connection;
import com.mpush.api.protocol.JsonPacket;
import com.mpush.api.protocol.Packet;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

import static com.mpush.api.protocol.Command.GROUP;

/**
 * Created by ohun on 2015/12/29.
 *
 * @author ohun@live.cn
 */
public class GroupMessage extends ByteBufMessage {
    public String deviceId;
    public String userId;
    public String groupId;
    public String msgType;

    public GroupMessage(Packet message, Connection connection) {
        super(message, connection);
    }

    public static GroupMessage build(Connection connection) {
        if (connection.getSessionContext().isSecurity()) {
            return new GroupMessage(new Packet(GROUP), connection);
        } else {
            return new GroupMessage(new JsonPacket(GROUP), connection);
        }
    }

    @Override
    public void decode(ByteBuf body) {
        deviceId = decodeString(body);
        userId = decodeString(body);
        groupId = decodeString(body);
        msgType = decodeString(body);
    }

    @Override
    public void encode(ByteBuf body) {
        encodeString(body, deviceId);
        encodeString(body, userId);
        encodeString(body, groupId);
        encodeString(body, msgType);
    }

    @Override
    protected Map<String, Object> encodeJsonBody() {
        Map<String, Object> body = new HashMap<>(3);
        body.put("deviceId", deviceId);
        body.put("userId", userId);
        body.put("groupId", groupId);
        body.put("msgType", msgType);
        return body;
    }

    @Override
    public String toString() {
        return "KickUserMessage{" +
                "deviceId='" + deviceId + '\'' +
                ", userId='" + userId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", msgType='" + msgType + '\'' +
                '}';
    }
}
