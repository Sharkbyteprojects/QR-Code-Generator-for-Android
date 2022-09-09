package com.sharkbyte.qrcodegenerator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Math;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sharkbyte.qrcodegenerator.databinding.ActivityMainBinding;

// TODO: Create Save as file
public class MainActivity extends AppCompatActivity {
    // Used to load the 'qrcodegenerator' library on application startup.
    Bitmap b;EditText textA;
    SeekBar seekBarUnitSize, errCorrection;
    SeekBar[] bgc;
    SeekBar[] fgc;
    static {
        System.loadLibrary("qrcodegenerator");
    }

    public void errb(String message, String title){
        AlertDialog.Builder builder
                = new AlertDialog
                .Builder(MainActivity.this);

        // Set the message show for the Alert time
        builder.setMessage(message);

        // Set Alert Title
        builder.setTitle(title);
        AlertDialog alertDialog = builder.create();

        // Show the Alert Dialog box
        alertDialog.show();
    }

    private ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        textA = binding.textArea;
        seekBarUnitSize = binding.seekBarUnitSize;
        errCorrection = binding.errCorrection;
        bgc = new SeekBar[]{binding.bgr, binding.bgg, binding.bgb};
        fgc = new SeekBar[]{binding.fgr, binding.fgg, binding.fgb};

        FloatingActionButton share = binding.shareit;
        MainActivity mainActivity = this;
        Toast.makeText(mainActivity, "Welcome", Toast.LENGTH_LONG).show();
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(b == null){
                    Toast.makeText(mainActivity, "No QR Code available", Toast.LENGTH_LONG).show();
                    return;
                }
                qrupdate();
                shareImageandText(b, textA.getText().toString());
            }
        });

        binding.copyr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sharkbyteprojects"));
                startActivity(browserIntent);
            }
        });

        seekBarUnitSize.setProgress(7);
        binding.clearThis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textA.setText("");
                for (SeekBar s:bgc) {
                    s.setProgress(0);
                }
                for (SeekBar s:fgc) {
                    s.setProgress(255);
                }
                qrupdate();
            }
        });
        binding.update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qrupdate();
                closeKeyboard();
            }
        });
        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                qrupdate();
                closeKeyboard();
            }
        };
        SeekBar[] sb = {
                seekBarUnitSize,
                errCorrection,
                bgc[0], bgc[1], bgc[2],
                fgc[0], fgc[1], fgc[2]
        };
        for(int x = 0;x < sb.length;x++){
            sb[x].setOnSeekBarChangeListener(onSeekBarChangeListener);
        }
        binding.textView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeKeyboard();
            }
        });

        /* SHARE */
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    binding.textArea.setText(sharedText);
                }

            }
        }
        qrupdate();
    }

    int shiftIt(int v, int d){
        return (v & 0xff) << (8 * d);
    }
    void qrupdate(){
        try {
            b = toBitmap(
                    qrFromStr(
                            textA.getText().toString(),
                            seekBarUnitSize.getProgress() + 1,
                            shiftIt(fgc[1].getProgress() & 0xff, 1) |
                                    shiftIt(fgc[0].getProgress() & 0xff, 2) |
                                    shiftIt(fgc[2].getProgress() & 0xff, 0),
                            shiftIt(bgc[1].getProgress() & 0xff, 1) |
                                    shiftIt(bgc[0].getProgress() & 0xff, 2) |
                                    shiftIt(bgc[2].getProgress() & 0xff, 0),
                            errCorrection.getProgress(), 2
                    )
            );
            ((ImageView)findViewById(R.id.imageView)).setImageBitmap(b);
        }catch(Exception ex){
            errb(ex.getMessage(), "Error");
            ex.printStackTrace();
        }
    }

    Bitmap toBitmap(int[] b){
        if(b.length == 1) return null;
        int xy = (int)Math.round(Math.sqrt(b.length));
        return Bitmap.createBitmap(b, xy, xy, Bitmap.Config.RGB_565);
    }

    /**
     * A native method that is implemented by the 'qrcodegenerator' native library,
     * which is packaged with this application.
     */
    public native int[] qrFromStr(String in, int size, int rgb_fg, int rgb_bg, int failcorrection, int border);

    private void closeKeyboard()
    {
        // this will give us the view
        // which is currently focus
        // in this layout
        View view = this.getCurrentFocus();

        // if nothing is currently
        // focus then this will protect
        // the app from crash
        if (view != null) {

            // now assign the system
            // service to InputMethodManager
            InputMethodManager manager
                    = (InputMethodManager)
                    getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            manager
                    .hideSoftInputFromWindow(
                            view.getWindowToken(), 0);
        }
    }

    private void shareImageandText(Bitmap bitmap, String t) {
        Uri uri = getimageToShare(bitmap);
        Intent intent = new Intent(Intent.ACTION_SEND);

        // putting uri of image to be shared
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        // Add subject Here
        intent.putExtra(Intent.EXTRA_SUBJECT, String.format("QR Code with Content \"%s\"", t));
        intent.putExtra(Intent.EXTRA_TEXT, "Sharing QR Code");

        // setting type to image
        intent.setType("image/png");

        // calling startactivity() to share
        startActivity(Intent.createChooser(intent, "Share Via"));
    }

    // Retrieving the url to share
    private Uri getimageToShare(Bitmap bitmap) {
        File imagefolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            imagefolder.mkdirs();
            File file = new File(imagefolder, "shared_image.png");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            uri = FileProvider.getUriForFile(this, "com.sharkbyte.qrcodegenerator.fileprovider", file);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return uri;
    }
}