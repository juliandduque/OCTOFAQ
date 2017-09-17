package com.bradleybossard.speech_to_textdemo;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.support.annotation.MainThread;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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


public class MainActivity extends ActionBarActivity
{
    private EditText URLS;
    private Button submitURL;
    private TextView statusLabel;
    private String SERVICEURL = "https://westus.api.cognitive.microsoft.com/qnamaker/v2.0/knowledgebases/create?";
    private String REQUESTURL = "https://westus.api.cognitive.microsoft.com/qnamaker/v2.0/knowledgebases/";
    private String SERVICEKEY = "354c22285b484aa3ac2e556a70b904b0";
    private String SERVICENAME = "OCTOFAQ";
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
            baseUrl = url;
            FaqUrlsRequest faqUrlsRequest = new FaqUrlsRequest(url, "100");
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
            String urlCsv = sb.toString();
            if(urlCsv.lastIndexOf(",") == urlCsv.length() - 1) urlCsv = urlCsv.substring(0, urlCsv.length() - 1);
            new PostFeedTask().execute(urlCsv);
        }
    }

    class PostFeedTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... urla) {

            String responser = "";
            try {

                String[] ur = urla[0].toString().split(",");

                ArrayList<String> urls = new ArrayList<String>();

                for (int i = 0; i < ur.length; i++)
                {
                    String temp = ur[i];
                    if(!temp.startsWith("http") && !temp.startsWith("https"))
                        temp = "http://" + temp;
//                    if(!ur[i].startsWith("http://www.") && !ur[i].startsWith("https://www."))
//                    {
//                        temp = "http://www." + temp;
//                    }
                    if(urls.indexOf(".pdf") == -1)
                        urls.add(temp);
                }

                JSONObject BODY = new JSONObject();
                BODY.put("name", SERVICENAME);
                for(int cnt = 0; cnt < urls.size(); cnt++)
                    Log.e("MainActivity >>>>", urls.get(cnt));
                BODY.put("urls", new JSONArray(urls));

                URL url = new URL(SERVICEURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("Ocp-Apim-Subscription-Key", SERVICEKEY);
                connection.setRequestProperty("Content-Type", "application/json");

                OutputStream os = connection.getOutputStream();

                System.out.println(BODY.toString());
                os.flush();
                os.write(BODY.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                responser = connection.getResponseCode() + "";

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line, newjson = "";
                    while ((line = reader.readLine()) != null) {
                        newjson += line;
                        System.out.println(line);
                    }
                    // System.out.println(newjson);
                    String json = newjson.toString();
                    JSONObject jason = new JSONObject(json);

                REQUESTURL = REQUESTURL + jason.getString("kbId") + "/generateAnswer";

            } catch (Exception e) {
                responser = "400";
                e.printStackTrace();
            }

            return responser;
        }

        @Override
        protected void onPostExecute(String feed)
        {
            if (feed.contains("201")) { //success
                statusLabel.setText("Success");
                try
                {
                    Intent intent = new Intent(MainActivity.this, Chat.class);
                    intent.putExtra("EXTRAMESSAGE", REQUESTURL);
                    REQUESTURL = "https://westus.api.cognitive.microsoft.com/qnamaker/v2.0/knowledgebases/";
                    submitURL.setEnabled(true);
                    URLS.setEnabled(true);
                    startActivity(intent);
                }
                catch(Exception e)
                {
                    statusLabel.setText("Something wrong happened :( Try again later or check your URL!");
                    submitURL.setEnabled(true);
                    URLS.setEnabled(true);
                }
            }
            else
            {
                statusLabel.setText("Something wrong happened :( Try again later or check your URL!");
                submitURL.setEnabled(true);
                URLS.setEnabled(true);
            }
        }
    }

}
