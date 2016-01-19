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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Karthik on 17-Dec-15.
 */
public class ProcessAsyncEngine extends AsyncTask<Object, Void, Bitmap>  {

    private static final String TAG = "ProcessAsyncEngine";

    private static final boolean TRANSPARENT_IS_BLACK = false;
    private static final double SPACE_BREAKING_POINT = 13.0/30.0;
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/TextID/";

    private Bitmap bmp;

    private Context context;

    @Override
    protected Bitmap doInBackground(Object... params) {

        try {

            if(params.length < 2) {
                Log.e(TAG, "Error passing parameter to execute - missing params");
                return null;
            }

            if(!(params[0] instanceof Context) || !(params[1] instanceof Bitmap)) {
                Log.e(TAG, "Error passing parameter to execute(context, bitmap)");
                return null;
            }

            context = (Context)params[0];

            bmp = (Bitmap)params[1];

            if(context == null || bmp == null) {
                Log.e(TAG, "Error passed null parameter to execute(context, bitmap)");
                return null;
            }

            Log.v(TAG, "Starting Binarization");

            // Binarization
            Bitmap mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);

            if(CaptureActivity.getEnhancedState())
            {
                Log.v(TAG, "Enhanced processing");
                // I will look at each pixel and use the function shouldBeBlack to decide whether to make it black or otherwise white
                //Bitmap mutableBitmap = convertToMutable(bmp);
                for(int i=0;i<mutableBitmap.getWidth();i++) {
                    for(int c=0;c<mutableBitmap.getHeight();c++) {
                        int pixel = mutableBitmap.getPixel(i, c);
                        if(shouldBeBlack(pixel))
                            mutableBitmap.setPixel(i, c, Color.BLACK);
                        else
                            mutableBitmap.setPixel(i, c, Color.WHITE);
                    }
                }
            }
            else
            {
                Log.v(TAG, "Grayscale processing");
                Paint paint = new Paint();
                Canvas canvas=new Canvas(mutableBitmap);
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                paint.setColorFilter(new ColorMatrixColorFilter(cm));
                canvas.drawBitmap(mutableBitmap, 0, 0, paint);
            }

            Log.v(TAG, "Got binarized bitmap");
            Log.v(TAG, "Saving binarized bitmap");

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(DATA_PATH+"/bocr.png");
                mutableBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return mutableBitmap;

        } catch (Exception ex) {
            Log.d(TAG, "Error: " + ex + "\n" + ex.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(Bitmap binarizedImage) {

        if(binarizedImage == null || context == null)
            return;

        new RecognitionActivity(context).Recognize(binarizedImage);

        super.onPostExecute(binarizedImage);
    }

    private static boolean shouldBeBlack(int pixel) {
        int alpha = Color.alpha(pixel);
        int redValue = Color.red(pixel);
        int blueValue = Color.blue(pixel);
        int greenValue = Color.green(pixel);
        if(alpha == 0x00) //if this pixel is transparent let me use TRANSPARENT_IS_BLACK
            return TRANSPARENT_IS_BLACK;
        // distance from the white extreme
        double distanceFromWhite = Math.sqrt(Math.pow(0xff - redValue, 2) + Math.pow(0xff - blueValue, 2) + Math.pow(0xff - greenValue, 2));
        // distance from the black extreme //this should not be computed and might be as well a function of distanceFromWhite and the whole distance
        double distanceFromBlack = Math.sqrt(Math.pow(0x00 - redValue, 2) + Math.pow(0x00 - blueValue, 2) + Math.pow(0x00 - greenValue, 2));
        // distance between the extremes //this is a constant that should not be computed :p
        double distance = distanceFromBlack + distanceFromWhite;
        // distance between the extremes
        return ((distanceFromWhite/distance)>SPACE_BREAKING_POINT);
    }

}
