package com.wpmac.addisplay.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类
 * 
 */
public class Utils {

	
	public static  final  double EARTH_RADIUS = 6378.137;
	static final String LOG_TAG = "PullToRefresh";
	public static void warnDeprecation(String depreacted, String replacement) {
		
	}
	public static String getUUID(){
		
		return UUID.randomUUID().toString().replace("-", "");
	}
    /**
     * dp转换PX
     * @param res
     * @param dp 
     * @return
     */
	public static int dpToPx(Resources res, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
	}

	/**
	 * 转换long型日期格式
	 * 
	 * @param context
	 * @param date
	 * @return
	 */
	public static String formatDate(Context context, long date) {
		@SuppressWarnings("deprecation")
		int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT
				| DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_CAP_AMPM
				| DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_DATE
				| DateUtils.FORMAT_SHOW_TIME;
		return DateUtils.formatDateTime(context, date, format_flags);
	}

	/**
	 * 转换long型日期格式
	 * 
	 * @param date
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	public static String formatDate(long date) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		return format.format(new Date(date));
	}
	
	

	/**
	 * 转换long型日期格式
	 * 
	 * @param date
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	public static String formatHour(long date) {
		SimpleDateFormat format = new SimpleDateFormat("HH");
		return format.format(new Date(date));
	}


	/**
	 * 获取当前的时间
	 * 
	 * @param context
	 * @return
	 */
	public static String getTime(Context context) {
		return formatDate(context, System.currentTimeMillis());
	}

	/**
	 * 获取当前的时间
	 * 
	 * @return
	 */
	public static String getTime() {
		return formatDate(System.currentTimeMillis());
	}
	
	

	/**
	 * 判断手机号码格式
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isCellphone(String str) {
		Pattern pattern = Pattern.compile("1[34578][0-9]{9}");
		Matcher matcher = pattern.matcher(str);
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * 获取版本号
	 * @param context
	 * @return
	 */
	public static String getAppVersion(Context context)
	
	{
		PackageInfo info=null;
		try {
			info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return info.versionName;

	}
	
	/**
	 * 判断是否是有效值
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isValidValue(String str) {
		boolean b = false;
		if(str==null){
			b= false;
		}else if("".equals(str)){
			b=false;
		}else if("null".equals(str)){
			b=false;
		}else if("NULL".equals(str)){
			b=false;
		}else if("0".equals(str)){
			b = false;
		}else{
			b = true;
		}
		return b;
	}
	
	
	/**
	 * 判断是否是有效值
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isValidValueString(String str) {
		boolean b = false;
		if(str==null){
			b= false;
		}else if("".equals(str)){
			b=false;
		}else if("null".equals(str)){
			b=false;
		}else if("NULL".equals(str)){
			b=false;
		}else{
			b = true;
		}
		return b;
	}
	

	
	public static List<String> arrToList(String[] arr)
	   {  
		List<String> list = new ArrayList<String>();
		for(int i=0;i<arr.length;i++){
			list.add(arr[i]);
		}
	       return  list;  
	   }



	public static String arrToString(String[] arr)
	{
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<arr.length;i++){
			sb.append(arr[i]);
		}
		return  sb.toString();
	}

}
