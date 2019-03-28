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

package com.mpush.api;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by ohun on 2015/12/23.
 *
 * @author ohun@live.cn
 */
public class Constants {
    public final static Charset UTF_8 = StandardCharsets.UTF_8;
    public final static byte[] EMPTY_BYTES = new byte[0];
    public final static String HTTP_HEAD_READ_TIMEOUT = "readTimeout";
    public final static String EMPTY_STRING = "";
    public final static String ANY_HOST = "0.0.0.0";
    public final static String KICK_CHANNEL_PREFIX = "/mpush/kick/";
    public final static String ONLINE_CHANNEL = "/mpush/online/";
    public final static String OFFLINE_CHANNEL = "/mpush/offline/";

    public static String getKickChannel(String hostAndPort) {
        return KICK_CHANNEL_PREFIX + hostAndPort;
    }

}
