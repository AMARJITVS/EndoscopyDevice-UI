package com.htic.amar.endoscopy;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    StringBuffer readSB = new StringBuffer();

    /* thread to read the data */
    public handler_thread handlerThread;

    /* declare a FT311 UART interface variable */
    public FT311UARTInterface uartInterface;

    //Button declaration
    ImageButton home,hd,light,whtbal1,whtbal2,enhance,extin,pump,avepeak;

    /* local variables */
    byte[] writeBuffer;
    byte[] readBuffer;
    char[] readBufferToChar;
    int[] actualNumBytes;
    public boolean hdcheck=false,lightcheck=false,whtbalcheck1=false,whtbalcheck2=false,extincheck=false,pumpcheck=false,avepeakcheck=false;
    public int enhancecheck=0;
    int numBytes;
    byte status;

    //Configuration settings
    public static int baudRate = 115200; /* baud rate */
    public static byte stopBit = 1; /* 1:1stop bits, 2:2 stop bits */
    public static byte dataBit = 8; /* 8:8bit, 7: 7bit */
    public static byte parity = 0; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
    public static byte flowControl = 0; /* 0:none, 1: flow control(CTS,RTS) */

    //Context
    public Context global_context;
    //Configure check
    public boolean bConfiged = false;
    //Preferences
    public SharedPreferences sharePrefSettings;
    //Intent action
    public String act_string;

    public ImageView homeview,hdview,lightview,wbview1,wbview2,enview1,enview2,enview3,extinview,pumpview1,avepeakview1,avepeakview2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("APP", "STARTED");

        //Widget declaration
        home = (ImageButton) findViewById(R.id.home);
        hd = (ImageButton) findViewById(R.id.hd);
        light = (ImageButton) findViewById(R.id.light);
        whtbal1 = (ImageButton) findViewById(R.id.whtbal1);
        whtbal2 = (ImageButton) findViewById(R.id.whtbal2);
        enhance = (ImageButton) findViewById(R.id.enhance);
        extin = (ImageButton) findViewById(R.id.extin);
        pump = (ImageButton) findViewById(R.id.pump);
        avepeak = (ImageButton) findViewById(R.id.avepeak);

        homeview = (ImageView) findViewById(R.id.homeview);
        hdview = (ImageView) findViewById(R.id.hdview);
        lightview = (ImageView) findViewById(R.id.lightview);
        wbview1 = (ImageView) findViewById(R.id.whtbalview);
        wbview2 = (ImageView) findViewById(R.id.whtbalview2);
        enview1 = (ImageView) findViewById(R.id.enhanceview1);
        enview2 = (ImageView) findViewById(R.id.enhanceview2);
        enview3 = (ImageView) findViewById(R.id.enhanceview3);
        extinview = (ImageView) findViewById(R.id.extinview);
        pumpview1 = (ImageView) findViewById(R.id.pumpview1);
        avepeakview1 = (ImageView) findViewById(R.id.avepeakview1);
        avepeakview2 = (ImageView) findViewById(R.id.avepeakview2);


        sharePrefSettings = getSharedPreferences("UARTLBPref", 0);

        global_context = this;

       		/* allocate buffer */
        writeBuffer = new byte[64];
        readBuffer = new byte[4096];
        readBufferToChar = new char[4096];
        actualNumBytes = new int[1];

        act_string = getIntent().getAction();
        if (-1 != act_string.indexOf("android.intent.action.MAIN")) {
            restorePreference();
        } else if (-1 != act_string.indexOf("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")) {
            cleanPreference();
        }

        home.setOnTouchListener(new View.OnTouchListener () {
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // The user just touched the screen
                        homeview.setImageResource(R.drawable.oval2);
                        if (!bConfiged) {
                            bConfiged = true;
                            uartInterface.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
                            savePreference();
                            Toast.makeText(global_context, "CONFIGURED", Toast.LENGTH_SHORT).show();
                        }
                        if (FT311UARTInterface.checks) {
                            readSB.delete(0, readSB.length());
                            writeData("0");
                            enhancelow();
                            average();
                        } else {
                            Toast.makeText(global_context, "DEVICE NOT CONFIGURED", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // The touch just ended
                        homeview.setImageResource(R.drawable.oval);
                        break;
                }

                return false;
            }
        });
        enhance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FT311UARTInterface.checks) {
                    readSB.delete(0, readSB.length());
                    if(enhancecheck==0) {
                       enhancelow();

                    }
                    else if(enhancecheck==1) {
                        writeData("8");
                        enhancecheck++;
                        enview1.setImageResource(R.drawable.oval);
                        enview2.setImageResource(R.drawable.oval2);
                        enview3.setImageResource(R.drawable.oval);
                        enhance.setBackgroundResource(R.drawable.button_enhance_mid);
                    }
                    else if(enhancecheck==2) {
                        writeData("9");
                        enhancecheck=0;
                        enview1.setImageResource(R.drawable.oval);
                        enview2.setImageResource(R.drawable.oval);
                        enview3.setImageResource(R.drawable.oval2);
                        enhance.setBackgroundResource(R.drawable.button_enhance_high);
                    }
                } else {
                    Toast.makeText(global_context, "DEVICE NOT CONFIGURED...PRESS HOME...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        hd.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                if (FT311UARTInterface.checks) {
                    readSB.delete(0, readSB.length());
                    if(!hdcheck) {
                       mBluon();
                        if(lightcheck)
                            ledoff();
                    }
                    else if(hdcheck) {
                       mBluoff();
                    }
                } else {
                    Toast.makeText(global_context, "DEVICE NOT CONFIGURED...PRESS HOME...", Toast.LENGTH_SHORT).show();
                }
            }
            // }

        });

        light.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                if (FT311UARTInterface.checks) {
                    readSB.delete(0, readSB.length());
                    if(!lightcheck) {
                       ledon();
                        if(hdcheck)
                            mBluoff();
                    }
                    else if(lightcheck) {
                        ledoff();
                    }
                } else {
                    Toast.makeText(global_context, "DEVICE NOT CONFIGURED...PRESS HOME...", Toast.LENGTH_SHORT).show();
                }
            }


        });

        extin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FT311UARTInterface.checks) {
                    readSB.delete(0, readSB.length());
                    if(!extincheck) {
                        writeData("10");
                        extincheck=true;
                        extinview.setImageResource(R.drawable.oval2);
                        extin.setBackgroundResource(R.drawable.button_extin);
                    }
                    else if(extincheck) {
                        writeData("11");
                        extincheck=false;
                        extinview.setImageResource(R.drawable.oval);
                        extin.setBackgroundResource(R.drawable.button_back);
                    }
                } else {
                    Toast.makeText(global_context, "DEVICE NOT CONFIGURED...PRESS HOME...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        pump.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                if (FT311UARTInterface.checks) {
                    readSB.delete(0, readSB.length());
                    if(!pumpcheck) {
                        writeData("12");
                        pumpcheck=true;
                        pumpview1.setImageResource(R.drawable.oval2);
                        pump.setBackgroundResource(R.drawable.button_pump);
                    }
                    else if(pumpcheck) {
                        writeData("13");
                        pumpcheck=false;
                        pumpview1.setImageResource(R.drawable.oval);
                        pump.setBackgroundResource(R.drawable.button_pump2);
                    }
                } else {
                    Toast.makeText(global_context, "DEVICE NOT CONFIGURED...PRESS HOME...", Toast.LENGTH_SHORT).show();
                }
            }
            // }

        });

        avepeak.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                if (FT311UARTInterface.checks) {
                    readSB.delete(0, readSB.length());
                    if(!avepeakcheck) {
                       average();
                    }
                    else if(avepeakcheck) {
                        writeData("15");
                        avepeakcheck=false;
                        avepeakview1.setImageResource(R.drawable.oval);
                        avepeakview2.setImageResource(R.drawable.oval2);
                        avepeak.setBackgroundResource(R.drawable.button_back2);
                    }
                } else {
                    Toast.makeText(global_context, "DEVICE NOT CONFIGURED...PRESS HOME...", Toast.LENGTH_SHORT).show();
                }
            }


        });

        whtbal1.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                if (FT311UARTInterface.checks) {
                    readSB.delete(0, readSB.length());

                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            for(int i=0;i<2;i++) {
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        if (!whtbalcheck1) {
                                            writeData("5");
                                            whtbalcheck1 = true;
                                            wbview1.setImageResource(R.drawable.oval2);
                                            whtbal1.setBackgroundResource(R.drawable.button_wb3);
                                        } else if (whtbalcheck1) {
                                            whtbalcheck1 = false;
                                            wbview1.setImageResource(R.drawable.oval);
                                            whtbal1.setBackgroundResource(R.drawable.button_wb);
                                        }
                                    }
                                });
                                // sleep to slow down the add of entries
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    // manage error ...
                                }
                            }
                        }
                    }).start();

                } else {
                    Toast.makeText(global_context, "DEVICE NOT CONFIGURED...PRESS HOME...", Toast.LENGTH_SHORT).show();
                }
            }
        });
        whtbal2.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                if (FT311UARTInterface.checks) {
                    readSB.delete(0, readSB.length());
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            for(int i=0;i<2;i++) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                    if(!whtbalcheck2) {
                        writeData("6");
                        whtbalcheck2=true;
                        wbview2.setImageResource(R.drawable.oval2);
                        whtbal2.setBackgroundResource(R.drawable.button_wb4);
                    }
                    else if(whtbalcheck2) {
                        whtbalcheck2=false;
                        wbview2.setImageResource(R.drawable.oval);
                        whtbal2.setBackgroundResource(R.drawable.button_wb2);
                    }
                                    }
                                });
                                // sleep to slow down the add of entries
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    // manage error ...
                                }
                            }
                        }
                    }).start();

                } else {
                    Toast.makeText(global_context, "DEVICE NOT CONFIGURED...PRESS HOME...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //FT311UARTInterface call statement
        uartInterface = new FT311UARTInterface(this, sharePrefSettings);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Start read handler
        handlerThread = new handler_thread(handler);

        handlerThread.start();

    }
    private void mBluon()
    {
        writeData("1");
        hdcheck=true;
        hdview.setImageResource(R.drawable.oval2);
        hd.setBackgroundResource(R.drawable.button_mblu);
    }
    private void mBluoff()
    {
        writeData("2");
        hdcheck=false;
        hdview.setImageResource(R.drawable.oval);
        hd.setBackgroundResource(R.drawable.button_back);
    }
    private void ledon()
    {
        writeData("3");
        lightcheck=true;
        lightview.setImageResource(R.drawable.oval2);
        light.setBackgroundResource(R.drawable.button_led);
    }
    private void ledoff()
    {
        writeData("4");
        lightcheck=false;
        lightview.setImageResource(R.drawable.oval);
        light.setBackgroundResource(R.drawable.button_back);
    }
    private void enhancelow()
    {
        writeData("7");
        enhancecheck++;
        enview1.setImageResource(R.drawable.oval2);
        enview2.setImageResource(R.drawable.oval);
        enview3.setImageResource(R.drawable.oval);
        enhance.setBackgroundResource(R.drawable.button_back4);
    }
    private void average()
    {
        writeData("14");
        avepeakcheck=true;
        avepeakview1.setImageResource(R.drawable.oval2);
        avepeakview2.setImageResource(R.drawable.oval);
        avepeak.setBackgroundResource(R.drawable.button_back3);
    }

    //Cleaning preferences
    protected void cleanPreference() {
        SharedPreferences.Editor editor = sharePrefSettings.edit();
        editor.remove("configed");
        editor.remove("baudRate");
        editor.remove("stopBit");
        editor.remove("dataBit");
        editor.remove("parity");
        editor.remove("flowControl");
        editor.apply();
    }

    //Save preferences
    protected void savePreference() {
        if (true == bConfiged) {
            sharePrefSettings.edit().putString("configed", "TRUE").apply();
            sharePrefSettings.edit().putInt("baudRate", baudRate).apply();
            sharePrefSettings.edit().putInt("stopBit", stopBit).apply();
            sharePrefSettings.edit().putInt("dataBit", dataBit).apply();
            sharePrefSettings.edit().putInt("parity", parity).apply();
            sharePrefSettings.edit().putInt("flowControl", flowControl).apply();
        } else {
            sharePrefSettings.edit().putString("configed", "FALSE").apply();
        }
    }

    //Restore preferences
    protected void restorePreference() {
        String key_name = sharePrefSettings.getString("configed", "");
        if (key_name.contains("TRUE")) {
            bConfiged = true;
        } else {
            bConfiged = false;
        }

        baudRate = sharePrefSettings.getInt("baudRate", 115200);
        stopBit = (byte) sharePrefSettings.getInt("stopBit", 1);
        dataBit = (byte) sharePrefSettings.getInt("dataBit", 8);
        parity = (byte) sharePrefSettings.getInt("parity", 0);
        flowControl = (byte) sharePrefSettings.getInt("flowControl", 0);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Always makes screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Calling Resume accessory
        if (2 == uartInterface.ResumeAccessory()) {
            cleanPreference();
            restorePreference();
        }

    }

    @Override
    protected void onDestroy() {
        //Clears Always screen on condition
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Destroys accessory
        uartInterface.DestroyAccessory(bConfiged);


        super.onDestroy();
    }


    //Handler to receive data from MC
    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            for (int i = 0; i < actualNumBytes[0]; i++) {
                readBufferToChar[i] = (char) readBuffer[i];
            }
            appendData(readBufferToChar, actualNumBytes[0]);
        }
    };

    /* usb input data handler */
    private class handler_thread extends Thread {
        Handler mHandler;

        /* constructor */
        handler_thread(Handler h) {

            mHandler = h;
        }


        public void run() {
            Message msg;

            while (true) {

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {

                }

                status = uartInterface.ReadData(4096, readBuffer, actualNumBytes);

                if (status == 0x00 && actualNumBytes[0] > 0) {
                    msg = mHandler.obtainMessage();
                    mHandler.sendMessage(msg);
                }

            }
        }
    }


    //Writes or sends data to the MC
    public void writeData(String data) {
        String srcStr = data;
        String destStr = "";

        destStr = srcStr;

        numBytes = destStr.length();
        for (int i = 0; i < numBytes; i++) {
            writeBuffer[i] = (byte) destStr.charAt(i);
        }
        uartInterface.SendData(numBytes, writeBuffer);


    }


    //USING RECEIVED DATA FROM MC
    public void appendData(char[] data, int len) {
        if (len >= 1)
            readSB.append(String.copyValueOf(data, 0, len));
    }
}













