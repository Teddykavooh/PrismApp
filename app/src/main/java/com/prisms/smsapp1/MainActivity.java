 package com.prisms.smsapp1;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import vpos.apipackage.PosApiHelper;
import vpos.apipackage.PrintInitException;
import vpos.apipackage.StringUtil;

 public class MainActivity extends AppCompatActivity {
    ArrayList<String> smsMessagesList = new ArrayList<>();
    ListView messages;
    ArrayAdapter<String> arrayAdapter;
    @SuppressLint("StaticFieldLeak")
    private static MainActivity inst;
    private Context mContext;
    private Activity mActivity;
    private static final int MY_PERMISSIONS_REQUEST_CODE = 123;
    /*Print Additions*/
    private Button prnAct;
    private Button prnDea;
    //private EditText licenseInputBox;
    private TextView textViewMsg;
    private final static int ENABLE_RG = 10;
    private final static int DISABLE_RG = 11;
    int IsWorking = 0;
    int ret = -1;
    private boolean m_bThreadFinished = true;
    private int voltage_level;
    private int BatteryV;
    private BroadcastReceiver receiver;
    private String text;
    private String newText;
    private String deviceId;
    SharedPreferences prismAppSp;
    String ext_text;

    /*Print focus*/
    PosApiHelper posApiHelper = PosApiHelper.getInstance();
    public String tag = "MainActivity";
    final int PRINT_CONSUME = 0;
    final int PRINT_BMP = 2;
    final int PRINT_OPEN = 8;
    final int AUTO_PRINT = 3;
    int powerLaunch = 0;
    /*Print focus ends*/

    /*Header and Footer*/
    SharedPreferences myData;
    EditText header;
    EditText footer;
    String myHeader;
    String myFooter;
    RelativeLayout myLay;

    /*Intercepting messages.*/

    public static MainActivity instance() {
        return inst;
    }

    //MENU

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem icnOn = menu.findItem(R.id.prnOnIcon);
        MenuItem icnOff = menu.findItem(R.id.prnOffIcon);
        MenuItem printerOn = menu.findItem(R.id.printerOn);
        MenuItem printerOff = menu.findItem(R.id.printerOff);
        if (powerLaunch == 1) {
            icnOff.setVisible(false);
            icnOn.setVisible(true);
            printerOn.setVisible(false);
            printerOff.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.delete:
                //delete alert Dialog
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Delete")
                        .setMessage("All threads will be deleted")
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //code that will be run if someone chooses delete
                                smsMessagesList.clear();
                                arrayAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("CANCEL", null)
                        .show();
                return true;
            case R.id.exit:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_exit_to_app_24)
                        .setTitle("EXIT")
                        .setMessage("Quit all application processes?")
                        .setPositiveButton("EXIT", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                onDestroy();
                                finishAffinity();
                                System.exit(0);
                            }
                        })
                        .setNegativeButton("BACK", null)
                        .show();
            default:
            return false;
        }
    }

    public void iconOff(MenuItem i) {
        Toast.makeText(getApplicationContext(), "Click activate print to enable.",
                Toast.LENGTH_SHORT).show();
    }

    public void iconOn(MenuItem i) {
        Toast.makeText(getApplicationContext(), "Click deactivate print to disable.",
                Toast.LENGTH_SHORT).show();
    }

    public void licenceCheck(MenuItem i) {
        if (deviceId.equals("e239aa73efe3f7f2")) {
            Toast.makeText(getApplicationContext(), "You are licensed.",
                    Toast.LENGTH_SHORT).show();
            //System.out.println("Device ID: " + deviceId);
        } else {
            Toast.makeText(getApplicationContext(), "You are unlicensed.",
                    Toast.LENGTH_SHORT).show();
            //System.out.println("Device ID: " + deviceId);
        }
    }

    //Printer Activation and Deactivation
    public void printOn(MenuItem i) {
        if (deviceId.equals("e239aa73efe3f7f2")) {
            /*Default:fcf52d5c63cb4676*/
            /*set Power ON*/
            powerLaunch = 1;
            PosApiHelper.getInstance().SysSetPower(powerLaunch);
            prnAct.setVisibility(View.INVISIBLE);
            prnDea.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(), "Print activated.",
                    Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();
//            b1.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getApplicationContext(), "You are not verified for this service.",
                    Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("VERIFICATION")
                    .setMessage("In need of activation?\n" + "Call 0721555001, 0726465617\n"
                            + "or\n" + "email: androidposkenya.co.ke")
                    .setNegativeButton("BACK", null)
                    .show();
        }
    }

    public void printOff(MenuItem i) {
        /*set Power OFF*/
        powerLaunch = 0;
        PosApiHelper.getInstance().SysSetPower(powerLaunch);
        prnDea.setVisibility(View.INVISIBLE);
        prnAct.setVisibility(View.VISIBLE);
        Toast.makeText(getApplicationContext(), "Print deactivated.",
                Toast.LENGTH_SHORT).show();
        invalidateOptionsMenu();
//        b1.setVisibility(View.INVISIBLE);

    }

    public void onClickBmp(MenuItem i) {
        if (powerLaunch == 1) {
            if (printThread != null && printThread.isThreadFinished()) {
                Log.e(tag, "Thread is still running...");
                return;
            }

            printThread = new Print_Thread(PRINT_BMP);
            printThread.start();
        } else {
            Toast.makeText(getApplicationContext(), "Activate Print to continue",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void onAbout(MenuItem i) {
        Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(aboutIntent);
    }

    public void onStyle(MenuItem i) {
        myLay.setVisibility(View.VISIBLE);
        myLay.bringToFront();
        myLay.invalidate();
    }

     public void onArrBack2(View v) {
        myLay.setVisibility(View.INVISIBLE);
     }

    /*public void onClickPrnOpen(MenuItem i) {
        //refreshSmsInbox();
        if (powerLaunch == 1) {
            if (printThread != null && printThread.isThreadFinished()) {
                Log.e(tag, "Thread is still running...");
                return;
            }

            printThread = new Print_Thread(PRINT_OPEN);
            printThread.start();
        } else {
            Toast.makeText(getApplicationContext(), "Activate Print to continue",
                    Toast.LENGTH_SHORT).show();
        }
    }*/

    public void onPrnOpen() {
        if (powerLaunch == 1) {
            if (printThread != null && printThread.isThreadFinished()) {
                Log.e(tag, "Thread is still running...");
                return;
            }

            printThread = new Print_Thread(PRINT_OPEN);
            printThread.start();
        } else {
            Toast.makeText(getApplicationContext(), "Activate Print to continue",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
//        System.out.println("onStart method power launch value: " + powerLaunch);
    }

    /* Displaying messages.*/

    @SuppressLint("HardwareIds")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

     /*   View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions); */

        //Get Verifier
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        //System.out.println(deviceId);
        setContentView(R.layout.activity_main);

        // Get the application context
        mContext = getApplicationContext();
        mActivity = MainActivity.this;

        myLay = findViewById(R.id.lay2);
        header = findViewById(R.id.header);
        footer = findViewById(R.id.footer);
        myData = getSharedPreferences("com.prisms.smsapp1", MODE_PRIVATE);

        messages = findViewById(R.id.messages);
        //input = (EditText) findViewById(R.id.input);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                smsMessagesList);
        messages.setAdapter(arrayAdapter);

        /* All Permissions*/
        //Determine if the current Android version is >=23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        } else {
            //initViews();
            Toast.makeText(this, "Android version is less than Api 23",
                    Toast.LENGTH_SHORT).show();
        }

        /*print functionality*/
        messages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l) {
                if (powerLaunch == 1) {
                    new AlertDialog.Builder(MainActivity.this)
                            //Use ResourceCompat.getDrawable
                            .setIcon(ResourcesCompat.getDrawable(getResources(),
                                    R.drawable.ic_baseline_local_printshop_24, null))
                            .setTitle("PRINT")
                            .setMessage("Print this message?")
                            .setPositiveButton("PRINT", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    /* Converting listView element to string. */
                                    text = (String) ((TextView) view).getText();
                                    /**
                                     * Get without balance
                                     *
                                     ext_bal();
                                     */
                                    onClickConsume();
                                }
                            })
                            .setNegativeButton("QUIT", null)
                            .show();
                } else {
                    //view.getContext() sub of getApplicationContext()
                    Toast.makeText(getApplicationContext(),
                            "Please, Activate print to continue.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*delete functionality*/
        messages.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int msgToDelete = i;
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Delete")
                        .setMessage("This message will be deleted")
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                smsMessagesList.remove(msgToDelete);
                                arrayAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("CANCEL", null)
                        .show();
                return true;
            }
        });

        /*Print Activation and Deactivation*/
        /*Tempo. Verification*/
        prnAct = findViewById(R.id.printOn);
        prnDea = findViewById(R.id.printOff);
        //b1 = findViewById(R.id.prnOpen);
        prnAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPrnOpen();
            }
        });

        prnDea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPrnOpen();
            }
        });
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub

        disableFunctionLaunch(true);
        /*getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);*/

        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        receiver = new BatteryReceiver();
        registerReceiver(receiver, filter);
    }

    public void updateInbox(final String smsMessage) {
        arrayAdapter.insert(smsMessage, 0);
        arrayAdapter.notifyDataSetChanged();
    }

    /*Bringing up our runtime permission requests.*/
    protected void checkPermission(){
        if(ContextCompat.checkSelfPermission(mActivity,Manifest.permission.READ_SMS)
//                + ContextCompat.checkSelfPermission(
//                mActivity,Manifest.permission.READ_CONTACTS)
                + ContextCompat.checkSelfPermission(
                mActivity,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){

            // Do something, when permissions not granted
            if(ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity,Manifest.permission.READ_SMS)
//                    || ActivityCompat.shouldShowRequestPermissionRationale(
//                    mActivity,Manifest.permission.READ_CONTACTS)
                    || ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                // If we should give explanation of requested permissions

                // Show an alert dialog here with request explanation
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setMessage("PrismApp requires Read SMS and Write External" +
                        " Storage permissions to do the task.");
                builder.setTitle("Please grant those permissions");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(
                                mActivity,
                                new String[]{
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_SMS
                                },
                                MY_PERMISSIONS_REQUEST_CODE
                        );
                    }
                });
                builder.setNeutralButton("Cancel",null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }else{
                /* Directly request for required permissions, without explanation */
                ActivityCompat.requestPermissions(
                        mActivity,
                        new String[]{
                                Manifest.permission.READ_SMS,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        MY_PERMISSIONS_REQUEST_CODE
                );
            }
        }else {
            /* Do something, when permissions are already granted */
            Toast.makeText(mContext,"Permissions already granted",Toast.LENGTH_SHORT).show();
            refreshSmsInbox();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
//        System.out.println("Hello my new request code is: "+ requestCode );
//        System.out.println("Hello my new grantResult is: "+ grantResults.length);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_CODE) {
            // When request is cancelled, the results array are empty
            if ((grantResults.length > 0) && (grantResults[0] +
                    grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                /* Permissions are granted */
                Toast.makeText(mContext, "Permissions granted.", Toast.LENGTH_SHORT).show();
                refreshSmsInbox();
            } else {
                /* Permissions are denied */
                Toast.makeText(mContext, "Permissions denied.", Toast.LENGTH_SHORT).show();
                checkPermission();
            }
        }
    }

    @SuppressLint("Recycle")
    public void refreshSmsInbox() {
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"),
                null, null, null, null);
        assert smsInboxCursor != null;
        int indexBody = smsInboxCursor.getColumnIndex("body");
        int indexAddress = smsInboxCursor.getColumnIndex("address");
        int indexDate = smsInboxCursor.getColumnIndex("date");
        int indexId = smsInboxCursor.getColumnIndex("_id");
