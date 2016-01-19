package karthiknr.TextID;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Created by Karthik on 16-Dec-15.
 */
public class RecognitionActivity {

    private static final String TAG = "RecognitionActivity";

    public static final String lang = "eng";

    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/TextID/";

    public Context context;

    protected OutputActivity output;

    TessBaseAPI baseApi;
    public RecognitionActivity(Context context)
    {
        this.context=context;
        baseApi = new TessBaseAPI();
        output = new OutputActivity(context);
    }

    protected void Recognize(Bitmap bmp)
    {
        Log.v(TAG, "Starting recognition");
        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, lang);
        baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "!?@#$%&*()<>_-+=/.,:;'\\\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "");
        baseApi.setPageSegMode(1);// PSM_AUTO_OSD
        //baseApi.setPageSegMode(3);// PSM_AUTO(No OSD)
        baseApi.setImage(bmp);
        String recognizedText = baseApi.getUTF8Text();
        baseApi.end();

        Log.v(TAG,"Recog completed");
        Log.v(TAG, "OCRED TEXT: " + recognizedText);

        output.DisplayImage(bmp);
        output.DisplayOutput(recognizedText);

    }
}
