/*
 * Copyright (C) 2018 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.platform.resolver;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

class DirectiveUtil {

    static DirectiveHeader parseHeader(Gson gson, JsonObject directive) {
        final DirectiveHeader header = gson.fromJson(directive.get("header"), DirectiveHeader.class);
        if (header == null) {
            return null;
        }

        if (TextUtils.isEmpty(header.namespace) || TextUtils.isEmpty(header.name)
                || TextUtils.isEmpty(header.messageId)) {
            return null;
        }

        return header;
    }

}