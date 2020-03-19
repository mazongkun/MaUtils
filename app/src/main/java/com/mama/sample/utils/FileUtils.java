package com.mama.sample.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FileUtils {
	private static final String TAG = "FileUtils";
	private static final FileComparator sFileComparator = new FileComparator();
	private static final String IMAGE_JPG = ".jpg";
	public static final String SYSTEM_PHOTO_PATH = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/";

	public static void init(Context context) {
        Log.d(TAG, "mama= SYSTEM_PHOTO_PATH=" + SYSTEM_PHOTO_PATH);
        Log.d(TAG, "mama= externalPath=" + context.getExternalFilesDir(null));
        Log.d(TAG, "mama= picture=" + context.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
//        /storage/emulated/0/Android/data/com.mama.utils/files/Pictures
//        /sdcard/Android/data/com.mama.utils/files/Pictures
    }


	public static String getAssetData(Context context, String path) {
		InputStream stream = null;
		try {
			stream = context.getAssets().open(path);
			int length = stream.available();
			byte[] data = new byte[length];
			stream.read(data);
			return new String(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * file is exist and the file is not Directory
	 * 
	 * @param filePath
	 * @return boolean
	 */
	public static boolean isFileExist(String filePath) {
		File file = new File(filePath);
		return file.exists() && !file.isDirectory();
	}
	
	/**
	 * file is exist and the file is Directory
	 * 
	 * @param dirPath
	 * @return boolean
	 */
	public static boolean isDirExist(String dirPath) {
		File file = new File(dirPath);
		return file.exists() && file.isDirectory();
	}

	public static boolean isPathExist(String path) {
		if (TextUtils.isEmpty(path)) {
			printLog("isPathExist path is null");
			return false;
		}
		return new File(path).exists();
	}

	/**
	 * Create Directory For Path
	 * 
	 * @param path
	 * @return boolean
	 */
	public static boolean createDirectory(String path) {
		if (TextUtils.isEmpty(path)) {
			printLog("createDirectory path is empty");
			return false;
		}
		File dirFile = new File(path);
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return dirFile.mkdirs();
		}
		return true;
	}

	/**
	 * assert_path resources copy into Local SD card
	 * 
	 * @param assertPathDir
	 * @param dirPath
	 */
	public static void copyFilesToLocalIfNeed(Context context, String assertPathDir, String dirPath) {
		File pictureDir = new File(dirPath);
		if (!pictureDir.exists() || !pictureDir.isDirectory()) {
			pictureDir.mkdirs();
		}
		try {
			String[] fileNames = context.getAssets().list(assertPathDir);
			if (fileNames.length == 0)
				return;
			for (int i = 0; i < fileNames.length; i++) {
				File file = new File(dirPath + File.separator + fileNames[i]);
				if (file.exists() && file.isFile()) {
					if (compareFile(context, dirPath + File.separator + fileNames[i],
							assertPathDir + File.separator + fileNames[i])) {
						printLog("-->copyAssertDirToLocalIfNeed " + file.getName() + " exists");
						continue;
					}
				}
				InputStream is = context.getAssets().open(assertPathDir + File.separator + fileNames[i]);
				int size = is.available();
				byte[] buffer = new byte[size];
				is.read(buffer);
				is.close();
				String mypath = dirPath + File.separator + fileNames[i];
				FileOutputStream fop = new FileOutputStream(mypath);
				fop.write(buffer);
				fop.flush();
				fop.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Used to copy assert file into Local SD card
	 * 
	 * @param context
	 * @param assetsPath
	 * @param strOutFileName
	 * @param isCover
	 * @throws IOException
	 */
	public static void copyAssetsFileToLocalIfNeed(Context context, String assetsPath, String strOutFileName,
                                                   boolean isCover) throws IOException {
		File file = new File(strOutFileName);
		if (file.exists() && file.isFile() && !isCover) {
			printLog("copyAssertFileToLocalIfNeed " + file.getName() + " exists");
			return;
		}
		InputStream myInput;
		OutputStream myOutput = new FileOutputStream(strOutFileName);
		myInput = context.getAssets().open(assetsPath);
		byte[] buffer = new byte[1024];
		int length = myInput.read(buffer);
		while (length > 0) {
			myOutput.write(buffer, 0, length);
			length = myInput.read(buffer);
		}
		myOutput.flush();
		myInput.close();
		myOutput.close();
	}

	/**
	 * 获取指定文件大小
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static long getFileSize(String path) {
		File f = new File(path);
		return getFileSize(f);
	}

	/**
	 * 获取指定文件大小
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static long getFileSize(File file) {
		long size = 0;
		if (!file.exists() || file.isDirectory()) {
			printLog("getFileSize file is not exists or isDirectory !");
			return 0;
		}
		if (file.exists()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				size = fis.available();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return size;
	}

	/**
	 * 获取Asset目录下某个文件的大小，非目录
	 * 
	 * @param context
	 * @param path
	 * @return
	 */
	public static long getAssertFileSize(Context context, String path) {
		if (context == null || path == null || "".equals(path)) {
			printLog("getAssertFileSize context is null or path is null !");
			return 0;
		}
		printLog("getAssertFileSize path:" + path);
		AssetManager assetManager = context.getAssets();
		int size = 0;
		try {
			InputStream inStream = assetManager.open(path);
			size = inStream.available();
			inStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return size;
	}

	/**
	 * 比较sd卡文件与asset目录下的文件大小是否一致
	 * 
	 * @param context
	 * @param filePath
	 * @param assetPath
	 * @return
	 */
	public static boolean compareFile(Context context, String filePath, String assetPath) {
		boolean isSameFile = false;
		File file = new File(filePath);
		if (!file.exists() || file.isDirectory()) {
			isSameFile = false;
		}
		if (getFileSize(file) == getAssertFileSize(context, assetPath)) {
			isSameFile = true;
		}
		return isSameFile;
	}

	@SuppressLint("SimpleDateFormat")
	public static String genSystemCameraPhotoPath() {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		checkDirPath(SYSTEM_PHOTO_PATH);
		return SYSTEM_PHOTO_PATH + "IMAGE_" + timeStamp + ".jpg";
	}

	public static void checkDirPath(String path) {
		if (!isDirExist(path)) {
			createDirectory(path);
		}
	}

	/**
	 * Delete file by File
	 * 
	 * @param filePath
	 */
	public static void deleteFile(String filePath) {
		if (TextUtils.isEmpty(filePath)) {
			printLog("deleteFile path is null");
			return;
		}
		File file = new File(filePath);
		deleteFile(file);
	}
	public static void deleteDirFile(File dir) {
		if (dir == null || !dir.exists() || !dir.isDirectory())
			return;
		for (File file : dir.listFiles()) {
			if (file.isFile())
				file.delete();
			else if (file.isDirectory())
				deleteDirFile(file);
		}
		dir.delete();
	}
	public static void deleteAndMakeDir(String path) {
		File outDir = new File(path);
		deleteDirFile(outDir);
		if(!outDir.exists() || !outDir.isDirectory()) {
			outDir.mkdirs();
		}
	}
	/**
	 * Delete file by File
	 * 
	 * @param file
	 */
	public static void deleteFile(File file) {
		if (null == file) {
			printLog("deleteFile file is null");
			return;
		}

		if (!file.exists() || !file.isFile()) {
			printLog("deleteFile file is not exists or file is dir!");
			return;
		}

		file.delete();
	}

	/**
	 * Used to clear file of directory
	 * 
	 * @param path
	 * @param isDeleteThisDir
	 */
	public static void clearDir(String path, boolean isDeleteThisDir) {
		if (TextUtils.isEmpty(path)) {
			printLog("clearDir path is null");
			return;
		}
		File file = new File(path);
		clearDir(file, isDeleteThisDir);
	}

	public static boolean checkSDCardAvailable() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	/**
	 * Delete the dir in SD card
	 * 
	 * @param dirFile
	 * @param isDeleteThisDir
	 * 
	 */
	public static void clearDir(File dirFile, boolean isDeleteThisDir) {
		if (!checkSDCardAvailable() || dirFile == null) {
			printLog("clearDir dirFile is null");
			return;
		}
		try {
			if (dirFile.isDirectory()) {
				File files[] = dirFile.listFiles();
				for (int i = 0; i < files.length; i++) {
					clearDir(files[i], true);
				}
			}
			if (isDeleteThisDir) {
				if (!dirFile.isDirectory()) {
					dirFile.delete();
				} else {
					if (dirFile.listFiles().length == 0) {
						dirFile.delete();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Used to get System Camera Photo Path
	 * 
	 * @return String
	 */
	@SuppressLint("SimpleDateFormat")
	public static String getSystemCameraPhotoPath() {
		printLog("genSystemCameraPhotoPath SYSTEM_PHOTO_PATH ---> " + SYSTEM_PHOTO_PATH);
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		createDirectory(SYSTEM_PHOTO_PATH);
		return SYSTEM_PHOTO_PATH + "IMAGE_" + timeStamp + IMAGE_JPG;
	}

	/**
	 * Used to get System Camera Photo Path
	 * 
	 * @return String
	 */
	@SuppressLint("SimpleDateFormat")
	public static String getBitmapSavePath() {
		printLog("genSystemCameraPhotoPath SYSTEM_PHOTO_PATH ---> " + SYSTEM_PHOTO_PATH);
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		return timeStamp + IMAGE_JPG;
	}

	/**
	 * Used to get the file name if file is exist
	 * 
	 * @param filePath
	 * @return String
	 */
	public static String getFileName(String filePath) {
		File file = new File(filePath);
		if (file.exists()) {
			return file.getName();
		}
		return null;
	}

	/**
	 * Used to get the files of direction and put into list<String>
	 * 
	 * @param dirPath
	 * @return List<String>
	 */
	public static List<String> loadChildFiles(String dirPath) {
		if (TextUtils.isEmpty(dirPath)) {
			printLog("loadImages---> path is null");
			return null;
		}
		List<String> fileList = new ArrayList<String>();
		File file = new File(dirPath);
		if (!file.exists()) {
			return fileList;
		}
		File[] files = file.listFiles();
		List<File> allFiles = new ArrayList<File>();
		for (File img : files) {
			allFiles.add(img);
		}
		Collections.sort(allFiles, sFileComparator);
		for (File img : allFiles) {
			fileList.add(img.getAbsolutePath());
		}
		return fileList;
	}

	/**
	 * Put the file into byte[]
	 * 
	 * @param filePath
	 * @return byte[]
	 */
	public static byte[] readFile(String filePath) {
		if (TextUtils.isEmpty(filePath) && isPathExist(filePath)) {
			printLog("readFile path is null");
			return null;
		}
		try {
			FileInputStream fis = new FileInputStream(filePath);
			int length = fis.available();
			byte[] buffer = new byte[length];
			fis.read(buffer);
			fis.close();
			return buffer;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

    public static byte[] readRawFile(Context context, int resourceId) throws IOException {
        if( null == context || resourceId < 0 ){
            return null;
        }

        String result = null;
        byte[] buffer = null;

        InputStream inputStream = context.getResources().openRawResource( resourceId );
        // 获取文件的字节数
        int length = inputStream.available();
        if (length <= 0) {
            return null;
        }

        // 创建byte数组
        buffer = new byte[length];
        // 将文件中的数据读到byte数组中
        int len = inputStream.read(buffer);
        return buffer;
    }

	/**
	 * Save the file to savePath if file is exist
	 * 
	 * @param savePath
	 * @param data
	 */
	public static void saveFile(String savePath, byte[] data) {
		if (data == null || data.length == 0) {
			printLog("saveFile data is null");
			return;
		}
		try {
			File file = new File(savePath);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static class FileComparator implements Comparator<File> {
		public int compare(File file1, File file2) {
			if (file1.lastModified() < file2.lastModified()) {
				return -1;
			} else {
				return 1;
			}
		}
	}

	/**
	 * Print log
	 * 
	 * @param logStr
	 */
	public static void printLog(String logStr) {
		Log.d(TAG, logStr);
	}

	public static byte[] getFromAssets(Context context, String fileName) {
		byte[] buffer = null;
		try {
			InputStream in = context.getResources().getAssets().open(fileName);
			// 获取文件的字节数
			int lenght = in.available();
			// 创建byte数组
			buffer = new byte[lenght];
			// 将文件中的数据读到byte数组中
			in.read(buffer);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public static byte[] getFromSdcard(Context context, String path) {
		byte[] buffer = null;
		try {
			FileInputStream in = new FileInputStream(path);
			// 获取文件的字节数
			int lenght = in.available();
			// 创建byte数组
			buffer = new byte[lenght];
			// 将文件中的数据读到byte数组中
			in.read(buffer);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public static String getPath(Context context, Uri uri) {
		String[] pojo = { MediaStore.Images.Media.DATA };
		ContentResolver cr = context.getContentResolver();
		Cursor c = null;
		if (uri.getScheme().equals("content")) {// 判断uri地址是以什么开头的
			c = cr.query(uri, pojo, null, null, null);
		} else {
			c = cr.query(getFileUri(context, uri), null, null, null, null);// 红色字体判断地址如果以file开头
		}
		c.moveToFirst();
		// 这是获取的图片保存在sdcard中的位置
		int colunm_index = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		String srcPath = c.getString(colunm_index);
		return srcPath;
	}

	public static Uri getFileUri(Context context, Uri uri) {
		if (uri.getScheme().equals("file")) {
			String path = uri.getEncodedPath();
			Log.d(TAG, "path1 is " + path);
			if (path != null) {
				path = Uri.decode(path);
				Log.d(TAG, "path2 is " + path);
				ContentResolver cr = context.getContentResolver();
				StringBuffer buff = new StringBuffer();
				buff.append("(").append(MediaStore.Images.ImageColumns.DATA).append("=").append("'" + path + "'")
						.append(")");
				Cursor cur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						new String[] { MediaStore.Images.ImageColumns._ID }, buff.toString(), null, null);
				int index = 0;
				for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
					index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
					index = cur.getInt(index);
				}
				if (index == 0) {
					// do nothing
				} else {
					Uri uri_temp = Uri.parse("content://media/external/images/media/" + index);
					Log.d(TAG, "uri_temp is " + uri_temp);
					if (uri_temp != null) {
						uri = uri_temp;
					}
				}
			}
		}
		return uri;
	}

	public static String getLatestPhotoPath(Context context) {
		String sdcardPath = Environment.getExternalStorageDirectory().toString();

		ContentResolver mContentResolver = context.getContentResolver();
		Cursor mCursor = mContentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA },
				MediaStore.Images.Media.MIME_TYPE + "=? OR " + MediaStore.Images.Media.MIME_TYPE + "=?",
				new String[] { "image/jpeg", "image/png" }, MediaStore.Images.Media._ID + " DESC");
		String photoPath = null;

		while (mCursor.moveToNext()) {
			String path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
			if (path.startsWith(sdcardPath + "/DCIM/100MEDIA") || path.startsWith(sdcardPath + "/DCIM/Camera/")
					|| path.startsWith(sdcardPath + "DCIM/100Andro")) {
				photoPath = path;
				break;
			}
		}
		mCursor.close();
		return photoPath;
	}
	public static String getLatestPhotoPathFromDir(String dir) {
		File dirFile = new File(dir);
		if (dirFile.isDirectory()) {
			File[] files = dirFile.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File file, String s) {
					return s.endsWith(".jpg");
				}
			});
			if (files != null && files.length > 0) {
				return files[files.length-1].getAbsolutePath();
			}
		}
		return null;
	}
	public static List<String> getFilesPathFromDir(String dir) {
		List<String> list = new ArrayList<String>();
		File dirFile = new File(dir);
		if (dirFile.isDirectory()) {
			File[] files = dirFile.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File file, String s) {
					return s.endsWith(".jpg");
				}
			});
			
			for (int i = files.length-1; i >= 0; i--) {
				list.add(files[i].getAbsolutePath());
			}
		}
		return list;
	}
}