package com.transnacala.lory.data.remote;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.transnacala.lory.ui.auth.LoginActivity;
import com.transnacala.lory.utils.BooleanTypeAdapter;
import com.transnacala.lory.utils.Constants;
import com.transnacala.lory.utils.LongTypeAdapter;
import com.transnacala.lory.utils.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit = null;

    public static synchronized Retrofit getClient(Context context) {
        if (retrofit == null) {
            Context appContext = context.getApplicationContext();
            SessionManager session = new SessionManager(appContext);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();

                        String token = session.getToken();
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        Response response = chain.proceed(builder.build());

                        // Global 401 handling (ignora se o erro 401 for da própria tentativa de login)
                        if (response.code() == 401 && !original.url().encodedPath().contains("/auth/login")) {
                            session.logout();
                            Intent intent = new Intent(appContext, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.putExtra("SESSION_EXPIRED", true);
                            appContext.startActivity(intent);
                        }

                        return response;
                    })
                    .build();

            Gson customGson = new GsonBuilder()
                    .registerTypeAdapter(long.class, new LongTypeAdapter())
                    .registerTypeAdapter(Long.class, new LongTypeAdapter())
                    .registerTypeAdapter(boolean.class, new BooleanTypeAdapter())
                    .registerTypeAdapter(Boolean.class, new BooleanTypeAdapter())
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(customGson))
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService(Context context) {
        return getClient(context).create(ApiService.class);
    }

    public static void resetClient() {
        retrofit = null;
    }
}