title: Synchronize files from ftpserver
categories: 项目
tags:
  - Android
  - FTP
  - 线程
  - 同步文件
date: 2016-06-30 21:10:57
---
#项目功能
1.	软件为运行于安卓平台的客户端软件。
2.	客户端软件运行于一台19寸屏的安卓媒体机上。
3.	该客户端软件有播放广告视频的功能，广告视频从FTP服务器下载，本地的视频文件每隔三分钟与FTP服务器同步一次，保持本地视频和服务器视频的同步。
4.	FTP服务器的下载目录要求可配置，第一次运行该软件时弹出配置窗口，要求输入服务器IP地址，用户名和密码，以及服务器视频所在的目录路径。配置完成后，以后运行不再需要重新配置。
5.	媒体机主板通过串口外接了一块STM32单片机，STM32单片机会将采集到的PM2.5指数，温湿度信息上传，媒体板通过串口接收，并将数据显示在屏幕的最上方。通信协议见“媒体板和单片机通信协议.docx”文档。
6. 开机播放一段欢迎视频，视频打包在Android程序内部

#功能分析
![](http://7xqtsx.com1.z0.glb.clouddn.com/16-6-30/89255535.jpg)

工程部分结构图

LaunchActivity:播放欢迎视频

LoginActivity：登录FTP服务器并且下载所有的视频文件

DisplayActivity:播放服务器上最新的文件，并且每个三分钟检查视频的更新和废除，若有更新，则下载下来，不在服务器目录的文件删除掉，否则存储空间占用不断增加

>* 由于设备的网络问题和服务器等造成下载中断，不完整等，必须要有重试下载和检查下载完整的机制
>* 每播放完一组视频，若有更新，刷新播放队列，若无更新，循环播放该组视频
>* 服务器上的文件信息要实时保存到数据库，以供对比
>* 需要下载服务器指定目录下的一类文件

#依赖包
```
 compile fileTree(include: ['*.jar'], dir: 'libs')
    compile 'com.android.support:recyclerview-v7:23.4.0'
    compile 'com.android.support:leanback-v17:23.4.0'
//    compile files('libs/commons-net-3.0.1.jar')
    compile 'com.jakewharton:butterknife:7.0.0'
    compile 'org.xutils:xutils:3.3.26'
    compile files('libs/commons-net-3.3.jar')
   

```

##v17 Leanback Library
https://developer.android.com/topic/libraries/support-library/features.html#v17-leanback
内置了对TV设备的支持

##org.xutils:xutils:3.3.26
开源框架 这里主要是用到数据库模块

##commons-net-3.3.jar
apache出品的与FTP服务器交互的jar包


#技术实现
##数据库的实时更新
创建:在LoginActivity

```
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

```

我们需要一个NewFile


```
@Table(name="mp4file_info")
public class NewFile {
    @Column(name="id",isId = true,autoGen = true,property = "")
    private int id;
    @Column(name = "file_Name")
    private String fileName;
    @Column(name = "file_Size")
    private long fileSize;
    @Column(name = "file_Path")
    private String filePath;

    public NewFile(String fileName, long fileSize, String filePath) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.filePath = filePath;
    }

    public NewFile() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "NewFile{" +
                "fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +'\'' +
                ", filePath=" + filePath +
                '}';
    }
}

```

在下载工具类中：

```
 NewFile newFile = new NewFile(file.getName(), file.getSize(), LocalSavePath + fileName);
                            newFiles.add(newFile);
                        }
                    }
                }
            }
            //退出登陆FTP，关闭ftpCLient的连接
            ftpClient.logout();
            ftpClient.disconnect();


            DbManager db = x.getDb(LoginActivity.daoConfig);
            db.delete(NewFile.class);
            db.save(newFiles);
            Log.i("数据库保存实时服务器文件信息", newFiles.toString());

```

##视频的循环播放
```
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
    

```
主要是对mVideoView的两个监听

##从数据库刷新播放列表
```
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
            Log.i("需要播放:", " file_Nanme<<" + file_Nanme + " file_Size<<" + file_Size + " file_path<<" + file_path);
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

```

##删除无用的文件
```
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

```

#Thread和Hanler实现循环线程
在LoginActivity中有network和download两个线程，一个连接，一个下载，连接失败则会重试连接，成功则开启下载，下载成功则跳转展示页面，失败则重新连接

在DisplayActivity中有三个线程，network，download，initdata，多出一个刷新播放队列的线程

都是通过Therad和Hanler实现，循环线程，确保下载成功

```
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

```



－－－－－－2016.07.24-－－－－－
#待改进
1.下载过程中不提供进度条，若要支持进度条，需要以流的方式下载文件，增加进度计算和回调反馈

2.下载不支持断点续传，若要支持，要设置断点参数，引用ftp下载相关方法