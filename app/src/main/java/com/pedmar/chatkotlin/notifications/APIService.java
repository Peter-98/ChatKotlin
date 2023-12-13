package com.pedmar.chatkotlin.notifications;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.io.IOException;

public interface APIService {

    @Headers({
            "Content-Type: application/json",
            "Authorization: key=${readAuthorizationFromConfig()}"
    })
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);

    default String readAuthorizationFromConfig() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("file:///path/to/config.json")
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                // Busca la cadena "authorization" en el JSON y extrae el valor
                int index = responseBody.indexOf("\"authorization\"");
                if (index != -1) {
                    int startIndex = responseBody.indexOf("\"", index + 15);
                    int endIndex = responseBody.indexOf("\"", startIndex + 1);
                    return responseBody.substring(startIndex + 1, endIndex);
                } else {
                    // Si no se encuentra la clave "authorization"
                    return "no se encuentra la clave authorization";
                }
            } else {
                // Maneja el caso de error si la solicitud no es exitosa
                return "APIService solicitud no es exitosa";
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Maneja la excepción si hay un error de IO
            return "Error en APIService.java";
        }
    }
}
