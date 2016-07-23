package com.wpmac.addisplay.avtivity;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.VideoView;

import com.wpmac.addisplay.R;
import com.wpmac.addisplay.constant.Constants;
import com.wpmac.addisplay.usb.USBDriver;
import com.wpmac.addisplay.utils.FTPUtils;
import com.wpmac.addisplay.utils.FileUtils;

import org.xutils.DbManager;
import org.xutils.db.sqlite.SqlInfo;
import org.xutils.db.table.DbModel;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DisplayActivity extends Activity {

    private VideoView mVideoView;
    private TextView dataTv;
    private List<String> videoList = new ArrayList<String>();
    //需要播放的文件组
//    private File[] mp4Files;
    private List<File> mp4Files = new ArrayList<File>();
    //当前播放的文件
    private File playFile;
    private int count = 0;
    //需要播放的文件数量
    private int displaySize = 0;
    //检查更新
    private Handler mHandler;
    private Handler usbHandler;
    private Handler readusbHandler;

    private String dir, port, name, password, ip;
    private FTPUtils ftpUtils;
    //是否下载完成ftp上的文件
    boolean downlodFlag;
    //是否刷新数据完成
    boolean flag;
    boolean initFlag;
    //一个循环播放完毕
    boolean playendFlag = true;
    boolean isOpenDiverFlag = false;
    //是否写入数据
    boolean writeDataFlag = false;

    protected final Object threadLock = new Object();//加锁

    byte[] writeBuffer = new byte[512];
    byte[] readBuffer = new byte[512];
    int actualNumBytes;


    public Context global_context;
    public boolean READ_ENABLE_340 = false;
//    public String act_string;

//    EditText sendEditText;
//    TextView receiveText;
//    Button sendButton;
//    Button openDeviceButton;

    int baudRate; /* baud rate */ //send to hardware by AOA
    byte stopBit; /* 1:1stop bits, 2:2 stop bits */
    byte dataBit; /* 8:8bit, 7: 7bit 6: 6bit 5: 5bit*/
    byte parity; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
    byte flowControl;

    USBDriver usbDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        initView();
//        dir = getIntent().getStringExtra("dir");
//        ip = getIntent().getStringExtra("ip");
//        name = getIntent().getStringExtra("name");
//        password = getIntent().getStringExtra("password");
//        port = getIntent().getStringExtra("port");
        ip = Constants.ip;
        name = Constants.name;
        password = Constants.password;
        port = Constants.port;
        dir = Constants.dir;
        global_context = getApplicationContext();
        Log.i("setting", ip + "" + name + password + port + dir);
        new Thread(initDataTask).run();
        checkUpdateVideo();
        //从这里开始是usb
        usbDriver = new USBDriver(this, (UsbManager)getSystemService(Context.USB_SERVICE));
        Log.i("usb", "test");

        baudRate =19200 ;
        stopBit = 1;
        dataBit = 8;
        parity = 0;
        flowControl = 0;

        //  打開設備
        UsbDevice usbDevice = usbDriver.enumerateDevice();
        if (usbDevice != null){
            usbDriver.openDevice(usbDevice);
            usbDriver.UartInit();
            usbDriver.SetConfig(baudRate,dataBit,stopBit,parity,flowControl);
            System.out.println("初始化完毕");
        }

        if (!READ_ENABLE_340){
            READ_ENABLE_340 = true;
            QueryThread queryThread = new QueryThread(handler_340);
            queryThread.start();
        }

    }

    // 先發送一個數據
    private class QueryThread extends Thread{

        Handler handler ;

        QueryThread(Handler handler){
            this.handler = handler;
            this.setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            while (READ_ENABLE_340){
                Message message = handler.obtainMessage();
                message.what = 0;

                // 寫數據
                int numBytes = 0;
                int mLen = 0;

                writeBuffer = new byte[] {0x0a, 0x01, 0x0a, 0x0a, 0x0f};
                numBytes = writeBuffer.length;
                try {
                    mLen = usbDriver.writeData(writeBuffer, numBytes);
                    Log.i("下发",String.valueOf(mLen));
                } catch (IOException e) {
//                    Toast.makeText(global_context, "WriteData Error", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

                if (numBytes != mLen){
//                    Toast.makeText(global_context, "length error", Toast.LENGTH_SHORT).show();
                }


                //  讀數據
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (threadLock){
                    if (usbDriver != null){
                        actualNumBytes = usbDriver.readData(readBuffer, 64);
                    }
                    if (actualNumBytes>0){
                        Log.i("Display", "开始刷新");
                        handler.sendMessage(message);
                        actualNumBytes = 0;
                    }
                }

                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void initView() {
        mVideoView = (android.widget.VideoView) findViewById(R.id.video);
        dataTv = (TextView) findViewById(R.id.datatv);

    }

    Runnable initDataTask = new Runnable() {
        @Override
        public void run() {
            initData();
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putBoolean("value", initFlag);
            msg.setData(data);
            ihandler.sendMessage(msg);
        }
    };

    Handler ihandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            flag = data.getBoolean("value");
            if (flag && playendFlag) {
                playFile = mp4Files.get(count);
                playendFlag = false;
                loadView(playFile.getAbsolutePath());
            }
            // TODO
            // UI界面的更新等相关操作
        }
    };


    Runnable networkTask = new Runnable() {

        @Override
        public void run() {
            // TODO
            // 在这里进行 http request.网络请求相关操作
            ftpUtils = FTPUtils.getInstance();
            flag = ftpUtils.initFTPSetting(ip, Integer.valueOf(port), name, password);
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putBoolean("value", flag);
            msg.setData(data);
            chandler.sendMessage(msg);
        }
    };

    Handler chandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            flag = data.getBoolean("value");
            if (flag) {
                new Thread(downloadTask).start();
            }
            // TODO
            // UI界面的更新等相关操作
        }
    };

    Runnable downloadTask = new Runnable() {

        @Override
        public void run() {
            // TODO
            // 在这里进行 http request.网络请求相关操作
            String savePath = getVideoFilePathStr();
                downlodFlag = ftpUtils.downLoadTypeFile(dir, savePath, "mp4");

            Message msg = new Message();
            Bundle data = new Bundle();
            data.putBoolean("value", downlodFlag);
            msg.setData(data);
            dhandler.sendMessage(msg);
        }
    };
    Handler dhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            flag = data.getBoolean("value");
            if (flag) {
                new Thread(initDataTask).run();
            }
        }
    };


    private void checkUpdateVideo() {

//        Intent intent= new Intent(this, UpdateService.class);
//        startService(intent);
        mHandler = new Handler();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                new Thread(networkTask).start();
                mHandler.postDelayed(this, Constants.checkViedoTime);
            }
        });
    }

    private String getVideoFilePathStr() {
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        String pathdir;
        if (!sdCardExist) {
            pathdir = getFilesDir().getAbsolutePath();
        } else {
            pathdir = FileUtils.CacheDir[3];//新建一级主目录
        }
        return pathdir;
    }

    public void loadView(String path) {

        Uri uri = Uri.parse(path);
        final String fileinfo = path + "";
        mVideoView.setVideoURI(uri);
        mVideoView.start();

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {

                mp.start();// 播放
                Log.i("开始播放", "服务器的第" + count + "个文件>>>>>>" + playFile.getName());
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                android.util.Log.i("播放完成", "服务器的第" + count + "个文件>>>>>>" + playFile.getName());
                count++;
                if (count < displaySize) {
                } else {
                    count = 0;
                    playendFlag = true;
                }
                Log.i("", "count:" + count + "dispalysize:" + displaySize);
                playFile = mp4Files.get(count);
                Uri uri = Uri.parse(playFile.getAbsolutePath());
//                Log.i("setOnCompletionListener",playFile.getAbsolutePath());
                mVideoView.setVideoURI(uri);
                mVideoView.start();// 播放

            }
        });

    }


    /*
    从数据库读取应该播放的文件目录
     */
    private void initData() {

        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        String pathdir;
        if (!sdCardExist) {
            pathdir = getFilesDir().getAbsolutePath();//新建一级主目录
        } else {
            pathdir = FileUtils.CacheDir[3];//新建一级主目录
        }
        DbManager db = x.getDb(LoginActivity.daoConfig);
        String sql = "select id,file_Name,file_Size,file_Path from mp4file_info ";
        List<DbModel> dbModelAll = null;
        try {
            dbModelAll = db.findDbModelAll(new SqlInfo(sql));
        } catch (DbException e) {
            e.printStackTrace();
        }
        List<String> pathList = new ArrayList<String>();
        for (DbModel dbmodel : dbModelAll) {
            String id = dbmodel.getString("id");
            String file_Nanme = dbmodel.getString("file_Name");
            String file_path = dbmodel.getString("file_Path");
            long file_Size = dbmodel.getLong("file_Size");
            pathList.add(file_path);
            Log.i("需要播放:", " file_Name<<" + file_Nanme + " file_Size<<" + file_Size + " file_path<<" + file_path);
        }
        mp4Files.clear();
        for (String path : pathList) {
            File file = new File(path);
            mp4Files.add(file);
        }
        DeletediscardedFile();
//        File file = new File(pathdir);
//        mp4Files = file.listFiles(getFileExtensionFilter(".mp4"));
        displaySize = mp4Files.size();
        initFlag = true;

    }


    /*
 *对比本地和本地存储的表（存储了服务器上的文件信息）
 *删除本地无用的文件
 */
    private void DeletediscardedFile() {

        String loaclpath = getVideoFilePathStr();
        File localfiles = new File(loaclpath);
        File[] localfile = localfiles.listFiles();
        boolean flag = false;
        for (int i = 0; i < localfile.length; i++) {
            flag = false;
            for (int j = 0; j < mp4Files.size(); j++) {
                //如果文件名称相同则不删除
                String name1 = localfile[i].getAbsolutePath();
                String name2 = mp4Files.get(j).getAbsolutePath();
                if (name1.equals(name2)) {
                    flag = true;
                } else {
                }
            }
            if (flag == false) {
                File deletefile = new File(localfile[i].getAbsolutePath());
                deletefile.delete();
                Log.i("deletefile:>>>>>>>>>>>", localfile[i].getName());
            }
        }

    }

    final Handler handler_340 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (actualNumBytes != 0x00) {
                //dataTv.setText(new String(readBuffer, 0, actualNumBytes));
                if (actualNumBytes == 5 && readBuffer[0]==0x0a
                        &&readBuffer[1]==0x01 && readBuffer[4]==0x0f){

                    int first = (int)readBuffer[2];
                    int second = (int)readBuffer[3];

                    if (first<0){
                        first = -first + 128;
                    }

                    if(second<0){
                        second = -second +128;
                    }

                    int data = first * 16 + second;

                    dataTv.setText("PM2.5:  "+String.valueOf(data));
                    Log.i("Display", "刷新结束");
                }
            }
        }
    };

    //显示读取的数据的线程
    private class ReadThread extends Thread {
        Handler handler;

        ReadThread(Handler handler) {
            this.handler = handler;
            this.setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            while (READ_ENABLE_340) {
                Message message = handler.obtainMessage();
                message.what = 0;
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (threadLock) {
                    if (usbDriver != null) {
                        actualNumBytes = usbDriver.readData(readBuffer, 64);
                    }
                    if (actualNumBytes > 0) {
                        handler.sendMessage(message);
                        actualNumBytes = 0;
                    }
                }
            }
        }
    }

    Runnable readDataFromUsbDiver = new Runnable() {
        @Override
        public void run() {
            if (READ_ENABLE_340 && writeDataFlag) {
                writeDataFlag = false;

                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (threadLock) {
                    if (usbDriver != null) {
                        actualNumBytes = usbDriver.readData(readBuffer, 64);
                    }
                    if (actualNumBytes > 0) {
//                        handler.sendMessage(message);
                        Message msg = new Message();
                        Bundle data = new Bundle();
                        data.putInt("value", actualNumBytes);
                        msg.setData(data);
                        rhandler.sendMessage(msg);
                        actualNumBytes = 0;
                    }
                }
            }

        }
    };

    Handler rhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (actualNumBytes != 0x00) {
                dataTv.setText(new String(readBuffer, 0, actualNumBytes));
            }
        }
    };


    @Override
    protected void onStop() {
        if (READ_ENABLE_340) {
            READ_ENABLE_340 = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        if (usbDriver != null) {
            if (usbDriver.isConnected()) {
                usbDriver.closeDevice();
            }
            usbDriver = null;
        }
        super.onDestroy();
    }


}

