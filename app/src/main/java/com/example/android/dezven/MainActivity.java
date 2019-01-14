package com.example.android.dezven;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.internal.service.Common;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.onesignal.OneSignal;

import android.provider.Settings.Secure;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private String iMei;
     private FusedLocationProviderClient mFusedLocationClient;
    private double currentLatitude;
    private double currentLongitude;
    private static final int MY_PERMISSIONS_REQUEST_MULTIPLE = 4;
    private static final int READ_CONTACTS = 1;
    private String latitude;
    private String longitude;
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private String sendImei;
    private WebView webView;
    private long enqueue;
    private DownloadManager dm;
    BroadcastReceiver receiver;
    private String android_id;
    private String androidId;
    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private static final int MY_CAMERA_REQUEST = 7;
    private static final int  REQUEST_IMAGE_CAPTURE = 9;
    private static final String TAG = "tag";
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(R.layout.webview);

        // OneSignal Initialization
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        webView = (WebView) findViewById(R.id.webview);
        if (savedInstanceState == null) {
            android_id = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
            webView.loadUrl("http://msg24.in/devshree_app/login-new.php?android_id"+android_id);
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebViewClient(new MyWebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.setWebViewClient(new MyWebViewClient());
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DownloadedImage");
                dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                enqueue = dm.enqueue(request);
                Toast.makeText(MainActivity.this, "Downloading File...", Toast.LENGTH_SHORT).show();
            }
        });

        webView.setWebChromeClient(new ChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d(TAG, "onPermissionRequest");
                MainActivity.this.runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void run() {
                        Log.d(TAG, request.getOrigin().toString());
                        if(request.getOrigin().toString().equals("file:///")) {
                            Log.d(TAG, "GRANTED");
                            request.grant(request.getResources());
                        } else {
                            Log.d(TAG, "DENIED");
                            request.deny();
                        }
                    }
                });
            }
            });

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) + ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) + ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Please allow permissions to better serve you.", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_MULTIPLE);
            }
        } else {
        }
        }

    public class WebAppInterface {
        Context mContext;
    /** Instantiate the interface and set the context */
    WebAppInterface(Context c) {
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void showToast(String toast) {
        userID = toast;
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }
    }

        @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
        }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grandResults) {

        switch (requestCode) {

            case MY_PERMISSIONS_REQUEST_MULTIPLE: {
                if (grandResults.length > 0 && grandResults[0] == PackageManager.PERMISSION_GRANTED) {

                    try {
                        iMei = null;
                        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
                        if (null != tm) {
                            iMei = tm.getDeviceId();
                            sendImeiNetworkRequest();
                            }
                        if (null == iMei || 0 == iMei.length()) {
                            iMei = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
                        }

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {

                                        if (location != null) {
                                            String locationString = String.valueOf(location);
                                            currentLatitude = location.getLatitude();
                                            currentLongitude = location.getLongitude();
                                            sendNetworkRequest(currentLatitude, currentLongitude);
                                                                                 }
                                    }
                                });
                        webView.setDownloadListener(new DownloadListener() {
                            @Override
                            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                                DownloadManager.Request request = new DownloadManager.Request(
                                        Uri.parse(url));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DownloadedImage");
                                dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                enqueue = dm.enqueue(request);
                                Toast.makeText(MainActivity.this, "Downloading File...", Toast.LENGTH_SHORT).show();
                            }
                        });
                        webView.setWebChromeClient(new ChromeClient());

                        } catch (SecurityException e)

                    {
                        e.printStackTrace();

                    }

                    } else {

                    Toast.makeText(this, "User denied permissions", Toast.LENGTH_SHORT).show();
                }
            }
            }
            }

    private void sendNetworkRequest(double Latitude, double Longitude) {

     //   sendImei = String.valueOf(iMei);
        latitude = String.valueOf(Latitude);
        longitude = String.valueOf(Longitude);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://msg24.in/devshree_app/admin/api.php";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(getApplicationContext(), "Post successful.", Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Error Occurred", Toast.LENGTH_SHORT).show();
                }
                }) { @Override
        protected Map<String, String> getParams() {
            Map<String, String> params = new HashMap<String, String>();
            params.put("userid", userID);
            params.put("device", android_id);
            params.put("lat", latitude);
            params.put("lon", longitude);
            return params;
        }
        };
        queue.add(stringRequest);
    }

    private void sendImeiNetworkRequest() {

        sendImei = String.valueOf(iMei);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://msg24.in/devshree_app/admin/api.php";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(getApplicationContext(), "Post successful.", Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Error Occurred", Toast.LENGTH_SHORT).show();
                }

        }) { @Override
        protected Map<String, String> getParams() {
            Map<String, String> params = new HashMap<String, String>();
            params.put("userid", userID);
            params.put("device", android_id);
            params.put("imei", sendImei);
            return params;
        }
        };
        queue.add(stringRequest);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && this.webView.canGoBack()) {
            this.webView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("mailto:")) {
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                        startActivity(emailIntent);
                    return true;
                } else if (url.startsWith("tel:")) {
                    Intent telephoneIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(telephoneIntent);
                    return true;
                }
                else
                    view.loadUrl(url);
                    return true;
                }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

    public class ChromeClient extends WebChromeClient {

        // For Android 5.0
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

            return true;

        }

        // openFileChooser for Android 3.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {

            mUploadMessage = uploadMsg;
            // Create AndroidExampleFolder at sdcard
            File imageStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES)
                    , "AndroidExampleFolder");

            if (!imageStorageDir.exists()) {
                // Create AndroidExampleFolder at sdcard
                imageStorageDir.mkdirs();
            }

            // Create camera captured image file path and name
            File file = new File(
                    imageStorageDir + File.separator + "IMG_"
                            + String.valueOf(System.currentTimeMillis())
                            + ".jpg");

            mCapturedImageURI = Uri.fromFile(file);

            // Camera capture image intent
            final Intent captureIntent = new Intent(
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");

            // Create file chooser intent
            Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

            // Set camera intent to file chooser
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                    , new Parcelable[] { captureIntent });

            // On select image call onActivityResult method of activity
            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
            }

        // openFileChooser for Android < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, "");
        }

        //openFileChooser for other Android versions
        public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                    String acceptType,
                                    String capture) {
            openFileChooser(uploadMsg, acceptType);
        }
        }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] results = null;

            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            if (requestCode == FILECHOOSER_RESULTCODE) {

                if (null == this.mUploadMessage) {
                    return;
                    }

                Uri result = null;

                try {
                    if (resultCode != RESULT_OK) {

                        result = null;

                    } else {

                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :" + e,
                            Toast.LENGTH_LONG).show();
                }

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
                }
        }
        return;
    }
}

