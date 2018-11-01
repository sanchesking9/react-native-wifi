package com.mediatek.demo.smartconnection;

import com.broadcom.cooee.Cooee;
import com.facebook.react.uimanager.*;
import com.facebook.react.bridge.*;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import voice.encoder.VoicePlayer;
import voice.encoder.DataEncoder;

import java.util.ArrayList;

public class RNWifiModule extends ReactContextBaseJavaModule {
    private VoicePlayer player = new VoicePlayer();

    private String sendMac = null;
    private String wifiName;
    private String currentBssid;
    private int mLocalIp;
    private JniLoader loader;

    private boThread boThread;

    private String localPassword;

    public RNWifiModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "WifiManager";
    }

    @ReactMethod
    public void list(Callback successCallback, Callback errorCallback) {
        try {
            WifiManager mWifiManager = (WifiManager) getReactApplicationContext()
                    .getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (mWifiManager.getWifiState() == mWifiManager.WIFI_STATE_ENABLED) {
                mWifiManager.disconnect();
                mWifiManager.reconnect();
            } else {
                mWifiManager.setWifiEnabled(true);
            }

            WritableArray wifiArray = Arguments.createArray();
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            String wifiName = wifiInfo.getSSID();
            if (wifiName.length() > 2 && wifiName.charAt(0) == '"'
                    && wifiName.charAt(wifiName.length() - 1) == '"') {
                wifiName = wifiName.substring(1, wifiName.length() - 1);
            }

            wifiArray.pushString(wifiName);

            successCallback.invoke(wifiArray);
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void getWifi(String wifi, Callback result) {
        WifiManager wifiMan = (WifiManager) getReactApplicationContext()
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        WifiInfo wifiInfo = wifiMan.getConnectionInfo();
        mLocalIp = wifiInfo.getIpAddress();
        wifiName = wifiInfo.getSSID();
        if (wifiName.length() > 2 && wifiName.charAt(0) == '"'
                && wifiName.charAt(wifiName.length() - 1) == '"') {
            wifiName = wifiName.substring(1, wifiName.length() - 1);
        }

        List<ScanResult> wifiList = wifiMan.getScanResults();
        ArrayList<String> mList = new ArrayList<String>();
        mList.clear();

        for (int i = 0; i < wifiList.size(); i++) {
            mList.add((wifiList.get(i).BSSID).toString());

        }

        currentBssid = wifiInfo.getBSSID();

        if (currentBssid == null) {
            for (int i = 0; i < wifiList.size(); i++) {
                if ((wifiList.get(i).SSID).toString().equals(wifiName)) {
                    currentBssid = (wifiList.get(i).BSSID).toString();
                    break;
                }
            }
        }
        else {
            if (currentBssid.equals("00:00:00:00:00:00")
                    || currentBssid.equals("")) {
                for (int i = 0; i < wifiList.size(); i++) {
                    if ((wifiList.get(i).SSID).toString().equals(wifiName)) {
                        currentBssid = (wifiList.get(i).BSSID).toString();
                        break;
                    }
                }
            }
        }
        if (currentBssid == null) {
            return;
        }

        String tomacaddress[] = currentBssid.split(":");
        int currentLen = currentBssid.split(":").length;

        for (int m = currentLen - 1; m > -1; m--) {
            for (int j = mList.size() - 1; j > -1; j--) {
                if (!currentBssid.equals(mList.get(j))) {
                    String array[] = mList.get(j).split(":");
                    if (!tomacaddress[m].equals(array[m])) {
                        mList.remove(j);
                    }
                }
            }

            if (mList.size() == 1 || mList.size() == 0) {
                if (m == 5) {
                    sendMac = tomacaddress[m - 1].toString() + tomacaddress[m].toString();
                } else if (m == 4) {
                    sendMac = tomacaddress[m].toString()
                            + tomacaddress[m + 1].toString();
                } else if (m == 3) {
                    sendMac = tomacaddress[m].toString()
                            + tomacaddress[m + 1].toString()
                            + tomacaddress[m + 2].toString();
                } else if (m == 2) {
                    sendMac = tomacaddress[m].toString()
                            + tomacaddress[m + 1].toString()
                            + tomacaddress[m + 2].toString()
                            + tomacaddress[m + 3].toString();
                } else if (m == 1) {
                    sendMac = tomacaddress[m].toString()
                            + tomacaddress[m + 1].toString()
                            + tomacaddress[m + 2].toString()
                            + tomacaddress[m + 3].toString()
                            + tomacaddress[m + 4].toString();
                } else {
                    sendMac = tomacaddress[m].toString()
                            + tomacaddress[m + 1].toString()
                            + tomacaddress[m + 2].toString()
                            + tomacaddress[m + 3].toString()
                            + tomacaddress[m + 4].toString()
                            + tomacaddress[m + 5].toString();
                }
                break;
            } else {
                sendMac = tomacaddress[4].toString()
                        + tomacaddress[4 + 1].toString();
                break;
            }
        }

        result.invoke(sendMac);
    }

    @ReactMethod
    public void sendSonic(String macN, final String wifiN, Callback result) {
        boolean res = JniLoader.LoadLib();
        Log.e("SmartConnection", "Load Smart Connection Library Result ：" + res);
        loader = new JniLoader();
        int proV = loader.GetProtoVersion();
        Log.e("SmartConnection", "proV ：" + proV);
        int libV = loader.GetLibVersion();
        Log.e("SmartConnection", "libV ：" + libV);

        int count = 5;

        int sendV1 = 1;
        int sendV4 = 1;
        int sendV5 = 1;

        float oI = 0.0f;
        float nI = 0.0f;

        if (sendV1 == 0 && sendV4 == 0 && sendV5 == 0) {
            return;
        }

        int retValue = JniLoader.ERROR_CODE_OK;
        String key = "";
        String mac="0xff 0xff 0xff 0xff 0xff 0xff";
        String mac1="";
        if(key==null) {
            Log.e("SmartConnection", "init Smart key is null");
        }else{
            Log.e("SmartConnection", "init Smart key-len="+key.length()+", key-emp="+key.isEmpty());
        }
        Log.e("SmartConnection", "init Smart key=" + key+", sendV1="+sendV1+", sendV4="+sendV4+", sendV5="+sendV5);
        retValue = loader.InitSmartConnection(key,mac,sendV1, sendV4, sendV5);
        Log.e("SmartConnection", "init return retValue=" + retValue);
        if (retValue != JniLoader.ERROR_CODE_OK) {
            return;
        }

        Log.e("SmartConnection", "Send Smart oI=" + oI+", nI="+nI);
        loader.SetSendInterval(oI, nI);

        String SSID = wifiName;
        localPassword = wifiN;
        String Custom = "";
        if(Custom==null) {
            Log.e("SmartConnection", "Start Smart Custom is null");
        }else{
            Log.e("SmartConnection", "Start Smart Custom-len="+Custom.length()+", Custom-emp="+Custom.isEmpty());
        }
        Log.e("SmartConnection", "Start Smart SSID=" + SSID + ", Password=" + localPassword + ", Custom=" + Custom+"sendMac"+sendMac);
        retValue = loader.StartSmartConnection(SSID, localPassword, Custom);
        Log.e("localPassword", localPassword);
        Log.e("sendMac", sendMac);
        afterSendSonic(sendMac, localPassword.toString(), count);
        Log.e("SmartConnection", "start return retValue=" + retValue);
        if (retValue != JniLoader.ERROR_CODE_OK) {
            return;
        }
        rHandler.sendEmptyMessageDelayed(1,7000);

        result.invoke(count);
    }

    @ReactMethod
    public void stopConnect() {

        int retValue = loader.StopSmartConnection();
        Log.e("SmartConnection", "Stop return failed : " + retValue);

        player.stop();
        if (boThread != null && boThread.isAlive()){
            boThread.interrupt();
        }
    }

    private void afterSendSonic(String mac, final String wifi, int count) {
        byte[] midbytes = null;

        try {
            midbytes = HexString2Bytes(mac);
            printHexString(midbytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (midbytes.length > 6)
        {
            Toast.makeText(getReactApplicationContext(), "no support",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] b = null;
        int num = 0;
        if (midbytes.length == 2) {
            b = new byte[] { midbytes[0], midbytes[1] };
            num = 2;
        } else if (midbytes.length == 3) {
            b = new byte[] { midbytes[0], midbytes[1], midbytes[2] };
            num = 3;
        } else if (midbytes.length == 4) {
            b = new byte[] { midbytes[0], midbytes[1], midbytes[2], midbytes[3] };
            num = 4;
        } else if (midbytes.length == 5) {
            b = new byte[] { midbytes[0], midbytes[1], midbytes[2],
                    midbytes[3], midbytes[4] };
            num = 5;
        } else if (midbytes.length == 6) {
            b = new byte[] { midbytes[0], midbytes[1], midbytes[2],
                    midbytes[3], midbytes[4], midbytes[5] };
            num = 6;
        } else if (midbytes.length == 1) {
            b = new byte[] { midbytes[0] };
            num = 1;
        }

        int a[] = new int[19];
        a[0] = 6500;
        int i, j;
        for (i = 0; i < 18; i++)
        {
            a[i + 1] = a[i] + 200;
        }

        player.setFreqs(a);

        player.play(DataEncoder.encodeMacWiFi(b, wifi.trim()), count, 1000);
    }

    private class boThread extends Thread {
        private boolean isRun = false;
        private String mssid;
        private String mpwd;
        private int mip;

        private boThread(String ssid, String pwd, int ip, boolean run) {
            mssid = ssid;
            mpwd = pwd;
            mip = ip;
            isRun = run;
        }

        @Override
        public void interrupt() {
            super.interrupt();
            isRun = false;
        }

        @Override
        public void run() {
            super.run();
            while (isRun) {
                Log.e("api","cooee");
                Cooee.send(mssid, mpwd, mip);
            }
        }
    }

    private Handler rHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if(loader!=null) {
                        int retValue = loader.StopSmartConnection();
                        if (retValue != JniLoader.ERROR_CODE_OK) {

                        }
                    }
                    String SSID = wifiName;
                    String Password = localPassword;
                    boThread = new boThread(SSID, Password.trim(), mLocalIp, true);
                    boThread.start();

                    break;
                case 2:
                    break;
                case 3:
                    break;

            }
        }
    };

    private static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    private static byte[] HexString2Bytes(String src) {
        byte[] ret = new byte[src.length() / 2];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < src.length() / 2; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    private static void printHexString(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            System.out.print("aaa" + hex.toUpperCase() + " ");
        }
        System.out.println("");
    }
}