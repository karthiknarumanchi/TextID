/* Copyright 2015 Karthik Narumanchi
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/


package karthiknr.TextID;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Created by Karthik on 16-Dec-15.
 */
public class OutputActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "OutputActivity";

    private static TextToSpeech engine;
    private static String recognizedText;
    public Context context;

    private static boolean isAvailable = false;

    public OutputActivity(Context context)
    {
        this.context=context;
        Log.v(TAG,"Starting TTS");
        engine = new TextToSpeech(context, this);
        isAvailable = true;
    }

    @Override
    public void onInit(int status) {
        Log.v("Speech", "OnInit - Status ["+status+"]");
        if (status == TextToSpeech.SUCCESS) {
            Log.d("Speech", "Success!");
            engine.setLanguage(Locale.UK);
        }
    }

    protected static void DisplayOutput(String recognizedText)
    {
        CaptureActivity.SetText(recognizedText);
    }

    protected static void DisplayImage(Bitmap bmp)
    {
        CaptureActivity.SetImage(bmp);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void Speech(String recognizedText) {
        if(engine.isSpeaking())
            engine.stop();
        else
            engine.speak(recognizedText, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public static boolean IsSpeaking()
    {
        if(engine.isSpeaking())
            return true;
        return false;
    }

    public static void CloseTTS()
    {
        if(isAvailable)
        {
            isAvailable = false;
            Log.v(TAG,"Stopping TTS");
            engine.shutdown();
        }
    }
}
