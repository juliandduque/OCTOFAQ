package com.bradleybossard.speech_to_textdemo;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.regions.Regions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends ActionBarActivity
{
    private EditText URLS;
    private Button submitURL;
    private TextView statusLabel;
    private String CREATE_SERVICE_URL = "https://westus.api.cognitive.microsoft.com/qnamaker/v2.0/knowledgebases/create?";
    private String UPDATE_SERVICE_URL = "https://westus.api.cognitive.microsoft.com/qnamaker/v2.0/knowledgebases/";
    private String REQUEST_URL = "https://westus.api.cognitive.microsoft.com/qnamaker/v2.0/knowledgebases/";
    private String SERVICE_KEY = "ca294f56e7124392bc34eeffdd2f8d67";
    private String SERVICE_NAME = "OCTOFAQ";
    private LambdaInterface lambdaInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        URLS = (EditText) findViewById(R.id.URLs);
        submitURL = (Button) findViewById(R.id.SUBMIT_URL);
        statusLabel = (TextView) findViewById(R.id.statusLabel);

        submitURL.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if(submitURL.getText() != null && submitURL.getText() != "")
                {
                    submitURL.setEnabled(false);
                    URLS.setEnabled(false);

                    new UrlFetchTask().execute(URLS.getText().toString());
                }
            }
        });

        // Get credentials for AWS Lambda
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),    /* get the context for the application */
                "us-east-1:d958e476-855c-43b8-bd37-0e4bbc72c3f1",    /* Identity Pool ID */
                Regions.US_EAST_1           /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );
        // Initialize LambdaInvokerFactory
        LambdaInvokerFactory factory = new LambdaInvokerFactory(
                MainActivity.this.getApplicationContext(),
                Regions.US_EAST_1,
                credentialsProvider);

        lambdaInterface = factory.build(LambdaInterface.class);
    }

    class UrlFetchTask extends AsyncTask<String, Void, String> {

        private Exception exception;
        private String baseUrl;

        @Override
        protected String doInBackground(String... urls) {
            //Under assumption user only sends one url
            String url = urls[0].toString();
            if(url.indexOf("http") == -1) url = "http://" + url;
            if(url.lastIndexOf("/") >= url.lastIndexOf(".")) url += "/";
            baseUrl = url;
            FaqUrlsRequest faqUrlsRequest = new FaqUrlsRequest(url, "100");
            Log.e("DEBUG", "base url : " + faqUrlsRequest.getBaseUrl());
            return lambdaInterface.OctoFaqLinksRequest(faqUrlsRequest);
        }

        @Override
        protected void onPostExecute(String data)
        {
            String[] urls;
            if(data.length() == 0){
                urls = new String[1];
                urls[0] = baseUrl;
            } else {
                urls = data.split("\n");
            }
            StringBuilder sb = new StringBuilder();
            for(int cnt = 0; cnt < urls.length; cnt++){
                sb.append(urls[cnt]);
                sb.append(",");
            }
            String urlsString = sb.toString();
            if(urlsString.lastIndexOf(",") == urlsString.length() - 1) urlsString = urlsString.substring(0, urlsString.length() - 1);
            new CreateQnaService().execute(urlsString);
        }
    }

    class CreateQnaService extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... urls) {
            String responseCode = "";
            try {
                if("".equals(urls[0].trim())) return "404:";
                // Create connection
                URL url = new URL(CREATE_SERVICE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("Ocp-Apim-Subscription-Key", SERVICE_KEY);
                connection.setRequestProperty("Content-Type", "application/json");

                // Create request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("name", SERVICE_NAME);

                // Write request body
                OutputStream os = connection.getOutputStream();
                os.flush();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                // Get response code
                responseCode = connection.getResponseCode() + "";

                // Get kbId
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line, newjson = "";
                while ((line = reader.readLine()) != null) {
                    newjson += line;
                }
                JSONObject connectionResults = new JSONObject(newjson);
                REQUEST_URL += connectionResults.getString("kbId") + "/generateAnswer";
                UPDATE_SERVICE_URL += connectionResults.getString("kbId");
                Log.e("DEBUG", "kbId : " + connectionResults.getString("kbId"));
            } catch (Exception e) {
                responseCode = "400";
                e.printStackTrace();
            }

            return responseCode + ":" + urls[0];
        }

        @Override
        protected void onPostExecute(String returned)
        {
            Log.e("DEBUG", "returned : " + returned);
            if (returned.startsWith("201")) { //success
                statusLabel.setText("Service created successfully");
                String urlString = returned.substring(returned.indexOf(":")+1);
                Log.e("DEBUG", "url to update : " + urlString);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new UpdateQnaService().execute(urlString);
            } else {
                statusLabel.setText("Service creation failed");
                submitURL.setEnabled(true);
                URLS.setEnabled(true);
            }
        }
    }

    class UpdateQnaService extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... urls) {
            String responseCode, toReturn = "";
            try {
                Log.e("DEBUG", "update urls : " + urls[0]);
                String[] urlsArray = urls[0].toString().split(",");
                ArrayList<String> urlsList = new ArrayList<String>();

                // Add a max of 5 urls
                int numberOfUrlsToAdd = Math.min(urlsArray.length, 5);
                for (int cnt = 0; cnt < numberOfUrlsToAdd; cnt++) {
                    String url = urlsArray[cnt];
                    if(!url.startsWith("http") && !url.startsWith("https")) url = "http://" + url;
                    if(url.lastIndexOf("/") >= url.lastIndexOf(".")) url += "/";
                    urlsList.add(url);
                }
                // Store remaining urls
                StringBuilder toReturnBuilder = new StringBuilder();
                for(int cnt = 5; cnt < urlsArray.length; cnt++){
                    toReturnBuilder.append(",");
                    toReturnBuilder.append(urlsArray[cnt]);
                }
                toReturn = toReturnBuilder.toString();
                if(toReturn.length() != 0 && toReturn.startsWith(",")) toReturn = toReturn.substring(1);

                // Create request body
                JSONObject urlsJsonObject = new JSONObject();
                urlsJsonObject.put("urls", new JSONArray(urlsList));
                JSONObject requestBody = new JSONObject();
                requestBody.put("add", urlsJsonObject);
                Log.e("DEBUG", requestBody.toString());

                // Connect to service
                URL url = new URL(UPDATE_SERVICE_URL);
                Log.e("DEBUG", UPDATE_SERVICE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                connection.setRequestMethod("PATCH");
//                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("Ocp-Apim-Subscription-Key", SERVICE_KEY);
                connection.setRequestProperty("Content-Type", "application/json");

                OutputStream os = connection.getOutputStream();

//                os.flush();
                os.write(requestBody.toString().getBytes("UTF-8"));
//                os.flush();
                os.close();

                responseCode = connection.getResponseCode() + "";
                Log.e("DEBUG", "responseCode : " + responseCode);
                Log.e("DEBUG", "responseMessage : " + connection.getResponseMessage());
            } catch (Exception e) {
                responseCode = "400";
                e.printStackTrace();
            }

            return responseCode + ":" + toReturn;
        }

        @Override
        protected void onPostExecute(String returned)
        {
            Log.e("DEBUG", "returned : " + returned);
            if (returned.startsWith("204")) { //success
                statusLabel.setText("Added urls successfully");
                String urlsString = returned.substring(returned.indexOf(":")+1);
                if(urlsString.length() > 0) { // still have urls to add
                    new UpdateQnaService().execute(urlsString);
                } else {
                    new PublishQnaService().execute();
                }
            } else {
                statusLabel.setText("Something wrong happened :( Try again later or check your URL!");
                submitURL.setEnabled(true);
                URLS.setEnabled(true);
            }
        }
    }

    class PublishQnaService extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... urls) {
            String responseCode = "";
            try {
                // Connect to service
                URL url = new URL(UPDATE_SERVICE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("Ocp-Apim-Subscription-Key", SERVICE_KEY);

                OutputStream os = connection.getOutputStream();

                responseCode = connection.getResponseCode() + "";
            } catch (Exception e) {
                responseCode = "400";
                e.printStackTrace();
            }

            return responseCode;
        }

        @Override
        protected void onPostExecute(String returned)
        {
            Log.e("DEBUG", "returned : " + returned);
            if (returned.startsWith("204")) { //success
                statusLabel.setText("Published urls successfully");

                try {
                    Intent intent = new Intent(MainActivity.this, Chat.class);
                    Log.e("DEBUG", "REQUEST_URL : " + REQUEST_URL);
                    intent.putExtra("EXTRAMESSAGE", REQUEST_URL);
                    intent.putExtra("SERVICE_KEY", SERVICE_KEY);
                    submitURL.setEnabled(true);
                    URLS.setEnabled(true);
                    startActivity(intent);
                } catch (Exception e) {
                    statusLabel.setText("Tried to move to chat");
                    submitURL.setEnabled(true);
                    URLS.setEnabled(true);
                }
                REQUEST_URL = "https://westus.api.cognitive.microsoft.com/qnamaker/v2.0/knowledgebases/";
                UPDATE_SERVICE_URL = "https://westus.api.cognitive.microsoft.com/qnamaker/v2.0/knowledgebases/";
                String urlsString = returned.substring(returned.indexOf(":")+1);
            } else {
                statusLabel.setText("Something wrong happened :( Try again later or check your URL!");
                submitURL.setEnabled(true);
                URLS.setEnabled(true);
            }
        }
    }
}
