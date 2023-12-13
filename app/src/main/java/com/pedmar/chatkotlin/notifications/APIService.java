package com.pedmar.chatkotlin.notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;


public interface APIService {

    @Headers({
            "Content-Type: application/json",
            "Authorization: key=AAAAJrje2yE:APA91bGe9Viufp9J1FYNU-wH3NEO5n6lno6YIz6bdarSVVDLx2HTPxpLAZZQGlpYhEv-ENGlT8PqWALrmbr2Me_g6kbhBiAuEOw6N1MSCFZg5VAPpQXir2ThzH2bNOeeMEMDwGerubey"
    })
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);

}
