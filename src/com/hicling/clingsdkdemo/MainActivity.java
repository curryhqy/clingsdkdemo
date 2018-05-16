package com.hicling.clingsdkdemo;

import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.hicling.clingsdk.ClingSdk;
import com.hicling.clingsdk.bleservice.BluetoothDeviceInfo;
import com.hicling.clingsdk.devicemodel.PERIPHERAL_DEVICE_INFO_CONTEXT;
import com.hicling.clingsdk.devicemodel.PERIPHERAL_USER_REMINDER_CONTEXT;
import com.hicling.clingsdk.devicemodel.PERIPHERAL_WEATHER_DATA;
import com.hicling.clingsdk.devicemodel.PERIPHERAL_WEATHER_TYPE;
import com.hicling.clingsdk.listener.OnBleListener;
import com.hicling.clingsdk.listener.OnNetworkListener;
import com.hicling.clingsdk.listener.OnSdkReadyListener;
import com.hicling.clingsdk.model.DayTotalDataModel;
import com.hicling.clingsdk.model.DeviceConfiguration;
import com.hicling.clingsdk.model.DeviceNotification;
import com.hicling.clingsdk.model.MinuteData;
import com.hicling.clingsdk.model.UserProfile;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Created by Curry on 18/5/9.
 */

public class MainActivity extends FragmentActivity{
    private final static String TAG = MainActivity.class.getSimpleName();

    static boolean mbClingSdkReady = false;

    private static MainActivity instance;

    private static PERIPHERAL_DEVICE_INFO_CONTEXT mDeviceInfo = null;

    private static MainActivity.FirmwareUpgradeFragment mUpgradeFragment = null;
    private static double mdUpgradingProgress;

    private final static int msg_Upgrading_Progresss = 1000;
    private static Handler mMsgHandler = new Handler () {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

//			Log.i(TAG, "got handler msg: " + msg.what);
            switch (msg.what) {
                case msg_Upgrading_Progresss:
                    if ( mUpgradeFragment != null ) {
                        mUpgradeFragment.showFirmwareUpgradingProgress(mdUpgradingProgress);
                    }

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new MainActivity.PlaceholderFragment()).commit();
        }

