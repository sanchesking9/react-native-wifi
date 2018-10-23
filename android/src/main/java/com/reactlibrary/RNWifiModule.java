package com.reactlibrary;

import com.facebook.react.uimanager.*;
import com.facebook.react.bridge.*;
import com.facebook.systrace.Systrace;
import com.facebook.systrace.SystraceMessage;
// import com.facebook.react.LifecycleState;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;

import android.provider.Settings;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import java.util.List;
import java.lang.Thread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// custom
import voice.encoder.VoicePlayer;
import voice.encoder.DataEncoder;
import java.util.ArrayList;

public class RNWifiModule extends ReactContextBaseJavaModule {
  private VoicePlayer player = new VoicePlayer();

  private String sendMac = null;
  private String wifiName;
  private String currentBssid;

	//WifiManager Instance
	WifiManager wifi;
	ReactApplicationContext context;

	//Constructor
	public RNWifiModule(ReactApplicationContext reactContext) {
		super(reactContext);
		wifi = (WifiManager)reactContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		context = (ReactApplicationContext) getReactApplicationContext();
	}

	//Name for module register to use:
	@Override
	public String getName() {
		return "WifiManager";
	}

	//Method to load wifi list into string via Callback. Returns a stringified JSONArray
	@ReactMethod
	public void loadWifiList(Callback successCallback, Callback errorCallback) {
		try {
			List < ScanResult > results = wifi.getScanResults();
			JSONArray wifiArray = new JSONArray();

			for (ScanResult result: results) {
				JSONObject wifiObject = new JSONObject();
				if(!result.SSID.equals("")){
					try {
            wifiObject.put("SSID", result.SSID);
            wifiObject.put("BSSID", result.BSSID);
            wifiObject.put("capabilities", result.capabilities);
            wifiObject.put("frequency", result.frequency);
            wifiObject.put("level", result.level);
            wifiObject.put("timestamp", result.timestamp);
            //Other fields not added
            //wifiObject.put("operatorFriendlyName", result.operatorFriendlyName);
            //wifiObject.put("venueName", result.venueName);
            //wifiObject.put("centerFreq0", result.centerFreq0);
            //wifiObject.put("centerFreq1", result.centerFreq1);
            //wifiObject.put("channelWidth", result.channelWidth);
					} catch (JSONException e) {
          	errorCallback.invoke(e.getMessage());
					}
					wifiArray.put(wifiObject);
				}
			}
			successCallback.invoke(wifiArray.toString());
		} catch (IllegalViewOperationException e) {
			errorCallback.invoke(e.getMessage());
		}
	}

	  @ReactMethod
      public void list(Callback successCallback, Callback errorCallback) {
        try {
          WifiManager mWifiManager = (WifiManager) getReactApplicationContext().getSystemService(Context.WIFI_SERVICE);
          if (mWifiManager.getWifiState() == mWifiManager.WIFI_STATE_ENABLED) {
              mWifiManager.disconnect();
              mWifiManager.reconnect();
          } else {
              mWifiManager.setWifiEnabled(true);
          }
          List < ScanResult > results = mWifiManager.getScanResults();
          WritableArray wifiArray =  Arguments.createArray();
          for (ScanResult result: results) {
            if(!result.SSID.equals("")){
              wifiArray.pushString(result.SSID);
            }
          }
          successCallback.invoke(wifiArray);
        } catch (IllegalViewOperationException e) {
          errorCallback.invoke(e.getMessage());
        }
      }

	//Method to force wifi usage if the user needs to send requests via wifi
	//if it does not have internet connection. Useful for IoT applications, when
	//the app needs to communicate and send requests to a device that have no
	//internet connection via wifi.

