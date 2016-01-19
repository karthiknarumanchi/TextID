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
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karthik on 22-Dec-15.
 */

/**
 * NOT IN USE DUE TO ROTATION ISSUES
 */
public class ProcessAsyncActivity extends AsyncTask<Object, Void, Bitmap> {

    private static final String TAG = "ProcessAsyncActivity";

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

            Log.v(TAG, "Saving original bitmap");
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(DATA_PATH+"/oocr.png");
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
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

            Log.v(TAG, "Starting Processing");

            //OpenCV Warping
            Bitmap mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);

            Mat imgSource = new Mat (mutableBitmap.getHeight(), mutableBitmap.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(mutableBitmap,imgSource);
            Mat startM = findWarpedMat(imgSource);

            Mat sourceImage = new Mat (mutableBitmap.getHeight(), mutableBitmap.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(mutableBitmap,sourceImage);
            Mat warpedMat=warpImage(sourceImage,startM);

            Bitmap resultBitmap = Bitmap.createBitmap(warpedMat.cols(),  warpedMat.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(warpedMat, resultBitmap);

            Log.v(TAG, "Got warped bitmap");
            Log.v(TAG, "Saving warped bitmap");

            out = null;
            try {
                out = new FileOutputStream(DATA_PATH+"/wocr.png");
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
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

            return resultBitmap;

        } catch (Exception ex) {
            Log.d(TAG, "Error: " + ex + "\n" + ex.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(Bitmap warpedImage) {

        if(warpedImage == null || context == null)
            return;

        new RecognitionActivity(context).Recognize(warpedImage);

        super.onPostExecute(warpedImage);
    }


    public Mat findWarpedMat(Mat imgSource)
    {

        //convert the image to black and white does (8 bit)
        Imgproc.Canny(imgSource, imgSource, 50, 50);

        //apply gaussian blur to smoothen lines of dots
        Imgproc.GaussianBlur(imgSource, imgSource, new  org.opencv.core.Size(5, 5), 5);

        //find the contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imgSource, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = -1;
        int maxAreaIdx = -1;
        Log.d("size",Integer.toString(contours.size()));
        MatOfPoint temp_contour = contours.get(0); //the largest is at the index 0 for starting point
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint largest_contour = contours.get(0);
        //largest_contour.ge
        List<MatOfPoint> largest_contours = new ArrayList<MatOfPoint>();
        //Imgproc.drawContours(imgSource,contours, -1, new Scalar(0, 255, 0), 1);

        for (int idx = 0; idx < contours.size(); idx++) {
            temp_contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(temp_contour);
            //compare this contour to the previous largest contour found
            if (contourarea > maxArea) {
                //check if this contour is a square
                MatOfPoint2f new_mat = new MatOfPoint2f( temp_contour.toArray() );
                int contourSize = (int)temp_contour.total();
                MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
                Imgproc.approxPolyDP(new_mat, approxCurve_temp, contourSize*0.05, true);
                if (approxCurve_temp.total() == 4) {
                    maxArea = contourarea;
                    maxAreaIdx = idx;
                    approxCurve=approxCurve_temp;
                    largest_contour = temp_contour;
                }
            }
        }

        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BayerBG2RGB);
        //Mat sourceImage =Imgcodecs.imread(Environment.getExternalStorageDirectory().getAbsolutePath()+"/TextID/"+"/oocr.png");
        double[] temp_double;
        temp_double = approxCurve.get(0,0);
        Point p1 = new Point(temp_double[0], temp_double[1]);
        //Core.circle(imgSource,p1,55,new Scalar(0,0,255));
        //Imgproc.warpAffine(sourceImage, dummy, rotImage,sourceImage.size());
        temp_double = approxCurve.get(1,0);
        Point p2 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p2,150,new Scalar(255,255,255));
        temp_double = approxCurve.get(2,0);
        Point p3 = new Point(temp_double[0], temp_double[1]);
        //Core.circle(imgSource,p3,200,new Scalar(255,0,0));
        temp_double = approxCurve.get(3,0);
        Point p4 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p4,100,new Scalar(0,0,255));
        List<Point> source = new ArrayList<Point>();
        source.add(p1);
        source.add(p2);
        source.add(p3);
        source.add(p4);
        Mat startM = Converters.vector_Point2f_to_Mat(source);
        return startM;
    }

    public Mat warpImage(Mat inputMat, Mat startM) {
        int resultWidth = 1000;
        int resultHeight = 1000;

        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);

        Point ocvPOut1 = new Point(0, 0);
        Point ocvPOut2 = new Point(0, resultHeight);
        Point ocvPOut3 = new Point(resultWidth, resultHeight);
        Point ocvPOut4 = new Point(resultWidth, 0);
        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Imgproc.warpPerspective(inputMat,
                outputMat,
                perspectiveTransform,
                new Size(resultWidth, resultHeight),
                Imgproc.INTER_CUBIC);

        return outputMat;
    }

}
