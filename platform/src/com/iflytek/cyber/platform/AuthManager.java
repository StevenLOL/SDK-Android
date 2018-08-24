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

package com.iflytek.cyber.platform;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iflytek.cyber.platform.internal.retrofit2.SimpleCallback;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public class AuthManager {

    private static final String TAG = "AuthManager";

    private static final String SCOPE_IVS_ALL = "user_ivs_all";
    private static final String GRANT_DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code";
    private static final String GRANT_REFRESH_TOKEN = "refresh_token";

    private final String deviceId;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());

    private AuthApi api;
    private String clientId;

    private Call<DeviceCode> deviceCodeCall = null;
    private Call<Token> pollingCall = null;

    private Call<Token> refreshCall = null;

    public AuthManager(String clientId, String deviceId) {
        setEndpoint(BuildConfig.API_SERVER);
        this.clientId = clientId;
        this.deviceId = deviceId;
    }

    public void setEndpoint(String endpoint) {
        this.api = new ApiFactory(endpoint).createApi(AuthApi.class);
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void authorize(AuthorizeCallback callback) {
        if (deviceCodeCall != null) {
            deviceCodeCall.cancel();
            deviceCodeCall = null;
        }

        if (pollingCall != null) {
            pollingCall.cancel();
            pollingCall = null;
        }

        pollingHandler.removeCallbacksAndMessages(null);

        ScopeData scope = new ScopeData();
        scope.userIvsAll = new ScopeData.UserIvsAll();
        scope.userIvsAll.deviceId = deviceId;

        String scopeData = new Gson().toJson(scope);

        deviceCodeCall = api.getDeviceCode(clientId, SCOPE_IVS_ALL, scopeData);
        deviceCodeCall.enqueue(new SimpleCallback<DeviceCode>() {
            @Override
            public void onSuccess(DeviceCode body, Response<DeviceCode> response) {
                Log.d(TAG, "Request auth " + body.deviceCode + " " + body.userCode);

                deviceCodeCall = null;
                uiHandler.post(() -> callback.onPromptDeviceCode(body.verificationUri, body.userCode));
                pollingHandler.postDelayed(() -> pollToken(body.deviceCode, body.interval, callback),
                        TimeUnit.SECONDS.toMillis(body.interval));
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, Response<DeviceCode> response) {
                deviceCodeCall = null;
                uiHandler.post(() -> callback.onFailure(
                        new Error("Get device token failed with code " + code + " - " + body)));
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                if (deviceCodeCall != null) {
                    deviceCodeCall = null;
                    uiHandler.post(() -> callback.onFailure(t));
                }
            }
        });
    }

    private void pollToken(String deviceCode, long interval, AuthorizeCallback callback) {
        pollingCall = api.pollToken(clientId, GRANT_DEVICE_CODE, deviceCode);
        pollingCall.enqueue(new SimpleCallback<Token>() {
            @Override
            public void onSuccess(Token body, Response<Token> response) {
                Log.d(TAG, "Polling token " + deviceCode + " succeed");

                pollingCall = null;
                uiHandler.post(() -> callback.onGetToken(body.accessToken, body.refreshToken,
                        body.createdAt + body.expiresIn));
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, Response<Token> response) {
                Log.d(TAG, "Polling token " + deviceCode + "... " + code + " " + body);

                pollingCall = null;

                if (code != 400) {
                    uiHandler.post(() -> callback.onFailure(
                            new Error("Poll auth token failed with code " + code)));
                    return;
                }

                if (body == null) {
                    uiHandler.post(() -> callback.onFailure(
                            new Error("Poll auth token failed with code " + code)));
                    return;
                }

                final String error = body.get("error").getAsString();
                if (!"authorization_pending".equals(error)) {
                    uiHandler.post(callback::onReject);
                    return;
                }

                pollingHandler.postDelayed(() -> pollToken(deviceCode, interval, callback),
                        TimeUnit.SECONDS.toMillis(interval));
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                Log.e(TAG, "Polling token " + deviceCode + " failed", t);

                if (pollingCall != null) {
                    pollingCall = null;
                    uiHandler.post(() -> callback.onFailure(t));
                }
            }
        });
    }

    public void refresh(String refreshToken, RefreshCallback callback) {
        if (refreshCall != null) {
            refreshCall.cancel();
            refreshCall = null;
        }

        refreshCall = api.refreshToken(clientId, GRANT_REFRESH_TOKEN, refreshToken);
        refreshCall.enqueue(new SimpleCallback<Token>() {
            @Override
            public void onSuccess(Token body, Response<Token> response) {
                refreshCall = null;
                uiHandler.post(() -> callback.onGetToken(body.accessToken, body.refreshToken,
                        body.createdAt + body.expiresIn));
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, Response<Token> response) {
                refreshCall = null;
                if (code == 401) {
                    uiHandler.post(callback::onAuthExpired);
                } else {
                    uiHandler.post(() -> callback.onFailure(
                            new Error("Refresh auth token failed with code " + code)));
                }
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                if (refreshCall != null) {
                    refreshCall = null;
                    uiHandler.post(() -> callback.onFailure(t));
                }
            }
        });
    }

    public void cancel() {
        Log.d(TAG, "Cancel");

        if (deviceCodeCall != null) {
            deviceCodeCall.cancel();
            deviceCodeCall = null;
        }

        if (pollingCall != null) {
            pollingCall.cancel();
            pollingCall = null;
        }

        if (refreshCall != null) {
            refreshCall.cancel();
            refreshCall = null;
        }

        uiHandler.removeCallbacksAndMessages(null);
        pollingHandler.removeCallbacksAndMessages(null);
    }

    public interface AuthorizeCallback {

        void onPromptDeviceCode(String verificationUri, String userCode);

        void onGetToken(String accessToken, String refreshToken, long expiresAt);

        void onReject();

        void onFailure(Throwable t);

    }

    public interface RefreshCallback {

        void onGetToken(String accessToken, String refreshToken, long expiresAt);

        void onAuthExpired();

        void onFailure(Throwable t);

    }

    private interface AuthApi {
        @FormUrlEncoded
        @POST("/oauth/ivs/device_code")
        Call<DeviceCode> getDeviceCode(
                @Field("client_id") String clientId,
                @Field("scope") String scope,
                @Field("scope_data") String scopeData);

        @FormUrlEncoded
        @POST("/oauth/ivs/token")
        Call<Token> pollToken(
                @Field("client_id") String clientId,
                @Field("grant_type") String grantType,
                @Field("device_code") String deviceCode);

        @FormUrlEncoded
        @POST("/oauth/ivs/token")
        Call<Token> refreshToken(
                @Field("client_id") String clientId,
                @Field("grant_type") String grantType,
                @Field("refresh_token") String refreshToken);
    }

    private class DeviceCode {
        public String verificationUri;
        public String userCode;
        public String deviceCode;
        public int interval;
    }

    private class Token {
        public String refreshToken;
        public String accessToken;
        public long createdAt;
        public long expiresIn;
    }

}
