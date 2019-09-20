package org.openalpr.app;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.openalpr.OpenALPR;
import org.openalpr.model.Results;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ForthActivity extends Activity implements CvCameraViewListener2 {


    private String ANDROID_DATA_DIR;

    private boolean resultInProgress=false;

    // Initialize OpenCV manager.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            mOpenCvCameraView.enableView();

        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            if (_Ok) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
            }

        } else {
            if (_Ok) {


                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }

    }

    @Override
    public void onPause() {

        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

//    TessBaseAPI tessBaseApi = new TessBaseAPI();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forth);
        ANDROID_DATA_DIR = this.getApplicationInfo().dataDir;

//        tessBaseApi.init(DATA_PATH, "eng");

        String permissions[] = new String[]{
                Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE
        };


        _Ok = PermissionUtils.validate(this, 0, permissions);
        if (_Ok) {

            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraView);
            mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
        } else {
            Toast.makeText(ForthActivity.this, "Permission denied", Toast.LENGTH_LONG).show();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(ForthActivity.this, "Permission denied", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ForthActivity.this, "Permission Granted", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, ForthActivity.class));
            }
        }
    }

    // Load a network.
    public void onCameraViewStarted(int width, int height) {
        String proto = getPath("lpr.prototxt", this);
        String weights = getPath("lpr.caffemodel", this);
        net = Dnn.readNetFromCaffe(proto, weights);
        Log.i(TAG, "Network loaded successfully");
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        ;
        final float WH_RATIO = (float) IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 0.007843;
        final double MEAN_VAL = 127.5;
        final double THRESHOLD = 0.2;

//        pixel_means=[0.406, 0.456, 0.485]
//        pixel_stds=[0.225, 0.224, 0.229]
//        pixel_scale=255.0

        // Get a new frame
        Mat frame = inputFrame.rgba();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

//        Mat blob = Dnn.blobFromImage(frame, 0.00392, new Size(300, 300), new Scalar(0, 0, 0),/*swapRB*/false, /*crop*/false);

        // Forward image through network.
        Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                new Size(IN_WIDTH, IN_HEIGHT),
                new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false);

//        Mat blob=Dnn.blobFromImage(frame);

        net.setInput(blob);
        Mat detections = net.forward();
        int cols = frame.cols();
        int rows = frame.rows();
        Size cropSize;
        if ((float) cols / rows > WH_RATIO) {
            cropSize = new Size(rows * WH_RATIO, rows);
        } else {
            cropSize = new Size(cols, cols / WH_RATIO);
        }
        int y1 = (int) (rows - cropSize.height) / 2;
        int y2 = (int) (y1 + cropSize.height);
        int x1 = (int) (cols - cropSize.width) / 2;
        int x2 = (int) (x1 + cropSize.width);

        Mat subFrame = frame.submat(y1, y2, x1, x2);
        cols = subFrame.cols();
        rows = subFrame.rows();
        detections = detections.reshape(1, (int) detections.total() / 7);
        for (int i = 0; i < detections.rows(); ++i) {
            double confidence = detections.get(i, 2)[0];
            if (confidence > THRESHOLD) {
                int classId = (int) detections.get(i, 1)[0];
                int xLeftBottom = (int) (detections.get(i, 3)[0] * cols);
                int yLeftBottom = (int) (detections.get(i, 4)[0] * rows);
                int xRightTop = (int) (detections.get(i, 5)[0] * cols);
                int yRightTop = (int) (detections.get(i, 6)[0] * rows);

                Point point1 = new Point(xLeftBottom - 60, yLeftBottom);
                Point point2 = new Point(xRightTop + 60, yRightTop);

                // Draw rectangle around detected object.
                Imgproc.rectangle(subFrame, point1,
                        point2,
                        new Scalar(0, 255, 0), 5);
                try {
                    Rect roi = new Rect(xLeftBottom + 60, yLeftBottom, xRightTop + 120, 150);
//                    Rect rectCrop = new Rect(xLeftBottom, yLeftBottom, xRightTop, yRightTop);
                    Mat image_output = frame.submat(roi);
                    Bitmap bitmap = convertMatToBitMap(image_output);
                    System.out.print(bitmap);

                    if(!resultInProgress){
                        processResult(bitmap,subFrame,xLeftBottom,yLeftBottom);
                    }
                } catch (Exception e) {
                    resultInProgress=false;
                    e.printStackTrace();
                }
            }
        }
        return frame;
    }


    private class RecognizeText extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }


    }

    public String dateToString(Date date, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format, Locale.getDefault());

        return df.format(date);
    }

    private void processResult(Bitmap bitmap,Mat subFrame,int xLeftBottom,int yLeftBottom) {

        resultInProgress=true;

        File folder = new File(Environment.getExternalStorageDirectory() + "/OpenALPR/");
        if (!folder.exists()) {
            folder.mkdir();
        }

        // Generate the path for the next photo
        String name = dateToString(new Date(), "yyyy-MM-dd-hh-mm-ss");
        final File destination = new File(folder, name + ".jpg");

//        final String filename = Environment.getExternalStorageDirectory() + File.separator + System.currentTimeMillis() + ".png";
        FileOutputStream fOut = null;
//        final File file = new File(filename);
        try {
            fOut = new FileOutputStream(destination);
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

//        final ProgressDialog progress = ProgressDialog.show(this, "Loading", "Parsing result...", true);
        final String openAlprConfFile = ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf";
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 10;

        final String result = OpenALPR.Factory.create(ForthActivity.this, ANDROID_DATA_DIR).recognizeWithCountryRegionNConfig("us", "us", destination.getAbsolutePath(), openAlprConfFile, 10);
        Log.d("OPEN ALPR", result);
        try {
            destination.delete();
            final Results results = new Gson().fromJson(result, Results.class);
            String resu=results.getResults().get(0).getPlate();
            Imgproc.putText(subFrame, resu, new Point(xLeftBottom, yLeftBottom),
                        Core.FONT_HERSHEY_DUPLEX , 1.00, new Scalar(255, 0, 0),5);
        }catch (Exception e) {
        }

//        this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//
//                    Toast.makeText(ForthActivity.this, , Toast.LENGTH_SHORT).show();
//                    final AlertDialog.Builder builder=new AlertDialog.Builder(ForthActivity.this);
//                    builder.setTitle("Result");
//                    builder.setMessage("result is : "+results.getResults().get(0).getPlate() );
//                    builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            resultInProgress=false;
//                            Toast.makeText(ForthActivity.this, "restarted", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                    builder.show();
////                    System.out.print();
//                } catch (Exception exception) {
//                    resultInProgress=false;
//                    final ResultsError resultsError = new Gson().fromJson(result, ResultsError.class);
//                }
//            }
//        });
        // Picasso requires permission.WRITE_EXTERNAL_STORAGE
    }


    private static Bitmap convertMatToBitMap(Mat input) {
        Bitmap bmp = null;
        Mat rgb = new Mat();
        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB);

        try {
            bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgb, bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }
        return bmp;
    }


//    private String extractText(Bitmap bitmap) throws Exception {
//        tessBaseApi.setImage(bitmap);
//        String extractedText = tessBaseApi.getUTF8Text();
//        tessBaseApi.end();
//        return extractedText;
//    }

    public void onCameraViewStopped() {
    }

    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }

    private static final String TAG = "OpenCV/Sample/MobileNet";
    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};


    private Net net;
    private CameraBridgeViewBase mOpenCvCameraView;
    boolean _Ok = false;
}
