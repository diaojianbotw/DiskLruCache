package com.example.disklrucache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import libcore.io.DiskLruCache;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity implements OnClickListener{

	private Button huancun;
	private Button duqu;
	private ImageView image;
	private DiskLruCache mDiskLruCache = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		huancun = (Button) findViewById(R.id.huancun);
		duqu = (Button) findViewById(R.id.duqu);
		huancun.setOnClickListener(this);
		duqu.setOnClickListener(this);
		image =  (ImageView) findViewById(R.id.image);
		File cacheFile = getDishPath(this,"bitmap");
		if(!cacheFile.exists())
		{
			cacheFile.mkdirs();
		}
		try {
			//open()方法接收四个参数，第一个参数指定的是数据的缓存地址，
			//第二个参数指定当前应用程序的版本号，第三个参数指定同一个key可以对应多少个缓存文件，基本都是传1，
			//第四个参数指定最多可以缓存多少字节的数据
			mDiskLruCache = DiskLruCache.open(cacheFile, getVersion(this), 1, 10*1024*1024);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		return super.onOptionsItemSelected(item);
	}
	
	//获取缓存地址
	public File getDishPath(Context context,String name)
	{
		String cachePath;
		//判断SD卡是否存在，并且是否具有读写权限
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageDirectory()) ||
				!Environment.isExternalStorageRemovable() || !Environment.isExternalStorageRemovable())
		{
			cachePath = context.getExternalCacheDir().getPath();
			//获取路径
			//  /sdcard/Android/data/<application package>/cache 
		}
		else
		{
			cachePath = context.getCacheDir().getPath();
			//获取路径
			// /data/data/<application package>/cache
		}
		return new File(cachePath+File.separator+name);
	}
	
	//获取版本号
	public int getVersion(Context context)
	{
		PackageInfo info;
		try {
			info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 1;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
		{
			case R.id.huancun:
				new Thread(new Runnable() {
					@Override
					public void run() {
						String imageUrl = "http://img.my.csdn.net/uploads/201309/01/1378037235_7476.jpg";
						String key = hashKeyForDisk(imageUrl);
						try {
							DiskLruCache.Editor editor = mDiskLruCache.edit(key);
							if(editor!=null)
							{
								OutputStream outstream = editor.newOutputStream(0);
								if(downLoadUrltoStream(imageUrl,outstream))
								{
									editor.commit();
								}
								else
								{
									editor.abort();
								}
							}
							mDiskLruCache.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();
				break;
			case R.id.duqu:
				String imageUrl = "http://img.my.csdn.net/uploads/201309/01/1378037235_7476.jpg";
				String key = hashKeyForDisk(imageUrl);
			try {
				DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
				if(snapshot!=null)
				{
					InputStream is = snapshot.getInputStream(0);
					Bitmap bitm = BitmapFactory.decodeStream(is);
					image.setImageBitmap(bitm);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
	}
	
	//下载
	private boolean downLoadUrltoStream(String urlString,OutputStream outstream)
	{
		HttpURLConnection httpconnection=null;
		BufferedOutputStream out = null;
		BufferedInputStream  in=null;
		try {
			URL url = new URL(urlString);
			httpconnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(httpconnection.getInputStream(),8*1024);
			out = new BufferedOutputStream(outstream,8*1024);
			int b;
			while((b=in.read())!=-1)
			{
				out.write(b);
			}
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if(httpconnection!=null)
			{
				httpconnection.disconnect();
			}
			
				try {
					if(out!=null)
					{
					    out.close();
					}
					if(in != null)
					{
						in.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			
		}
		return false;
	}
	//获取md5编码
	public String hashKeyForDisk(String key) {  
	    String cacheKey;  
	    try {  
	        final MessageDigest mDigest = MessageDigest.getInstance("MD5");  
	        mDigest.update(key.getBytes());  
	        cacheKey = bytesToHexString(mDigest.digest());  
	    } catch (NoSuchAlgorithmException e) {  
	        cacheKey = String.valueOf(key.hashCode());  
	    }  
	    return cacheKey;  
	}  
	  
	private String bytesToHexString(byte[] bytes) {  
	    StringBuilder sb = new StringBuilder();  
	    for (int i = 0; i < bytes.length; i++) {  
	        String hex = Integer.toHexString(0xFF & bytes[i]);  
	        if (hex.length() == 1) {  
	            sb.append('0');  
	        }  
	        sb.append(hex);  
	    }  
	    return sb.toString();  
	}  
}
