package com.wpmac.addisplay.utils;

import android.util.Log;

import com.wpmac.addisplay.avtivity.LoginActivity;
import com.wpmac.addisplay.daomain.NewFile;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.xutils.DbManager;
import org.xutils.db.sqlite.SqlInfo;
import org.xutils.db.table.DbModel;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p/>
 * 用于Android和FTP服务器进行交互的工具类
 */
public class FTPUtils {
    private FTPClient ftpClient = null;
    private static FTPUtils ftpUtilsInstance = null;
    private String FTPUrl;
    private int FTPPort;
    private String UserName;
    private String UserPassword;
    private boolean error = false;
    private boolean downLoadComplete = false;

    private FTPUtils() {
        ftpClient = new FTPClient();
    }

    /*
     * 得到类对象实例（因为只能有一个这样的类对象，所以用单例模式）
     */
    public static FTPUtils getInstance() {
        if (ftpUtilsInstance == null) {
            ftpUtilsInstance = new FTPUtils();
        }
        return ftpUtilsInstance;
    }

    /**
     * 设置FTP服务器
     *
     * @param FTPUrl       FTP服务器ip地址
     * @param FTPPort      FTP服务器端口号
     * @param UserName     登陆FTP服务器的账号
     * @param UserPassword 登陆FTP服务器的密码
     * @return
     */
    public boolean initFTPSetting(String FTPUrl, int FTPPort, String UserName, String UserPassword) {
        this.FTPUrl = FTPUrl;
        this.FTPPort = FTPPort;
        this.UserName = UserName;
        this.UserPassword = UserPassword;

        int reply;

        try {
            //1.要连接的FTP服务器Url,Port
            ftpClient.connect(FTPUrl, FTPPort);

            //2.登陆FTP服务器
            ftpClient.login(UserName, UserPassword);

            //3.看返回的值是不是230，如果是，表示登陆成功
            reply = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                //断开
                ftpClient.disconnect();
                return false;
            }
            ftpClient.setBufferSize(1024);
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
            ftpClient.setFileTransferMode(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
            ftpClient.setDataTimeout(60000 * 3);      //设置传输超时时间为60秒
//            ftpClient.setConnectTimeout(60000 * 3);       //连接超时为60秒

            return true;

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 上传文件
     *
     * @param FilePath 要上传文件所在SDCard的路径
     * @param FileName 要上传的文件的文件名(如：Sim唯一标识码)
     * @return true为成功，false为失败
     */
    public boolean uploadFile(String FilePath, String FileName) {

        if (!ftpClient.isConnected()) {
            if (!initFTPSetting(FTPUrl, FTPPort, UserName, UserPassword)) {
                return false;
            }
        }

        try {

            //设置存储路径
//            ftpClient.makeDirectory("/data");
//            ftpClient.changeWorkingDirectory("/data");

            //设置上传文件需要的一些基本信息

//            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.ASCII_FILE_TYPE);

            //文件上传吧～
            FileInputStream fileInputStream = new FileInputStream(FilePath);
            ftpClient.storeFile(FileName, fileInputStream);

            //关闭文件流
            fileInputStream.close();

            //退出登陆FTP，关闭ftpCLient的连接
            ftpClient.logout();
            ftpClient.disconnect();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 下载文件
     *
     * @param FilePath 要存放的文件的路径
     * @param FileName 远程FTP服务器上的那个文件的名字
     * @return true为成功，false为失败
     */
    public boolean downLoadFile(String FilePath, String FileName) {

        if (!ftpClient.isConnected()) {
            if (!initFTPSetting(FTPUrl, FTPPort, UserName, UserPassword)) {
                return false;
            }
        }

        try {
            // 转到指定下载目录
//            ftpClient.changeWorkingDirectory("/data");

            // 列出该目录下所有文件
            FTPFile[] files = ftpClient.listFiles();

            // 遍历所有文件，找到指定的文件
            for (FTPFile file : files) {
                if (file.getName().equals(FileName)) {
                    //根据绝对路径初始化文件
                    File localFile = new File(FilePath);

                    // 输出流
                    OutputStream outputStream = new FileOutputStream(localFile);

                    // 下载文件
                    ftpClient.retrieveFile(file.getName(), outputStream);

                    //关闭流
                    outputStream.close();
                }
            }


            //退出登陆FTP，关闭ftpCLient的连接
            ftpClient.logout();
            ftpClient.disconnect();


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 下载一类文件
     *
     * @param LocalSavePath 要存放的文件的本地路径
     * @param FileType      远程FTP服务器上的需要下载的文件的类型（后缀）.mp4和MP4
     * @return true为成功，false为失败
     */
    public boolean downLoadTypeFile(String DownloadPath, String LocalSavePath, String FileType) {


        if (!ftpClient.isConnected()) {
            if (!initFTPSetting(FTPUrl, FTPPort, UserName, UserPassword)) {
                return false;
            }
        }

        try {
            // 转到指定下载目录
            ftpClient.changeWorkingDirectory(DownloadPath);
            ftpClient.setBufferSize(1024);
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.enterLocalPassiveMode();
//            ftpClient.enterLocalActiveMode();
            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
            ftpClient.setFileTransferMode(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
//            ftpClient.setDataTimeout(60000 * 3);       //设置传输超时时间为60秒
//            ftpClient.setConnectTimeout(60000 * 3);       //连接超时为60秒
            // 列出该目录下所有文件
            FTPFile[] files = ftpClient.listFiles();
            if (files.length == 0) {
                return false;
            }
            //筛选出mp4文件
//            FTPFile[] mp4files=new FTPFile[];
//            int count=0;
//            for(FTPFile file:files){
//                String fileName=file.getName();
//                String prefix=fileName.substring(fileName.lastIndexOf(".")+1);
//
//                if (prefix.equals(FileType)) {
//                    mp4files[count]=file;
//                    count++;
//                }
//            }
//            if(mp4files.length==0){
//                return false;
//            }
            int mp4FileNumber = 0;
//            int downloadFileNumer = 0;
            ArrayList<NewFile> newFiles = new ArrayList<NewFile>();
            // 遍历所有文件，找到指定的文件
            for (FTPFile file : files) {
                if (!ftpClient.isConnected()) {
                    initFTPSetting(FTPUrl, FTPPort, UserName, UserPassword);
                }


                String fileName = file.getName();
                String prefix = fileName.substring(fileName.lastIndexOf(".") + 1);
                long localSize = -1;
//
                if (prefix.equals("mp4") || prefix.equals("MP4")) {
                    //根据绝对路径初始化文件
                    mp4FileNumber++;
                    long serverSize = file.getSize();
                    File localFile = new File(LocalSavePath + file.getName());

                    if (localFile.exists()) {
                        localSize = localFile.length();
                        if (localSize != serverSize) {
                            Log.i("删除不完整文件>>>>>>>>>>",localFile.getName());
                            localFile.delete();
                            File saveFile = new File(LocalSavePath + file.getName());
                            OutputStream outputStream = new FileOutputStream(saveFile, true);
                            // 下载文件
//                        if(localSize>0){
//                            ftpClient.setRestartOffset(localSize);
//                        }
                            ftpClient.retrieveFile(file.getName(), outputStream);
//                        InputStream input = ftpClient.retrieveFileStream(DownloadPath + file.getName());
//                        byte[] b = new byte[1024];
//                        int length = 0;
//                        while ((length = input.read(b)) != -1) {
//                            outputStream.write(b, 0, length);
//                            currentSize = currentSize + length;
////                        if (currentSize / step != process) {
////                            process = currentSize / step;
////                            if (process % 5 == 0) {  //每隔%5的进度返回一次
//////                                listener.onDownLoadProgress(MainActivity.FTP_DOWN_LOADING, process, null);
////                            }
////                        }
//                        }
//                        //关闭流
//                        input.close();
                            outputStream.close();
                            // 主动调用一次getReply()把接下来的226消费掉. 这样做是可以解决这个返回null问题
//                        if (ftpClient.completePendingCommand()) {
//
//                        } else {
//                            downLoadComplete = false;
//                        }
                            //检查下载的文件是否完整
                            Log.i("检查下载的文件是否完整", "服务器文件:>>>>>" + file.getName()+":"+file.getSize() + ",下载后的文件大小" + localFile.length());
                            if (file.getSize() != saveFile.length()) {
                                downLoadComplete = false;
                            }else {
                                NewFile newFile = new NewFile(file.getName(), file.getSize(), LocalSavePath + fileName);
                                newFiles.add(newFile);

                            }


                        } else if (localSize == serverSize) {
                            Log.i("", "此文件已存在");
//                            downloadFileNumer++;
                            NewFile newFile = new NewFile(file.getName(), file.getSize(), LocalSavePath + fileName);
                            newFiles.add(newFile);
                        }
                    } else {
                        // 进度
//                    long step = serverSize / 100;
//                    long process = 0;
//                        long currentSize = 0;
                        // 开始准备下载文件
                        // 输出流
                        OutputStream outputStream = new FileOutputStream(localFile, true);

                        // 下载文件
//                        if(localSize>0){
//
//                            ftpClient.setRestartOffset(localSize);
//                        }
                        ftpClient.retrieveFile(file.getName(), outputStream);
//                        InputStream input = ftpClient.retrieveFileStream(DownloadPath + file.getName());
//                        byte[] b = new byte[1024];
//                        int length = 0;
//                        while ((length = input.read(b)) != -1) {
//                            outputStream.write(b, 0, length);
//                            currentSize = currentSize + length;
////                        if (currentSize / step != process) {
////                            process = currentSize / step;
////                            if (process % 5 == 0) {  //每隔%5的进度返回一次
//////                                listener.onDownLoadProgress(MainActivity.FTP_DOWN_LOADING, process, null);
////                            }
////                        }
//                        }
//                        //关闭流
//                        input.close();
                        outputStream.close();
                        // 主动调用一次getReply()把接下来的226消费掉. 这样做是可以解决这个返回null问题
//                        if (ftpClient.completePendingCommand()) {
//
//                        } else {
//                            downLoadComplete = false;
//                        }
                        //检查下载的文件是否完整
                        Log.i("检查下载的文件是否完整", "服务器文件:>>>>>" + file.getName() + file.getSize() + "下载后的文件大小" + localFile.length());
                        if (file.getSize() != localFile.length()) {
                            downLoadComplete = false;
                        } else {
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
//            DbManager db2 = x.getDb(LoginActivity.daoConfig);
            //下载的文件个数等于服务器上的文件个数
            String sql = "select * from mp4file_info";
            List<DbModel> dbModelAll = null;
            try {
                dbModelAll = db.findDbModelAll(new SqlInfo(sql));
            } catch (DbException e) {
                e.printStackTrace();
            }

            if (mp4FileNumber == dbModelAll.size()) {
                downLoadComplete = true;
                Log.i("检查下载的文件个数", "服务器文件个数:>>>>>" + mp4FileNumber + "已同步的文件个数" + dbModelAll.size());
            }
            return downLoadComplete;


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException ioe) {
                    // do nothing
                    error = true;
                    return downLoadComplete;
                }
            } else {
                return downLoadComplete;
            }


        }

    }

    private void downLoad() {
    }

}
