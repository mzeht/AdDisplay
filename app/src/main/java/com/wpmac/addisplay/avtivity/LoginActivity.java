package com.wpmac.addisplay.avtivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.wpmac.addisplay.utils.FTPUtils;
import com.wpmac.addisplay.R;
import com.wpmac.addisplay.utils.FileUtils;
import com.wpmac.addisplay.utils.Utils;
import com.wpmac.addisplay.constant.Constants;

import org.xutils.DbManager;
import org.xutils.db.table.TableEntity;
import org.xutils.x;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginActivity extends Activity {

    @Bind(R.id.user_name_edittext)
    EditText userNameEdittext;
    @Bind(R.id.user_password_edittext)
    EditText userPasswordEdittext;
    @Bind(R.id.ip_edittext)
    EditText ipEdittext;
    @Bind(R.id.dir_edittext)
    EditText dirEdittext;
    @Bind(R.id.port_edittext)
    EditText portEdittext;
    @Bind(R.id.login_button)
    Button loginButton;

    private ProgressDialog progressDialog;
    private ProgressDialog downloadProgressDialog;

    private String ip, name, port, password, dir;
    private FTPUtils ftpUtils;
    public boolean flag=false;
    public boolean downlodFlag;
    private Handler networkHandler;

    public static DbManager.DaoConfig daoConfig = new DbManager.DaoConfig()
            .setDbName("addisplay.db")
            .setTableCreateListener(new DbManager.TableCreateListener() {
                @Override
                public void onTableCreated(DbManager db, TableEntity<?> table) {
                    Log.i("mzeht", "onTableCreated<<" + table.getName());
                }
            })
//            .setAllowTransaction(true)
//            .setDbDir(new File("/mnt/sdcard/"))
//            .setDbVersion(1)
            .setDbUpgradeListener(new DbManager.DbUpgradeListener() {
                @Override
                public void onUpgrade(DbManager db, int oldVersion, int newVersion) {

                }
            })
            .setDbOpenListener(new DbManager.DbOpenListener() {
                @Override
                public void onDbOpened(DbManager db) {
                    db.getDatabase().enableWriteAheadLogging();
                }
            });
    DbManager db = x.getDb(daoConfig);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
//        initView();
        initData();
        setListener();
        login();

    }

    private void setListener() {

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                login();

            }
        });
    }

    private void login() {

        ip = ipEdittext.getText().toString();
        name = userNameEdittext.getText().toString();
        port = portEdittext.getText().toString();
        password = userPasswordEdittext.getText().toString();
        dir = dirEdittext.getText().toString();
        Constants.ip=ip;
        Constants.name=name;
        Constants.port=port;
        Constants.password=password;
        Constants.dir=dir;

        if (Utils.isValidValueString(ip) && Utils.isValidValueString(name) && Utils.isValidValueString(port) && Utils.isValidValueString(password
        ) && Utils.isValidValueString(dir)) {
            progressDialog = ProgressDialog
                    .show(LoginActivity.this, "", "登录服务器中", true);


                new Thread(networkTask).start();

        } else {
            Toast.makeText(getApplicationContext(), "信息填写不全，请检查", Toast.LENGTH_LONG).show();
        }
    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            flag = data.getBoolean("value");
            progressDialog.dismiss();
            if (flag == true) {
                if(progressDialog!=null&&progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
                Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_LONG).show();
                new Thread(downloadTask).start();
                progressDialog = ProgressDialog
                        .show(LoginActivity.this, "", "下载中", true);
            } else {
                new Thread(networkTask).start();
                Toast.makeText(getApplicationContext(), "连接失败", Toast.LENGTH_LONG).show();
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
            handler.sendMessage(msg);
        }
    };
    Runnable downloadTask = new Runnable() {

        @Override
        public void run() {
            // TODO
            // 在这里进行 http request.网络请求相关操作
            String savePath=getVideoFilePathStr();
            downlodFlag=ftpUtils.downLoadTypeFile(dir,savePath,"mp4");
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putBoolean("value", downlodFlag);
            msg.setData(data);
            downloadHandler.sendMessage(msg);
        }
    };

    Handler downloadHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            downlodFlag = data.getBoolean("value");
            progressDialog.dismiss();
            if (downlodFlag == true) {
                if(progressDialog!=null&&progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
                Toast.makeText(getApplicationContext(), "下载完成", Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
//                intent.putExtra("dir",dir);
//                intent.putExtra("ip",ip);
//                intent.putExtra("name",name);
//                intent.putExtra("password",password);
//                intent.putExtra("port",port);
                intent.setClass(LoginActivity.this,DisplayActivity.class);
                startActivity(intent);
            } else {
                new Thread(networkTask).start();
//                progressDialog = ProgressDialog
//                        .show(LoginActivity.this, "", "下载中", true);
                Toast.makeText(getApplicationContext(), "下载未完成", Toast.LENGTH_LONG).show();
            }
            // TODO
            // UI界面的更新等相关操作
        }
    };

    private void initData() {
//        ipEdittext.setText("114.215.210.84");
        ipEdittext.setText("120.26.117.32");
//        userNameEdittext.setText("orin_ftp");
        userNameEdittext.setText("cadftp");
//        userPasswordEdittext.setText("orin!@123");
        userPasswordEdittext.setText("cad431");
//        portEdittext.setText("21");
        portEdittext.setText("21");
        dirEdittext.setText("/home/cadftp/workdir/");
//        dirEdittext.setText("/Hesvidia/Media/");

    }

    private String getVideoFilePathStr(){
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        String pathdir;
        if(!sdCardExist){
            pathdir = getFilesDir().getAbsolutePath();
        }else{
            pathdir = FileUtils.CacheDir[3];//新建一级主目录
        }
        return pathdir;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }
}