//        System.out.println("Hapa Debug: " + smsInboxCursor.getColumnIndex("_id") );
//        System.out.println("Hapa Debug2: " + smsInboxCursor.getString(indexId) );
        /*Save index and top position*/
        int index = messages.getFirstVisiblePosition();
        View v = messages.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - messages.getPaddingTop());
        /*end*/
        if (indexBody < 0 || !smsInboxCursor.moveToFirst()) return;
        arrayAdapter.clear();
        do {
            long timeMillis = smsInboxCursor.getLong(indexDate);
            Date date = new Date(timeMillis);
            String str = "REF: " + smsInboxCursor.getString(indexId) + "\n"
                    + "From: " + smsInboxCursor.getString(indexAddress) + "\n"
                    + smsInboxCursor.getString(indexBody) + "\n"
                    + "Date: " + date + "\n";
            arrayAdapter.add(str);
//            System.out.println(str);
//            System.out.println("My bloody Id: " + smsInboxCursor.getString(indexId));
            //System.out.println("My count: " + arrayAdapter.getItem(1));
        } while (smsInboxCursor.moveToNext());
        //messages.setSelection(arrayAdapter.getCount() - 1);
        /*Cont.*/
        messages.setSelectionFromTop(index, top);

//        System.out.println("New data: " + messages.getItemAtPosition(indexId));/*REF: 114*/
//        System.out.println("New dataTwo: " + arrayAdapter.getItem(0));
//        System.out.println("New dataThree: " + arrayAdapter.getItemId(118));
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        disableFunctionLaunch(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableFunctionLaunch(false);
        PosApiHelper.getInstance().SysSetPower(0);
    }

    // Back button customization
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);

            return true;
        }
        return false;
    }

    // disable the power key when the device is boot from alarm but not ipo boot
    private static final String DISABLE_FUNCTION_LAUNCH_ACTION =
            "android.intent.action.DISABLE_FUNCTION_LAUNCH";
    private void disableFunctionLaunch(boolean state) {
        Intent disablePowerKeyIntent = new Intent(DISABLE_FUNCTION_LAUNCH_ACTION);
        if (state) {
            disablePowerKeyIntent.putExtra("state", true);
        } else {
            disablePowerKeyIntent.putExtra("state", false);
        }
        sendBroadcast(disablePowerKeyIntent);
    }

    //Custom Print
    public void customPrint() {
        posApiHelper.PrintStr("================================\n");
        posApiHelper.PrintStr("REDBERRY LOUNGE\n");
        posApiHelper.PrintStr("NAIROBI-KENYA\n");
        posApiHelper.PrintStr("================================\n");
        posApiHelper.PrintStr("M-PESA PAYMENT DETAILS:-\n");
        posApiHelper.PrintStr("================================\n");
        posApiHelper.PrintStr("\n" + text + "\n");
        posApiHelper.PrintStr("================================\n");
        posApiHelper.PrintStr("TILL Number: 5853023\n");
        posApiHelper.PrintStr("================================\n");
        posApiHelper.PrintStr("        \n");
    }

    public void customPrint2() {
        posApiHelper.PrintStr("================================\n");
        posApiHelper.PrintStr("REDBERRY LOUNGE\n");
        posApiHelper.PrintStr("NAIROBI-KENYA\n");
        posApiHelper.PrintStr("================================\n");
        posApiHelper.PrintStr("M-PESA PAYMENT DETAILS:-\n");
        posApiHelper.PrintStr("================================\n");
        posApiHelper.PrintStr( newText + "\n");
        posApiHelper.PrintStr("================================\n");
        posApiHelper.PrintStr("TILL Number: 5853023\n");
        posApiHelper.PrintStr("================================\n");
    }

    public void autoPrint() {
//        System.out.println("New dataFour: " + arrayAdapter.getItem(0)); /*Working well*/
        newText = arrayAdapter.getItem(0);
        refreshSmsInbox();
        if (powerLaunch == 1) {
            refreshSmsInbox();
            if (printThread != null && printThread.isThreadFinished()) {
                Log.e(tag, "Thread is still running...");
                return;
            }

            printThread = new Print_Thread(AUTO_PRINT);
            printThread.start();
        } else {
            Toast.makeText(getApplicationContext(), "Activate print to engage auto-printing.",
                    Toast.LENGTH_SHORT).show();
            refreshSmsInbox();
        }

    }

    public void onClickConsume() {
        if (printThread != null && printThread.isThreadFinished()) {
            Log.e(tag, "Thread is still running...");
            return;
        }

        printThread = new Print_Thread(PRINT_CONSUME);
        printThread.start();
    }

    MainActivity.Print_Thread printThread = null;

    public class Print_Thread extends Thread {

        int type;
        int RESULT_CODE = 0;

        public boolean isThreadFinished() {
            return !m_bThreadFinished;
        }

        public Print_Thread(int type) {
            this.type = type;
        }

        public void run() {
            Log.d("Print_Thread[ run ]", "run() begin");
            Message msg = Message.obtain();
            Message msg1 = new Message();

            synchronized (this) {

                m_bThreadFinished = false;
                try {
                    ret = posApiHelper.PrintInit();
                } catch (PrintInitException e) {
                    e.printStackTrace();
                    int initRet = e.getExceptionCode();
                    Log.e(tag, "initRer : " + initRet);
                }

                Log.e(tag, "init code:" + ret);

                ret = getValue();
                Log.e(tag, "getValue():" + ret);

                posApiHelper.PrintSetGray(ret);

                //posApiHelper.PrintSetVoltage(BatteryV * 2 / 100);

                ret = posApiHelper.PrintCheckStatus();
                if (ret == -1) {
                    RESULT_CODE = -1;
                    Log.e(tag, "Lib_PrnCheckStatus fail, ret = " + ret);
                    SendMsg("Error, No Paper!!");
                    m_bThreadFinished = true;
                    return;
                } else if (ret == -2) {
                    RESULT_CODE = -1;
                    Log.e(tag, "Lib_PrnCheckStatus fail, ret = " + ret);
                    SendMsg("Error, Printer Too Hot ");
                    m_bThreadFinished = true;
                    return;
                } else if (ret == -3) {
                    RESULT_CODE = -1;
                    Log.e(tag, "voltage = " + (BatteryV * 2));
//                    SendMsg("Battery less :" + (BatteryV * 2));
                    SendMsg("Battery is " + voltage_level + "%" + " Connect to power");
                    //System.out.println("Battery less :" + (BatteryV * 2));
                    m_bThreadFinished = true;
                    return;
                }
                /* else if (voltage_level < 5) {
                    RESULT_CODE = -1;
                    Log.e(tag, "voltage_level = " + voltage_level);
                    SendMsg("Battery capacity less : " + voltage_level);
                    m_bThreadFinished = true;
                    return;
                }*/
                else {
                    RESULT_CODE = 0;
                }

                switch (type) {

                    case AUTO_PRINT:
                        posApiHelper.PrintSetFont((byte) 26, (byte) 26, (byte) 0x00);
                        posApiHelper.PrintStr("        \n");
//                        posApiHelper.PrintStr("ENTETAINER TRUCKS CO. LTD\n");
//                        posApiHelper.PrintStr("SHELL ELDORET NAIROBI ROAD");
//                        posApiHelper.PrintStr("KAPSOYA, KENYA");
//                        posApiHelper.PrintStr("TILL BUY GOODS 202742");
                        posApiHelper.PrintStr("________________________________\n");
                        posApiHelper.PrintStr("M-PESA PAYMENTS DETAILS\n");
                        posApiHelper.PrintStr("________________________________\n");
                        posApiHelper.PrintStr( newText + "\n");
                        posApiHelper.PrintStr("________________________________\n");
//                        posApiHelper.PrintStr("Urban Point Restaurant CBD\n");
//                        posApiHelper.PrintStr("TILL NUMBER: 9150451\n");
//                        posApiHelper.PrintStr("Beyond Luxury & Hospitality\n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");

                        ret = posApiHelper.PrintStart();

                        Log.d("", "Lib_PrnStart ret = " + ret);

                        if (ret != 0) {
                            RESULT_CODE = -1;
                            Log.e("PrismApp", "Lib_PrnStart fail, ret = " + ret);
                            if (ret == -1) {
                                SendMsg("No Print Paper ");
                            } else if(ret == -2) {
                                SendMsg("too hot");
                            }else if(ret == -3) {
                                SendMsg("low voltage");
                            }else{
                                SendMsg("Print fail");
                            }
                        } else {
                            RESULT_CODE = 0;
                            SendMsg("Print Finish");
                        }

                        break;

                    case PRINT_CONSUME:
                        msg.what = DISABLE_RG;
                        handler.sendMessage(msg);
                        String boldCopy = "COPY";
                        /*Styling not working*/
                        SpannableString ss = new SpannableString(boldCopy);
                        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD_ITALIC);
                        ss.setSpan(boldSpan, 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        /*Full line == 30 characters*/
                        posApiHelper.PrintSetFont((byte) 26, (byte) 26, (byte) 0x00);
                        posApiHelper.PrintStr("        \n");
//                        posApiHelper.PrintStr("ENTETAINER TRUCKS CO. LTD\n");
//                        posApiHelper.PrintStr("SHELL ELDORET NAIROBI ROAD");
//                        posApiHelper.PrintStr("KAPSOYA, KENYA");
//                        posApiHelper.PrintStr("TILL BUY GOODS 202742");
                        posApiHelper.PrintStr("________________________________\n");
                        posApiHelper.PrintStr("M-PESA PAYMENTS DETAILS\n");
                        posApiHelper.PrintStr("________________________________\n");
                        posApiHelper.PrintStr(ss + "\n" + text + "\n");
                        posApiHelper.PrintStr("________________________________\n");
//                        posApiHelper.PrintStr("TILL NUMBER: 9150451\n");
//                        posApiHelper.PrintStr("Beyond Luxury & Hospitality\n");
                        posApiHelper.PrintStr("        \n");

                        /**
                         * M-PESA CUT
                         *
                        posApiHelper.PrintStr(ss + "\n" + ext_text + "\n");
                        */

                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");

                        ret = posApiHelper.PrintStart();
                        msg1.what = ENABLE_RG;
                        handler.sendMessage(msg1);

                        Log.d("", "Lib_PrnStart ret = " + ret);

                        if (ret != 0) {
                            RESULT_CODE = -1;
                            Log.e("PrismApp", "Lib_PrnStart fail, ret = " + ret);
                            if (ret == -1) {
                                SendMsg("No Print Paper ");
                            } else if(ret == -2) {
                                SendMsg("too hot");
                            }else if(ret == -3) {
                                SendMsg("low voltage");
                            }else{
                                SendMsg("Print fail");
                            }
                        } else {
                            RESULT_CODE = 0;
                            SendMsg("Print Finish");
                        }

                        break;

                    case PRINT_OPEN:
                        try {
                            ret = posApiHelper.PrintOpen();
                        } catch (PrintInitException e) {
                            e.printStackTrace();
                        }
                        Log.d("", "Lib_PrnStart ret = " + ret);
                        if (ret != 0) {
                            RESULT_CODE = -1;
                            Log.e("PrismApp", "Lib_PrnStart fail, ret = " + ret);
                            if (ret == -1) {
                                SendMsg("No Print Paper ");
                            } else if(ret == -2) {
                                SendMsg("too hot");
                            }else if(ret == -3) {
                                SendMsg("low voltage");
                            }else{
                                SendMsg("Print fail");
                            }
                        } else {
                            RESULT_CODE = 0;
                            SendMsg("Print Finish");
                        }

                        break;

                    case PRINT_BMP:
                        SendMsg("PRINT_BMP");
                        msg.what = DISABLE_RG;
                        handler.sendMessage(msg);
                        //0 left，1 middle ，2 right
                        //Print.Lib_PrnSetAlign(0);
                        Bitmap bmp = BitmapFactory.decodeResource(MainActivity.this.getResources(),
                                R.mipmap.pic);
                        ret = posApiHelper.PrintBmp(bmp);
                        posApiHelper.PrintStr("  www.androidposkenya.com\n");
                        posApiHelper.PrintStr("Powered by Renotech Systems\n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        if (ret == 0) {
                            posApiHelper.PrintStr("\n\n\n");

                            SendMsg("Printing... ");
                            ret = posApiHelper.PrintStart();

                            msg1.what = ENABLE_RG;
                            handler.sendMessage(msg1);

                            Log.d("", "Lib_PrnStart ret = " + ret);
                            if (ret != 0) {
                                RESULT_CODE = -1;
                                Log.e("liuhao", "Lib_PrnStart fail, ret = " + ret);
                                if (ret == -1) {
                                    SendMsg("No Print Paper ");
                                } else if(ret == -2) {
                                    SendMsg("too hot ");
                                }else if(ret == -3) {
                                    SendMsg("low voltage ");
                                }else{
                                    SendMsg("Print fail ");
                                }
                            } else {
                                RESULT_CODE = 0;
                                SendMsg("Print Finish ");
                            }
                        } else {
                            RESULT_CODE = -1;
                            SendMsg("Lib_PrnBmp Failed");
                        }

                        break;

                    default:
                        break;
                }
                m_bThreadFinished = true;

                Log.e(tag, "goToSleep2...");
            }
        }
    }

    private int getValue() {
        prismAppSp = getSharedPreferences("Gray", MODE_PRIVATE);
        return prismAppSp.getInt("value", 4);
    }

    public void SendMsg(String strInfo) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("MSG", strInfo);
        msg.setData(b);
        handler.sendMessage(msg);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case DISABLE_RG:
                    IsWorking = 1;
                    break;

                case ENABLE_RG:
                    IsWorking = 0;
                    break;
                default:
                    Bundle b = msg.getData();
                    final String strInfo = b.getString("MSG");
                    textViewMsg = findViewById(R.id.catchErr);
                    textViewMsg.setText(strInfo);
                    int shortAnimationDuration = 2000;
                    textViewMsg.setAlpha(0f);
                    textViewMsg.setVisibility(View.VISIBLE);
                    textViewMsg.animate()
                            .alpha(1f)
                            .setDuration(shortAnimationDuration)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    textViewMsg.setVisibility(View.INVISIBLE);
                                }
                            });

                    break;
            }
        }
    };

    public class BatteryReceiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public void onReceive(Context context, Intent intent) {
            voltage_level = Objects.requireNonNull(intent.getExtras()).getInt("level");
            System.out.println("Battery shitOne" + voltage_level);
            Log.e("wbw", "current  = " + voltage_level);
            BatteryV = intent.getIntExtra("voltage", 0);
            System.out.println("Battery shitTwo" + BatteryV);
            Log.e("wbw", "BatteryV  = " + BatteryV);
            Log.e("wbw", "V  = " + BatteryV * 2 / 100);
            //	m_voltage = (int) (65+19*voltage_level/100);
            //   Log.e("wbw","m_voltage  = " + m_voltage );
        }
    }

    //Remove M-PESA BALANCE
    public void ext_bal() {
        String new1 = StringUtils.substringBefore(text, " New ");
        String new2 = StringUtils.substringAfter(text, "Transaction ");
        Log.e("ext_bal: ", "New Msg: " + new1 + " Transaction " + new2);
        ext_text = new1 + " Transaction " + new2;
    }

    public void headFootFunc(View v) {
        myHeader = header.getText().toString();
        myFooter = footer.getText().toString();
        SharedPreferences.Editor editor = myData.edit();
        editor.putString("Header", myHeader);
        editor.putString("Footer", myFooter);
        editor.apply();
    }
}