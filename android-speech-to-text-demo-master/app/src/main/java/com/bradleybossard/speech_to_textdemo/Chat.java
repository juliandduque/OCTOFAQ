package com.bradleybossard.speech_to_textdemo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Chat extends ActionBarActivity {
    protected static final int RESULT_SPEECH = 1;
    private ImageButton btnSpeak;
    private EditText txtText;
    private ListView listView;
    private Button submitQuestion;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;

    private String SERVICE_REQUEST_URL = "https://westus.api.cognitive.microsoft.com/qnamaker/v2.0/knowledgebases/";
    private String SERVICE_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();
        SERVICE_REQUEST_URL = intent.getStringExtra("EXTRAMESSAGE");
        SERVICE_KEY = intent.getStringExtra("SERVICE_KEY");

        txtText = (EditText) findViewById(R.id.txtText);

        listView = (ListView) findViewById(R.id.FINDME);

        arrayList = new ArrayList<String>();

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        submitQuestion = (Button) findViewById(R.id.SUBMITQUESTION);

        // Adapter: You need three parameters 'the context, id of the layout (it will be where the data is shown),
        // and the array that contains the data
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, arrayList);

        // Here, you set the data in your ListView
        listView.setAdapter(adapter);

        arrayList.add("Please ask me anything!");

        btnSpeak.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {


                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Opps! Your device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });

        submitQuestion.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if(txtText.getText() != null && txtText.getText().toString() != "")
                {
                    new GetFeedTask().execute(txtText.getText().toString());
                }
            }
        });
    }


    class GetFeedTask extends AsyncTask<String, Void, String> {

        private Exception exception;
        String answer = "";
        String question = "";

        @Override
        protected String doInBackground(String... urla) {
            String responser = "";
            try {

                JSONObject BODY = new JSONObject();
                BODY.put("question", urla[0]);
                BODY.put("top", 1);

                question = urla[0];

                URL url = new URL(SERVICE_REQUEST_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(30000);
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("Ocp-Apim-Subscription-Key", SERVICE_KEY);
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
                }
                // System.out.println(newjson);
                String json = newjson.toString();
                JSONObject jason = new JSONObject(json);

                answer = jason.getJSONArray("answers").getJSONObject(0).getString("answer").replaceAll("&#39;", " ");

            } catch (Exception e) {
                responser = "400";
                e.printStackTrace();
            }

            return responser;
        }

        @Override
        protected void onPostExecute(String feed)
        {
            if (feed.contains("200")) { //success
                if(answer.contains("in the KB")) answer = "I can't answer that...";

                arrayList.add(0, answer);
                arrayList.add(0, question);

                adapter.notifyDataSetChanged();

                txtText.setText("");
            }
            else
            {
                arrayList.add(0, question);
                adapter.notifyDataSetChanged();

                arrayList.add("Something wrong happened :( Try again later");

                adapter.notifyDataSetChanged();

                txtText.setText("");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    adapter.notifyDataSetChanged();

                    txtText.setText(text.get(0));

                    new GetFeedTask().execute(txtText.getText().toString());
                }
                break;
            }
        }
    }

    private void speakWords(String speech) {
//implement TTS here
    }
}
