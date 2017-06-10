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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Karthik on 15-Dec-15.
 */
public class CaptureActivity extends Activity implements View.OnClickListener {
	public static final String PACKAGE_NAME = "karthiknr.TextID";
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/TextID/";
	private static final String TAG = "CaptureActivity";

    public static final String lang = "eng";

    public static boolean isEnhanced = false;

	protected Button shutter_button;
    protected Button speech_button;
    protected Button gallery_button;
	protected static TextView _field;
    protected static ImageView _image;
	protected String _path;
	protected boolean _taken;
    protected OutputActivity output;
    protected static ProgressDialog progress;

	protected static final String PHOTO_TAKEN = "photo_taken";

	@Override
	public void onCreate(Bundle savedInstanceState) {

		String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

		for (String path : paths) {
			File dir = new File(path);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
					return;
				} else {
					Log.v(TAG, "Created directory " + path + " on sdcard");
				}
			}
		}
		if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
			try {
				AssetManager assetManager = getAssets();
				InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
				//GZIPInputStream gin = new GZIPInputStream(in);
				OutputStream out = new FileOutputStream(DATA_PATH
						+ "tessdata/" + lang + ".traineddata");

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				//while ((lenf = gin.read(buff)) > 0) {
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				//gin.close();
				out.close();

				Log.v(TAG, "Copied " + lang + " traineddata");
			} catch (IOException e) {
				Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
			}
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		_field = (TextView) findViewById(R.id.field);
        _image = (ImageView) findViewById(R.id.imageView);
		shutter_button = (Button) findViewById(R.id.shutter_button);
        speech_button = (Button) findViewById(R.id.speech_button);
        gallery_button = (Button) findViewById(R.id.gallery_button);
		shutter_button.setOnClickListener(this);
        speech_button.setOnClickListener(this);
        gallery_button.setOnClickListener(this);
		_path = DATA_PATH + "/ocr.jpg";
        progress = new ProgressDialog(this);
        progress.setTitle("Please Wait");
        progress.setMessage("Processing Image and Recognizing");
        progress.setCancelable(false);
        output=new OutputActivity(this);

        // For Open CV based Processing, uncomment both the below methodcall and the method
/*        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2,
                CaptureActivity.this, mOpenCVCallBack)) {
            Log.e("TEST", "Cannot connect to OpenCV Manager");
        }
*/
	}
/*
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
*/
    protected void startGalleryActivity() {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

	protected void startCameraActivity() {
		File file = new File(_path);
		Uri outputFileUri = Uri.fromFile(file);
		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		startActivityForResult(intent, 0);
	}

    @Override
    public void onClick(View v) {
        if(v == shutter_button) {
            _field.setText("");
            _image.setVisibility(View.INVISIBLE);
            Log.v(TAG,"Closing any open TTS Engines");
            output.CloseTTS();
            Log.v(TAG, "Starting Camera app");
            startCameraActivity();
        }
        if(v == speech_button) {
            if(_field.getText().length()>0)
            {
                Log.v(TAG, "Starting/Stopping Speech");
                output.Speech(_field.getText().toString());
            }
            else
            {
                Log.v(TAG, "Field empty");
                Toast.makeText(getApplicationContext(), "Capture an image first!", Toast.LENGTH_SHORT).show();
            }
        }
        if(v == gallery_button) {
            _field.setText("");
            _image.setVisibility(View.INVISIBLE);
            Log.v(TAG,"Closing any open TTS Engines");
            output.CloseTTS();
            Log.v(TAG, "Starting Gallery Activity");
            startGalleryActivity();
        }
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "resultCode: " + resultCode);
        if(requestCode == 1)//Gallery Request Code
        {
            if (resultCode == RESULT_OK) {
                onPhotoChosen(data);
            } else {
                Log.v(TAG, "User cancelled");
            }
        }
        else if(requestCode == 0)//Camera Request Code
        {
            if (resultCode == RESULT_OK) {
                onPhotoTaken();
            } else {
                Log.v(TAG, "User cancelled");
            }
        }
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(CaptureActivity.PHOTO_TAKEN, _taken);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.i(TAG, "onRestoreInstanceState()");
		if (savedInstanceState.getBoolean(CaptureActivity.PHOTO_TAKEN)) {
            _taken = true;
			onPhotoTaken();
		}
	}

    protected void onPhotoChosen(Intent data) {
        Uri uri = data.getData();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            progress.show();
            new ProcessImageActivity(this).ProcessImage(bitmap,false);//Photo not captured
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onPhotoTaken() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2; //Increase for decrease in quality. 1 is for zero sampling,4 unte image res 1/16 untundi
        Bitmap bitmap = BitmapFactory.decodeFile(_path, options);
        progress.show();
        new ProcessImageActivity(this).ProcessImage(bitmap,true);//Photo captured
    }

    public static void SetText(String txt)
    {
        if ( txt.length() != 0 ) {
            progress.dismiss();
            _field.setText(_field.getText().toString().length() == 0 ? txt : _field.getText() + " " + txt);
            Log.v(TAG, "Text Set to Field");
        }
        else {
            progress.dismiss();
            _field.setText("Unable to recognize");
        }

    }

    public static void SetImage(Bitmap bmp)
    {
        if(_image.getVisibility()== View.INVISIBLE) {
            _image.setImageBitmap(bmp);
            _image.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        output.CloseTTS();
        System.gc();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate your Menu
        getMenuInflater().inflate(R.menu.main, menu);

        // Get the action view used in your toggle item
        final MenuItem toggleservice = menu.findItem(R.id.toggleprocessing);
        final Switch actionView = (Switch) toggleservice.getActionView();
        actionView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Start or stop your Service
                if(isChecked)
                    isEnhanced=true;
                else
                    isEnhanced=false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            Toast.makeText(getApplicationContext(), "Karthik Narumanchi,2016", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static boolean getEnhancedState()
    {
        return isEnhanced;
    }

}
