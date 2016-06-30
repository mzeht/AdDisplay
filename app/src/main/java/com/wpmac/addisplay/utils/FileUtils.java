package com.wpmac.addisplay.utils;

import android.os.Environment;

import java.io.File;

/**
 * 文件管理器
 * @author Administrator
 *
 */
public class FileUtils {
	
	
	
	/**
	 * 缓存目录
	 */
	public static final String[] CacheDir = new String[] {"/sdcard/AdDisplay/PDF/"        //PDF
		                                               	  , "/sdcard/AdDisplay/LOG/"      // 保存日志
		                                               	  ,"/sdcard/AdDisplay/CacheImage/"      //缓存目录
		                                               	  ,"/sdcard/AdDisplay/VIDEO/"  //视频目录
		                                               	  ,"/sdcard/AdDisplay/CRASH/" //bug目录
		                                                   };  
	
	
	
	static{
		initialize();
	}
	
	/**
	 * 获取PDF保存目录
	 */
	public static String getPDFIconDirectory()
	{
		return CacheDir[0];
	}

	/**
	 * 获取日志保存目录
	 */
	public static String getLogDirectory()
	{
		return CacheDir[1];
	}
	
	/**
	 * 获取图片缓存目录
	 * @return
	 */
	public static String getCacheImageDirectory()
	{
		return CacheDir[2];
	}
	
   /**
    * 获取视频目录
    * @return
    */
	public static String getCacheVideoDir()
	{
		return CacheDir[3];
	}
	
	
	
	/**
	 * 获取重命名文件
	 * @return
	 */
	public static String getRenameDirectory()
	{
		return CacheDir[4];
	}

	
	
	public static String getCrashDir()
	{
		return CacheDir[4];
	}
	
	
	/**
	 * 初始化目录
	 */
	public static void initialize() {
		if (hasSDCard()) {
			for (String cacheDir : CacheDir) {
				createWorkDir(cacheDir);
			}
		}
	}
	
	
	/**
	 * 判断是否挂载SD 卡
	 * @return
	 */
	public static boolean hasSDCard() {
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			return false;
		} 
		return true;
	}
	

	/**
	 * 创建工作目录
	 * 
	 * @param filePath
	 */
	public static void createWorkDir(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			createWorkDir(file.getParent());
			file.mkdir();
		}
	}



	

}
