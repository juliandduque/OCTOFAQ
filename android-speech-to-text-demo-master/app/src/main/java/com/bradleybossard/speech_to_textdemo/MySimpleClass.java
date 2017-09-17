package com.bradleybossard.speech_to_textdemo;

import android.content.Context;
import android.speech.tts.TextToSpeech;

/**
 * Created by julia on 9/17/2017.
 */

public class MySimpleClass implements TextToSpeech.OnInitListener
{

    private TextToSpeech tts;
    private boolean ttsOk;

    // The constructor will create a TextToSpeech instance.
    MySimpleClass(Context context) {
        tts = new TextToSpeech(context, this);
    }

    @Override
    // OnInitListener method to receive the TTS engine status
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsOk = true;
        }
        else {
            ttsOk = false;
        }
    }

    // A method to speak something
    @SuppressWarnings("deprecation") // Support older API levels too.
    public void speak(String text, Boolean override) {
        if (ttsOk) {
            if (override) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
            else {
                tts.speak(text, TextToSpeech.QUEUE_ADD, null);
            }
        }
    }
}