        /* 显示App icon左侧的back键 */
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // this is test appkey and appsecret  HCa3f4b08b799e28af   7978e5e9477d07dd5d7dc79fd1bc00d7
        // user should request your own key on our web page ( http://developers.hicling.com )
        ClingSdk.init(this, "HCa3f4b08b799e28af", "7978e5e9477d07dd5d7dc79fd1bc00d7", new OnSdkReadyListener() {
            @Override
            public void onClingSdkReady() {
                Log.i(TAG, "onClingSdkReady()");
                mbClingSdkReady = true;
            }

            @Override
            public void onFailed(String s) {
                mbClingSdkReady = false;
                Log.i(TAG, "onClingSdkReady onFailed() :" + s);
            }
        });
        ClingSdk.setBleDataListener(mBleDataListener);
        ClingSdk.setDeviceConnectListener(mDeviceConnectedListener);
        ClingSdk.enableDebugMode(true);
        ClingSdk.start(this);
    }

    @Override
    protected void onDestroy () {
        ClingSdk.stop(this);
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onResume () {
        super.onResume();
        ClingSdk.onResume(this);
        Log.i(TAG, "onResume()");
    }

    @Override
    protected void onPause () {
        Log.i(TAG, "onPause()");
//        ClingSdk.onPause(this);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id==android.R.id.home){
            this.finish();//返回按钮
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container,
                    false);

            final EditText editTextUsername = (EditText) rootView.findViewById ( R.id.signinup_username );
            final EditText editTextPassword1 = (EditText) rootView.findViewById ( R.id.signinup_password1 );
            final EditText editTextPassword2 = (EditText) rootView.findViewById ( R.id.signinup_password2 );

            editTextUsername.setText("1812584700@qq.com");
            editTextPassword1.setText("heqinyang");

            Button btnSignIn = (Button) rootView.findViewById ( R.id.signin_btn );
            btnSignIn.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        if ( editTextUsername.getText().toString().length() > 0 && editTextPassword1.getText().toString().length() > 0) {
                            Log.i(TAG, "sign in: " + editTextUsername.getText().toString() + ", " + editTextPassword1.getText().toString());
                            ClingSdk.signIn(editTextUsername.getText().toString(), editTextPassword1.getText().toString(), new OnNetworkListener() {
                                @Override
                                public void onSucceeded(Object o, Object o1) {
                                    Log.i(TAG, "sign in successful");
                                    instance.showApiTestPage ();
                                }

                                @Override
                                public void onFailed(int i, String s) {
                                    Log.i(TAG, "onLoginFailed :" + i + ", " + s);
                                }
                            });
                        }
                    } else {
                        Log.i(TAG, "Cling sdk not ready, please try again");
                    }
                }
            });

            Button btnSignUp = (Button) rootView.findViewById ( R.id.signup_btn );
            btnSignUp.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        String password1 = editTextPassword1.getText().toString();
                        String password2 = editTextPassword2.getText().toString();
                        if ( editTextUsername.getText().toString().length() > 0 && password1.length() > 0 && 0 == password1.compareTo(password2)) {
                            ClingSdk.signUp(editTextUsername.getText().toString(), password1, null, new OnNetworkListener() {

                                @Override
                                public void onSucceeded(Object o, Object o1) {
                                    Log.i(TAG, "signUp successful");
                                    instance.showApiTestPage ();
                                }

                                @Override
                                public void onFailed(int i, String s) {
                                    Log.i(TAG, "signUp failed:" + i + ", " + s);

                                }
                            });
                        }
                    } else {
                        Log.i(TAG, "Cling sdk not ready, please try again");
                    }
                }
            });

            return rootView;
        }
    }


    private static ListView mListViewScanResult;
    private static View mScanView = null;
    public static class ScanFragment extends Fragment {
        public ScanFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mScanView = inflater.inflate(R.layout.fragment_scan, container,
                    false);

            Button btnDereg = (Button) mScanView.findViewById(R.id.deregister_btn);
            btnDereg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    if ( mbClingSdkReady) {
                        ClingSdk.deregisterDevice(new OnBleListener.OnDeregisterDeviceListener(){

                            @Override
                            public void onDeregisterDeviceSucceed() {
                                Log.i(TAG, "onDeregisterDeviceSucceed()");
                                showToast ( "Deregister Device Succeed" );
                                instance.updateScanText();
                            }

                            @Override
                            public void onDeregisterDeviceFailed(int i, String s) {
                                Log.i(TAG, "onDeregisterDeviceFailed(): " + i + ", " + s);
                            }
                        });
                    }
                }
            });

            mListViewScanResult = (ListView) mScanView.findViewById(R.id.scan_result_list);
            mListViewScanResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    ListAdapter adapter = mListViewScanResult.getAdapter();
                    Object obj = adapter.getItem(position);
                    if ( obj != null ) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mapItem = (Map<String, Object>) obj;
                        String devname = (String) mapItem.get("DEVNAME");
                        if ( mArrayListScanResult != null ) {
                            for ( BluetoothDeviceInfo bleinfo : mArrayListScanResult ) {
                                if ( bleinfo.getmBleDevice().getName().equals(devname)) {
                                    ClingSdk.stopScan();
                                    ClingSdk.registerDevice(bleinfo.getmBleDevice(), new OnBleListener.OnRegisterDeviceListener() {
                                        @Override
                                        public void onRegisterDeviceSucceed() {
                                            Log.i(TAG, "onRegisterDeviceSucceed()");
//                                            if(mbDeviceConnected) {
//                                                showDownloadPage();
//                                            }
                                            instance.updateScanText();
                                        }

                                        @Override
                                        public void onRegisterDeviceFailed(int i, String s) {
                                            Log.i(TAG, "onRegisterDeviceFailed() :"  + i + ", " + s);
                                        }
                                    });
                                    break;
                                }
                            }
                        }


                    }
                }
            });

            Button btnStartScan = (Button) mScanView.findViewById ( R.id.scan_start_btn );
            instance.updateScanText();
            btnStartScan.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( ! ClingSdk.isAccountBondWithCling() ) {
                        ClingSdk.setClingDeviceType ( ClingSdk.CLING_DEVICE_TYPE_ALL );
//						ClingSdk.setClingDeviceType ( ClingSdk.CLING_DEVICE_TYPE_BAND_1 );
                    }

                    if ( mbClingSdkReady ) {
                        ClingSdk.stopScan();
                        ClingSdk.startScan(10 * 60 * 1000, new OnBleListener.OnScanDeviceListener() { // 10 minutes
                            @Override
                            public void onBleScanUpdated(Object o) {
                                //蓝牙连接成功后，不会再扫描
                                Log.i(TAG, "onBleScanUpdated()");
                                if(o != null) {
                                    ArrayList<BluetoothDeviceInfo> arrayList = (ArrayList<BluetoothDeviceInfo>) o;
                                    instance.updateScanResultView(arrayList);
                                }
                            }
                        });

                        if ( mbDeviceConnected ) {
                            showDownloadPage ();
                        }
                    } else {
                        Log.i(TAG, "Cling sdk not ready, please try again");
                    }
                }
            });

            Button btnStopScan = (Button) mScanView.findViewById ( R.id.scan_stop_btn );
            btnStopScan.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    if ( mbClingSdkReady ) {
                        ClingSdk.stopScan();
                    } else {
                        Log.i(TAG, "Cling sdk not ready, please try again");
                    }
                }
            });

            return mScanView;
        }

    }

    private void updateScanText() {
        runOnUiThread(
                new Runnable() {
            @Override
            public void run() {
                if(mScanView != null) {
                    Button btnStartScan = (Button) mScanView.findViewById(R.id.scan_start_btn);
                    if (ClingSdk.isAccountBondWithCling()) {
                        String clingId = ClingSdk.getBondClingDeviceName();
                        int clingType = ClingSdk.getClingDeviceType();
                        String strPrefix = "U";
                        switch (clingType) {
                            case ClingSdk.CLING_DEVICE_TYPE_WATCH_1:
                                strPrefix = "W";
                                break;
                            case ClingSdk.CLING_DEVICE_TYPE_BAND_1:
                                strPrefix = "B1";
                                break;
                            case ClingSdk.CLING_DEVICE_TYPE_BAND_2:
                                strPrefix = "B2";
                                break;
                            case ClingSdk.CLING_DEVICE_TYPE_BAND_3:
                                strPrefix = "B3";
                                break;
                            case ClingSdk.CLING_DEVICE_TYPE_BAND_PACE:
                                strPrefix = "PA";
                                break;
                            case ClingSdk.CLING_DEVICE_TYPE_WATCH_GOGPS:
                                strPrefix = "GO";
                                break;
                        }

                        btnStartScan.setText(strPrefix + " " + clingId);//设置按钮名称为手环编号
                    } else {
                        btnStartScan.setText("Start Scan");
                    }
                }
            }
        });

    }

    private void showApiTestPage () {
        runOnUiThread ( new Runnable () {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new MainActivity.ScanFragment()).commit();
            }
        });
    }

    private static ArrayList<BluetoothDeviceInfo> mArrayListScanResult;
    private void updateScanResultView (final ArrayList<BluetoothDeviceInfo> arrlistDevices) {
        runOnUiThread ( new Runnable () {
            @Override
            public void run() {
                if ( mListViewScanResult!= null && arrlistDevices != null ) {
                    mArrayListScanResult = arrlistDevices;
                    ArrayList<Map<String, Object>> contents = new ArrayList<Map<String, Object>> ();
                    for ( BluetoothDeviceInfo bleinfo : arrlistDevices ) {
                        Log.i(TAG, String.format("device: %s, rssi:%d", bleinfo.getmBleDevice().getName(), bleinfo.getmRssi()));
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("DEVNAME", bleinfo.getmBleDevice().getName());
                        map.put("RSSI", String.valueOf(bleinfo.getmRssi())+"db");
                        contents.add(map);
                    }
                    SimpleAdapter adapter = new SimpleAdapter (MainActivity.this, contents,  R.layout.list_scan_item, new String []{"DEVNAME", "RSSI"}, new int []{R.id.listitem_device_name, R.id.listitem_rssi});
                    mListViewScanResult.setAdapter(adapter);
                }
            }
        });
    }

    private static void showDownloadPage () {
        instance.getFragmentManager().beginTransaction()
                .replace(R.id.container, new MainActivity.MinuteDataFragment()).addToBackStack(null).commit();
    }



    private static ListView mListViewData;
    public static class MinuteDataFragment extends Fragment {
        public MinuteDataFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_data, container,
                    false);

            mListViewData = (ListView) rootView.findViewById(R.id.data_result_list);

            Button btnGetProf = (Button) rootView.findViewById ( R.id.get_user_profile );
            btnGetProf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        ClingSdk.requestUserProfile(new OnNetworkListener() {
                            @Override
                            public void onSucceeded(Object o, Object o1) {
                                if(o != null) {
                                    UserProfile up = (UserProfile) o;
                                    Log.i(TAG, "onGetUserProfileSucceeded \n" + up.toString());

                                    showToast("onGetUserProfileSucceeded \n用户信息为：\n" + up.toString());
                                }
                            }

                            @Override
                            public void onFailed(int i, String s) {
                                Log.i(TAG, "onGetUserProfileFailed " + i + ", " + s);
                                showToast("获取用户信息失败！");
                            }
                        });
                    }
                }
            });


            Button btnEditProf = (Button) rootView.findViewById ( R.id.edit_user_profile );
            btnEditProf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        UserProfile userProfile = new UserProfile();//更新用户信息
                        userProfile.mMemberNickName = "何沁洋";
                        userProfile.mnRunLength = 105;
                        userProfile.mMemberHight = 172;
                        userProfile.mMemberWeight=120;
                        userProfile.mBirthday="1998-08-26";
                        ClingSdk.updateUserProfile(userProfile, new OnNetworkListener() {
                            @Override
                            public void onSucceeded(Object o, Object o1) {
                                Log.i(TAG, "onSetUserProfileSucceeded" );
                                showToast("更改用户信息成功，请重新查看！");
                            }

                            @Override
                            public void onFailed(int i, String s) {
                                Log.i(TAG, "onSetUserProfileFailed " + i + ", " + s );
                            }
                        });
                    }
                }
            });

            Button btnConfig = (Button) rootView.findViewById ( R.id.config_device_btn );
            btnConfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        showConfigPage ();
                    }
                }
            });

            Button btnDownload = (Button) rootView.findViewById ( R.id.request_data_btn );
            btnDownload.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        long endTime = System.currentTimeMillis()/1000;
                        long startTime = endTime - 10 * 3600;
                        //从服务器获取分钟数据
                        ClingSdk.requestMinuteData(startTime, endTime, new OnNetworkListener() {
                            @Override
                            public void onSucceeded(Object o, Object o1) {
                                Log.i(TAG, "onRequestMinuteDataReady()");
                                if ( o != null ) {
                                    ArrayList<MinuteData> arrayList = (ArrayList<MinuteData>) o;
                                    for ( MinuteData md : arrayList ) {
                                        Log.i(TAG, md.toString() );
                                        instance.updateMinuteDataView (arrayList);
                                    }
                                }
                            }

                            @Override
                            public void onFailed(int i, String s) {
                                Log.i(TAG, "onRequestMinuteDataFailed(): " + i + ", " + s);
                            }
                        });
                    }
                }
            });

            Button btnGetLocal = (Button) rootView.findViewById ( R.id.get_local_data_btn );
            btnGetLocal.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    long endTime = System.currentTimeMillis() / 1000;
                    long startTime = endTime - 60 * 60;
                    ArrayList<MinuteData> arrlist = ClingSdk.getMinuteData(startTime, endTime);

                    Log.i(TAG, "get minute data size: " + arrlist.size()+"\n");
                    Log.i(TAG,"get minute data size:\n"+arrlist);
                    showToast("本地数据库分钟数据为：\n"+arrlist);
                }
            });

            Button btnUpgrade = (Button) rootView.findViewById ( R.id.firmware_upgrade_btn );
            btnUpgrade.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "clicked upgrade button");
                    showUpgradePage ();
                }
            });

            Button btnTotalData = (Button) rootView.findViewById(R.id.get_dataTotal_btn);
            btnTotalData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    instance.getDayTotal();
                }
            });

            return rootView;
        }
    };

    private void updateMinuteDataView ( final ArrayList<MinuteData> arrlistData) {
        runOnUiThread ( new Runnable () {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                if ( mListViewData != null && arrlistData != null ) {
                    ArrayList<Map<String, Object>> contents = new ArrayList<Map<String, Object>> ();
                    for ( MinuteData minData : arrlistData ) {
//						Log.i(TAG, String.format("device: %s, rssi:%d", bleinfo.getmBleDevice().getName(), bleinfo.getmRssi()));
                        Map<String, Object> map = new HashMap<String, Object>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
                        String strDate = sdf.format ( new Date( minData.minuteTimeStamp * 1000 ) );
                        map.put("DATE", strDate);
                        map.put("VALUE", minData.toString());
                        contents.add(map);
                    }
                    SimpleAdapter adapter = new SimpleAdapter (MainActivity.this, contents,  R.layout.list_data_item, new String []{"DATE", "VALUE"}, new int []{R.id.listitem_timestamp, R.id.listitem_minute_data});
                    mListViewData.setAdapter(adapter);
                }
            }

        });
    }

    private static void showConfigPage () {
        instance.getFragmentManager().beginTransaction()
                .replace(R.id.container, new MainActivity.ConfigurationFragment()).addToBackStack(null).commit();
    }

    public static class ConfigurationFragment extends Fragment {
        public ConfigurationFragment () {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_config, container,
                    false);

            Button btnConfigDev = (Button) rootView.findViewById ( R.id.device_configuration_btn );
            btnConfigDev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        DeviceConfiguration devCfg = new DeviceConfiguration ();
                        devCfg.bActFlipWristEn = 1;
                        devCfg.bActHoldEn = 1;
                        devCfg.bNavShakeWrist = 1;
                        ClingSdk.setPerpheralConfiguration(devCfg);
                        showToast("config done, see result on Cling device");
                    }
                }
            });

            Button btnSmartNoti = (Button) rootView.findViewById ( R.id.Smart_notification_btn );
            btnSmartNoti.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        DeviceNotification devNotification = new DeviceNotification ();
                        devNotification.incomingcall = 1;
                        devNotification.missedcall = 1;
                        devNotification.social = 1;
                        ClingSdk.setPeripheralNotification(devNotification);
                        showToast("notification done");
                    }
                }
            });

            Button btnReminder = (Button) rootView.findViewById ( R.id.Reminder_btn );
            btnReminder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        PERIPHERAL_USER_REMINDER_CONTEXT reminder = new PERIPHERAL_USER_REMINDER_CONTEXT ();
                        reminder.hour = 10;
                        reminder.minute = 20;
                        reminder.name = "meeting";
                        ClingSdk.addPerpheralReminderInfo(reminder);
                        showToast("reminder done, see result on Cling device");
                    }
                }
            });

            Button btnWeather = (Button) rootView.findViewById ( R.id.Weather_btn );
            btnWeather.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        ArrayList<PERIPHERAL_WEATHER_DATA> arrlistWeather = new ArrayList<PERIPHERAL_WEATHER_DATA> ();
                        Calendar calendar = Calendar.getInstance();
                        int day = calendar.get(Calendar.DAY_OF_MONTH);
                        int month = calendar.get(Calendar.MONTH);
                        PERIPHERAL_WEATHER_DATA weather = new PERIPHERAL_WEATHER_DATA ();
                        weather.day = day;
                        weather.month = month + 1;
                        weather.temperature_high = 89;
                        weather.temperature_low = 36;
                        weather.type = PERIPHERAL_WEATHER_TYPE.PERIPHERAL_WEATHER_SNOWY;
                        arrlistWeather.add(weather);
                        // more weather forecast...
                        // weather = new PERIPHERAL_WEATHER_DATA ();...
                        // ...
                        // arrlistWeather.add(weather);
                        ClingSdk.setPeripheralWeatherInfo(arrlistWeather);
                        showToast_SHORT("weather done, see result on Cling device");
                        Log.i(TAG,arrlistWeather.toString());
                        showToast("日期，天气为："+arrlistWeather.toString());
                    }
                }
            });

            Button btnLanguage = (Button) rootView.findViewById ( R.id.Language_btn);
            btnLanguage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {

                        ClingSdk.setPeripheralLanguage(ClingSdk.CLING_DEVICE_LANGUAGE_TYPE_ZH_CN);
                        showToast_SHORT("手环语言已经改为中文！");
                    }
                }
            });

            Button btnSosMsg = (Button) rootView.findViewById ( R.id.sosMsg_btn);
            if(ClingSdk.getClingDeviceType() == ClingSdk.CLING_DEVICE_TYPE_BAND_1  //UV
                    || ClingSdk.getClingDeviceType() == ClingSdk.CLING_DEVICE_TYPE_BAND_2   //Nfc
                    || ClingSdk.getClingDeviceType() == ClingSdk.CLING_DEVICE_TYPE_BAND_3) {  //voc
                btnSosMsg.setVisibility(View.VISIBLE);
                if(mbRevSos && mbClingSdkReady) {
                    btnSosMsg.setText("Receive SOS MSG");
                }
            } else {
                btnSosMsg.setVisibility(View.GONE);
            }
            return rootView;
        }
    }

    private static void showUpgradePage () {
        Log.i(TAG, "show upgrade page");
        mUpgradeFragment = new MainActivity.FirmwareUpgradeFragment();
        instance.getFragmentManager().beginTransaction()
                .replace(R.id.container, mUpgradeFragment).addToBackStack(null).commit();
    }

    public static class FirmwareUpgradeFragment extends Fragment {
        private TextView txtvTitle;
        private ProgressBar pbar;
        private TextView txtvProgress;

        public FirmwareUpgradeFragment () {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_upgrade, container,
                    false);

            Button btnRequestFirmware = (Button) rootView.findViewById(R.id.request_firmware_btn);
            btnRequestFirmware.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        ClingSdk.requestFirmwareUpgradeInfo(new OnNetworkListener() {
                            @Override
                            public void onSucceeded(Object o, Object o1) {
                                Log.i(TAG, "onFirmwareInfoReceived "+ o.toString() + ", " + o1.toString());
                            }

                            @Override
                            public void onFailed(int i, String s) {
                                Log.i(TAG, "onFirmwareInfoFailed: "+ i + ", " + s);
                            }
                        });
                    }
                }
            });

            Button btnUpgrade = (Button) rootView.findViewById ( R.id.firmware_upgrading_btn);
            btnUpgrade.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mbClingSdkReady ) {
                        // note this configuration does not work on Cling Watch devices.
                        // Now only Cling Band devices support language configuration.
                        if ( mDeviceInfo != null ) {
                            ClingSdk.upgradeFirmware(mDeviceInfo.softwareVersion, new OnBleListener.OnUpgradeFirmwareListener() {
                                @Override
                                public void onFirmwareSpaceNotEnough() {
                                    Log.i(TAG, "onFirmwareSpaceNotEnough" );
                                }

                                @Override
                                public void onFirmwareDownloadFailed(int i, String s) {
                                    Log.i(TAG, "onFirmwareDownloadFailed: " + i + ", " + s);
                                }

                                @Override
                                public void onFirmwareDownloadProgress(Object o) {
		                        	Log.i(TAG, "onFirmwareDownloadProgress " + o.toString());
                                }

                                @Override
                                public void onFirmwareDownloadSucceeded() {
                                    Log.i(TAG, "onFirmwareDownloadSucceeded ");
                                }

                                @Override
                                public void onFirmwareUpgradeProgress(Object o) {
			                        Log.i(TAG, "onFirmwareUpgradeProgress " + o );
                                    if(o != null) {
                                        mdUpgradingProgress = Double.parseDouble(o.toString());
                                        mMsgHandler.sendMessage(mMsgHandler.obtainMessage(msg_Upgrading_Progresss));
                                    }
                                }

                                @Override
                                public void onFirmwareUpgradeSucceeded() {
                                    Log.i(TAG, "onFirmwareUpgradeSucceeded" );
                                }

                                @Override
                                public void onFirmwareUpgradeFailed(int i, String s) {
                                    Log.i(TAG, "onFirmwareUpgradeFailed: " + i + " , " + s );
                                    showToast ( "upgrading failed " + s );
                                }
                            });
                        }
                    }
                }
            });

            txtvProgress = (TextView) rootView.findViewById(R.id.firmware_progress_txt);
            txtvTitle = (TextView) rootView.findViewById (R.id.firmware_indicator_txt);
            pbar = (ProgressBar) rootView.findViewById(R.id.firmware_progress_bar);
            pbar.setMax(100);

            return rootView;
        }

        public void showDownloadProgress ( long progress ) {
            txtvTitle.setText("downloading");
            txtvProgress.setText("" + progress);
        }

        public void showFirmwareUpgradingProgress ( double progress ) {
            txtvTitle.setText("Upgrading");
            txtvProgress.setText(String.format(Locale.US, "%.1f%%", progress * 100 ));
            pbar.setProgress((int) (progress * 100) );
        }
    }
    /*
       这个showToast持续时间为3.5秒
     */
    private static void showToast (final String text) {
        instance.runOnUiThread ( new Runnable () {
            @Override
            public void run() {
                /*
                    Toast.LENGTH_LONG显示3.5秒
                    Toast.LENGTH_SHORT显示2秒
                 */
                Toast.makeText(instance, text, Toast.LENGTH_LONG).show();
            }
        });
    }
    /*
       这个showToast持续时间为2.5秒
     */
    private static void showToast_SHORT(final String text){
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(instance,text,Toast.LENGTH_SHORT).show();
            }
        });

    }

    private static boolean mbDeviceConnected = false;
    private static boolean mbRevSos = false;
    private OnBleListener.OnBleDataListener mBleDataListener = new OnBleListener.OnBleDataListener() {
        @Override
        public void onGotSosMessage() {
            Log.i(TAG, "received sos message");
            showToast ( "received sos message" );
            mbRevSos = true;
        }

        @Override
        public void onDataSyncingProgress(Object o) {
            Log.i(TAG, "onDataSyncingProgress " + o.toString() );
        }

        @Override
        public void onDataSyncedFromDevice() {
            Log.i(TAG, "data synced");
            showToast ( "data synced" );
        }

        @Override
        public void onDataSyncingMinuteData(Object o) {
            if(o != null) {
                Log.i(TAG, "onDataSyncingMinuteData is: " + o.toString());  //MinuteData
            }
        }
    };


    private OnBleListener.OnDeviceConnectedListener mDeviceConnectedListener = new OnBleListener.OnDeviceConnectedListener() {
        @Override
        public void onDeviceConnected() {
            Log.i(TAG, "onDeviceConnected()");
            showToast ( "Device Connected" );
            mbDeviceConnected = true;
            instance.updateScanText();
        }

        @Override
        public void onDeviceDisconnected() {
            Log.i(TAG, "onDeviceDisconnected()");
            showToast("device is disconnected");
            mbDeviceConnected = false;
        }

        @Override
        public void onDeviceInfoReceived(Object o) {
            if(o != null) {
                mDeviceInfo = (PERIPHERAL_DEVICE_INFO_CONTEXT) o;
                Log.i(TAG, "onDeviceInfoReceived: " + mDeviceInfo.softwareVersion);
            }
        }
    };


    private void getDayTotal(){
        long lStarttime = getDayBeginTimeWithTime();
        TreeSet<DayTotalDataModel> setDTDM = ClingSdk.getDayTotalList(lStarttime, lStarttime + 24 * 3600 - 1);
        if((setDTDM!=null)&&(setDTDM.size()==1)) {
            DayTotalDataModel DTDM = setDTDM.first();
            //当天数据
            Log.i(TAG, "getDayTotal DTDM is " + DTDM.toString());
            showToast("从本地获取的当天数据为：\n"+DTDM.toString());
        }
    }


    private long getDayBeginTimeWithTime()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new java.util.Date(System.currentTimeMillis()));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        java.util.Date start = calendar.getTime();

        long begintime = start.getTime() / 1000;

        return begintime;
    }


    private void deleteMindata() {
        long endtime = System.currentTimeMillis() /1000 ;
        long startTime = endtime - 20 * 60;
        ClingSdk.deleteMindataByTimeStamp(startTime, endtime);
    }
}
