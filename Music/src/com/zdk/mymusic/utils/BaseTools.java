package com.zdk.mymusic.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class BaseTools {
	public static String getAppVersion(Context context) throws Exception {
		PackageManager packageManager = context.getPackageManager();
		PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(),0);
		String versionName = packInfo.versionName;
		return versionName;
	}
	
	public static int getSystemVersion(){
		int version= android.os.Build.VERSION.SDK_INT;
		return version;
	}
}
