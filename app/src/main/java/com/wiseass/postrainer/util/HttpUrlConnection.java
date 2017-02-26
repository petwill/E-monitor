package com.wiseass.postrainer.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.StringTokenizer;
import android.content.Context;

/**
 * Created by user on 2017/2/23.
 */

public class HttpUrlConnection {
    private String deviceId, deviceKey, dataChannel;
    private Context mContext;
    public HttpUrlConnection(String device_id, String device_key, String channel, Context cc) {
        deviceId = device_id;
        deviceKey = device_key;
        dataChannel = channel;
        mContext = cc;
    }


    //private final String USER_AGENT = "Mozilla/5.0";
    // HTTP GET request
    public String getData() {


        int responseCode = 0;
        String ans = "-1";
        try {


            String url = ("https://api.mediatek.com/mcs/v2/devices/" + deviceId + "/datachannels/" + dataChannel +"/datapoints");
            Log.d("debug", url);

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // optional default is GET
            con.setRequestMethod("GET");
            //add request header
            //con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("deviceKey", deviceKey);


            responseCode = con.getResponseCode();

            //System.out.println("\nSending 'GET' request to URL : " + url);
            //System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine = in.readLine();
            //System.out.println(inputLine);
            StringTokenizer token = new StringTokenizer(inputLine, ":[]{}");

            while(token.hasMoreElements())
                ans = token.nextToken();

            return ans;


        } catch (Exception e) {
            e.printStackTrace();
            return Integer.toString(responseCode);
        }
        //return Integer.toString(responseCode);
            /* for multi-line response
          StringBuffer response = new StringBuffer();
          while ((inputLine = in.readLine()) != null) {
             response.append(inputLine);
          }
          in.close();
          //print result
          System.out.println(response.toString());
            */
    }




}
