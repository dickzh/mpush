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

package com.mpush.api.protocol;

/**
 * Created by ohun on 2015/12/22.
 *
 * @author ohun@live.cn
 */
public enum GroupCmdType {
    CREATE(1),
    JOIN(2),
    LEFT(3),
    KICK(4),
    DESTROY(5),
    UNKNOWN(-1);

    GroupCmdType(int type) {
        this.type = (byte) type;
    }

    public final byte type;

    public static GroupCmdType toType(byte b) {
        GroupCmdType[] values = values();
        if (b > 0 && b < values.length) {
            return values[b - 1];
        }
        return UNKNOWN;
    }
}