	//Receives a boolean to enable forceWifiUsage if true, and disable if false.
	//Is important to enable only when communicating with the device via wifi
	//and remember to disable it when disconnecting from device.
	@ReactMethod
	public void forceWifiUsage(boolean useWifi) {
        boolean canWriteFlag = false;

        if (useWifi) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    canWriteFlag = Settings.System.canWrite(context);

                    if (!canWriteFlag) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + context.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        context.startActivity(intent);
                    }

                }


                if (((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && canWriteFlag) || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M))) {
                    final ConnectivityManager manager = (ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkRequest.Builder builder;
                    builder = new NetworkRequest.Builder();
                    //set the transport type do WIFI
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);


                    manager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                manager.bindProcessToNetwork(network);
                            } else {
                                //This method was deprecated in API level 23
                                ConnectivityManager.setProcessDefaultNetwork(network);
                            }
                            try {
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            manager.unregisterNetworkCallback(this);
                        }
                    });
                }


            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ConnectivityManager manager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                manager.bindProcessToNetwork(null);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ConnectivityManager.setProcessDefaultNetwork(null);
            }
        }
    }

	//Method to check if wifi is enabled
	@ReactMethod
	public void isEnabled(Callback isEnabled) {
		isEnabled.invoke(wifi.isWifiEnabled());
	}

	//Method to connect/disconnect wifi service
	@ReactMethod
	public void setEnabled(Boolean enabled) {
		wifi.setWifiEnabled(enabled);
	}

	//Send the ssid and password of a Wifi network into this to connect to the network.
	//Example:  wifi.findAndConnect(ssid, password);
	//After 10 seconds, a post telling you whether you are connected will pop up.
	//Callback returns true if ssid is in the range
	@ReactMethod
	public void connectToProtectedSSID(String ssid, String password, Boolean isWep, Promise promise) {
		List < ScanResult > results = wifi.getScanResults();
		boolean connected = false;
		for (ScanResult result: results) {
			String resultString = "" + result.SSID;
			if (ssid.equals(resultString)) {
				connected = connectTo(result, password, ssid);
			}
		}
		if (connected) {
			promise.resolve(true);
		} else {
			promise.reject("Can't connect to wifi!", "Can't connect to wifi!");
		}
	}

	//Use this method to check if the device is currently connected to Wifi.
	@ReactMethod
	public void connectionStatus(Callback connectionStatusResult) {
		ConnectivityManager connManager = (ConnectivityManager) getReactApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (mWifi.isConnected()) {
			connectionStatusResult.invoke(true);
		} else {
			connectionStatusResult.invoke(false);
		}
	}

	//Method to connect to WIFI Network
	public Boolean connectTo(ScanResult result, String password, String ssid) {
		//Make new configuration
		WifiConfiguration conf = new WifiConfiguration();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        conf.SSID = ssid;
    } else {
        conf.SSID = "\"" + ssid + "\"";
    }

		String capabilities = result.capabilities;

		if (capabilities.contains("WPA")  ||
          capabilities.contains("WPA2") ||
          capabilities.contains("WPA/WPA2 PSK")) {

	    // appropriate ciper is need to set according to security type used,
	    // ifcase of not added it will not be able to connect
	    conf.preSharedKey = "\"" + password + "\"";

	    conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

	    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

	    conf.status = WifiConfiguration.Status.ENABLED;

	    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
	    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

	    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

	    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
	    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

	    conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
	    conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

		}	else if (capabilities.contains("WEP")) {
			conf.wepKeys[0] = "\"" + password + "\"";
			conf.wepTxKeyIndex = 0;
			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

		} else {
			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		}

		//Remove the existing configuration for this netwrok
		List<WifiConfiguration> mWifiConfigList = wifi.getConfiguredNetworks();

		int updateNetwork = -1;

		for(WifiConfiguration wifiConfig : mWifiConfigList){
			if(wifiConfig.SSID.equals(conf.SSID)){
				conf.networkId = wifiConfig.networkId;
				updateNetwork = wifi.updateNetwork(conf);
			}
		}

    // If network not already in configured networks add new network
		if ( updateNetwork == -1 ) {
      updateNetwork = wifi.addNetwork(conf);
      wifi.saveConfiguration();
		};

    if ( updateNetwork == -1 ) {
      return false;
    }

    boolean disconnect = wifi.disconnect();
		if ( !disconnect ) {
			return false;
		};

		boolean enableNetwork = wifi.enableNetwork(updateNetwork, true);
		if ( !enableNetwork ) {
			return false;
		};

		return true;
	}

	//Disconnect current Wifi.
	@ReactMethod
	public void disconnect() {
		wifi.disconnect();
	}

	//This method will return current ssid
	@ReactMethod
	public void getCurrentWifiSSID(final Promise promise) {
		WifiInfo info = wifi.getConnectionInfo();

		// This value should be wrapped in double quotes, so we need to unwrap it.
		String ssid = info.getSSID();
		if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
			ssid = ssid.substring(1, ssid.length() - 1);
		}

		promise.resolve(ssid);
	}

	//This method will return the basic service set identifier (BSSID) of the current access point
	@ReactMethod
	public void getBSSID(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();

		String bssid = info.getBSSID();

		callback.invoke(bssid.toUpperCase());
	}

	//This method will return current wifi signal strength
	@ReactMethod
	public void getCurrentSignalStrength(final Callback callback) {
		int linkSpeed = wifi.getConnectionInfo().getRssi();
		callback.invoke(linkSpeed);
	}

	//This method will return current wifi frequency
	@ReactMethod
	public void getFrequency(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();
		int frequency = info.getFrequency();
		callback.invoke(frequency);
	}

	//This method will return current IP
	@ReactMethod
	public void getIP(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();
		String stringip = longToIP(info.getIpAddress());
		callback.invoke(stringip);
	}

	//This method will remove the wifi network as per the passed SSID from the device list
	@ReactMethod
	public void isRemoveWifiNetwork(String ssid, final Callback callback) {
    List<WifiConfiguration> mWifiConfigList = wifi.getConfiguredNetworks();
    for (WifiConfiguration wifiConfig : mWifiConfigList) {
				String comparableSSID = ('"' + ssid + '"'); //Add quotes because wifiConfig.SSID has them
				if(wifiConfig.SSID.equals(comparableSSID)) {
					wifi.removeNetwork(wifiConfig.networkId);
					wifi.saveConfiguration();
					callback.invoke(true);
					return;
				}
    }
		callback.invoke(false);
	}

	// This method is similar to `loadWifiList` but it forcefully starts the wifi scanning on android and in the callback fetches the list
	@ReactMethod
	public void reScanAndLoadWifiList(Callback successCallback, Callback errorCallback) {
		WifiReceiver receiverWifi = new WifiReceiver(wifi, successCallback, errorCallback);
   	getReactApplicationContext().getCurrentActivity().registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    wifi.startScan();
	}

	public static String longToIP(int longIp){
		StringBuffer sb = new StringBuffer("");
		String[] strip=new String[4];
		strip[3]=String.valueOf((longIp >>> 24));
		strip[2]=String.valueOf((longIp & 0x00FFFFFF) >>> 16);
		strip[1]=String.valueOf((longIp & 0x0000FFFF) >>> 8);
		strip[0]=String.valueOf((longIp & 0x000000FF));
		sb.append(strip[0]);
		sb.append(".");
		sb.append(strip[1]);
		sb.append(".");
		sb.append(strip[2]);
		sb.append(".");
		sb.append(strip[3]);
		return sb.toString();
	}

	class WifiReceiver extends BroadcastReceiver {

			private Callback successCallback;
			private Callback errorCallback;
			private WifiManager wifi;

			public WifiReceiver(final WifiManager wifi, Callback successCallback, Callback errorCallback) {
				super();
				this.successCallback = successCallback;
				this.errorCallback = errorCallback;
				this.wifi = wifi;
 			}

			// This method call when number of wifi connections changed
      public void onReceive(Context c, Intent intent) {
				// LocalBroadcastManager.getInstance(c).unregisterReceiver(this);
				c.unregisterReceiver(this);
				// getReactApplicationContext().getCurrentActivity().registerReceiver
				try {
					List < ScanResult > results = this.wifi.getScanResults();
					JSONArray wifiArray = new JSONArray();

					for (ScanResult result: results) {
						JSONObject wifiObject = new JSONObject();
						if(!result.SSID.equals("")){
							try {
		            wifiObject.put("SSID", result.SSID);
		            wifiObject.put("BSSID", result.BSSID);
		            wifiObject.put("capabilities", result.capabilities);
		            wifiObject.put("frequency", result.frequency);
		            wifiObject.put("level", result.level);
		            wifiObject.put("timestamp", result.timestamp);
							} catch (JSONException e) {
		          	this.errorCallback.invoke(e.getMessage());
								return;
							}
							wifiArray.put(wifiObject);
						}
					}
					this.successCallback.invoke(wifiArray.toString());
					return;
				} catch (IllegalViewOperationException e) {
					this.errorCallback.invoke(e.getMessage());
					return;
				}
      }
  }


 @ReactMethod
 public void getWifi(String wifi, Callback result)
 {
     WifiManager wifiMan = (WifiManager) getReactApplicationContext().getSystemService(Context.WIFI_SERVICE);

     WifiInfo wifiInfo = wifiMan.getConnectionInfo();

     wifiName = wifi.toString();
     if (wifiName.length() > 2 && wifiName.charAt(0) == '"'
             && wifiName.charAt(wifiName.length() - 1) == '"') {
         wifiName = wifiName.substring(1, wifiName.length() - 1);
     }

     List<ScanResult> wifiList = wifiMan.getScanResults();
     ArrayList<String> mList = new ArrayList<String>();
     mList.clear();

     for (int i = 0; i < wifiList.size(); i++)
     {
         mList.add((wifiList.get(i).BSSID).toString());

     }


     if (currentBssid == null)
     {
         for (int i = 0; i < wifiList.size(); i++) {
             if ((wifiList.get(i).SSID).toString().equals(wifiName))
             {
                 currentBssid = (wifiList.get(i).BSSID).toString();
                 break;
             }
         }
     }
     else {
         if (currentBssid.equals("00:00:00:00:00:00")
                 || currentBssid.equals("")) {
             for (int i = 0; i < wifiList.size(); i++)
             {
                 if ((wifiList.get(i).SSID).toString().equals(wifiName)) {
                     currentBssid = (wifiList.get(i).BSSID).toString();
                     break;
                 }
             }
         }
     }
     if (currentBssid == null)
     {
         return;
     }

     String tomacaddress[] = currentBssid.split(":");
     int currentLen = currentBssid.split(":").length;

     for (int m = currentLen - 1; m > -1; m--)
     {
         for (int j = mList.size() - 1; j > -1; j--)
         {
             if (!currentBssid.equals(mList.get(j)))
             {
                 String array[] = mList.get(j).split(":");
                 if (!tomacaddress[m].equals(array[m])) {
                     mList.remove(j);//
                 }
             }
         }
         if (mList.size() == 1 || mList.size() == 0) {
             if (m == 5) {
                 sendMac = tomacaddress[m].toString();
             } else if (m == 4) {
                 sendMac = tomacaddress[m].toString()
                         + tomacaddress[m + 1].toString();
             } else {
                 sendMac = tomacaddress[5].toString()
                         + tomacaddress[4].toString()
                         + tomacaddress[3].toString();
             }
             break;
         }
     }


      result.invoke(sendMac);
 }

 @ReactMethod
 public  void  sendSonic(String mac, final String wifi, Callback result)
 {
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

     int count = 5;

     player.play(DataEncoder.encodeMacWiFi(b, wifi.trim()), count, 1000);

     result.invoke(count);
 }

 private static byte uniteBytes(byte src0, byte src1)
 {
     byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 })).byteValue();
     _b0 = (byte) (_b0 << 4);
     byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 })).byteValue();
     byte ret = (byte) (_b0 ^ _b1);
     return ret;
 }

 private static byte[] HexString2Bytes(String src)
 {
     byte[] ret = new byte[src.length() / 2];
     byte[] tmp = src.getBytes();
     for (int i = 0; i < src.length() / 2; i++)
     {
         ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
     }
     return ret;
 }

 private static void printHexString(byte[] b) {
     // System.out.print(hint);
     for (int i = 0; i < b.length; i++)
     {
         String hex = Integer.toHexString(b[i] & 0xFF);
         if (hex.length() == 1) {
             hex = '0' + hex;
         }
         System.out.print("aaa" + hex.toUpperCase() + " ");
     }
     System.out.println("");
 }
}
