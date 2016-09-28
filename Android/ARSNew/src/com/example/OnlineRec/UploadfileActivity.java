package com.example.OnlineRec;


import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import com.example.OnlineRec.R;


public class UploadfileActivity extends Activity
{
    //private String fileName = "360sicheck.txt";  //报文中的文件名参数
    private String path = Environment.getExternalStorageDirectory().getPath();  //Don't use "/sdcard/" here
 //   private String uploadFile = path + "/" + "360sicheck.txt";    //待上传的文件路径
   // private String postUrl = "http://mycloudnote.sinaapp.com/upload.php"; //处理POST请求的页面
    static public String postUrl = "http://192.168.43.215/upload.php"; //处理POST请求的页面
    static public String checkUrl= "http://192.168.43.215/check.php";  
   // private TextView mText1;
   // private TextView mText2;
   // private Button mButton;
    
	ListView listView; 
	TextView textView;
	// 记录当前的父文件夹
	File currentParent;
	// 记录当前路径下的所有文件的文件数组
	File[] currentFiles;
	
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
                String what = msg.getData().getString("result");
                what="结果："+what;
                Toast.makeText(UploadfileActivity.this,what,Toast.LENGTH_LONG).show();
        }
   };    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploadfile);
        //"文件路径：\n"+
   //   mText1 = (TextView) findViewById(R.id.textView2);
   //   mText1.setText(uploadFile);
   //   //"上传网址：\n"+
   //   mText2 = (TextView) findViewById(R.id.textView3);
   //   mText2.setText(postUrl);
   //   /* 设置mButton的onClick事件处理 */
   //   mButton = (Button) findViewById(R.id.button1);
   //   mButton.setOnClickListener(new View.OnClickListener()
   //   {
   //       public void onClick(View v)
   //       {
   //       	UploadFile(uploadFile);
   //       }
   //   });
    	listView = (ListView) findViewById(R.id.list);
		textView = (TextView) findViewById(R.id.path);
		// 获取系统的SD卡的目录
		File sdCardDir = Environment.getExternalStorageDirectory();
		String rootdir=sdCardDir.getPath();
		rootdir=rootdir+"/hucd"+"/";
		File root = new File(rootdir);	
		if (root.exists())
		{
			currentParent = root;
			currentFiles = root.listFiles();
			// 使用当前目录下的全部文件、文件夹来填充ListView
			inflateListView(currentFiles);
		}
		class CheckThread extends Thread {
			String fileName_;
			public CheckThread(String fileName) {
				fileName_=fileName;
			}
			public void run() {
				try {
					Log.i("www", fileName_);
					RecordActivity.PlayAudioFile(fileName_);
					HttpGet httpGet = new HttpGet(checkUrl+"?checkfilename="+fileName_.substring(fileName_.lastIndexOf("/")+1));
			        HttpClient httpClient = new DefaultHttpClient();
					HttpResponse response = httpClient.execute(httpGet);
					InputStream inputStream = response.getEntity().getContent();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(inputStream));
					String result = "";
					String line = "";
					while (null != (line = reader.readLine())) {
						result += line;
					}
					Log.i("www", result);
					Bundle bundle = new Bundle(); 
					bundle.putString("result",result);
					Message msg=new Message();
					msg.setData(bundle);
					handler.sendMessage(msg);
					if(result.length()==0){
						Log.i("www", "upload new file");
						UploadFile(fileName_);
					}
					 
					//Toast.makeText(UploadfileActivity.this, result.trim(), Toast.LENGTH_SHORT).show();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		// 为ListView的列表项的单击事件绑定监听器
		listView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
				int position, long id)
			{
				// 用户单击了文件，播放音频
				if (currentFiles[position].isFile()) {
					new CheckThread(currentFiles[position].getAbsolutePath()).start();
					// Global g=(Global)getApplicationContext();
					// g.PlayAudioFile(currentFiles[position].getAbsolutePath());
					return;
				}
					
				// 获取用户点击的文件夹下的所有文件
				File[] tmp = currentFiles[position].listFiles();
				if (tmp == null || tmp.length == 0){
				
				}else{
					// 获取用户单击的列表项对应的文件夹，设为当前的父文件夹
					currentParent = currentFiles[position]; //②
					// 保存当前的父文件夹内的全部文件和文件夹
					currentFiles = tmp;
					// 再次更新ListView
					inflateListView(currentFiles);
				}
			}
		});
    }
    private void inflateListView(File[] files) //①
	{
		// 创建一个List集合，List集合的元素是Map
		List<Map<String, Object>> listItems = 
			new ArrayList<Map<String, Object>>();
		for (int i = 0; i < files.length; i++)
		{
			Map<String, Object> listItem = 
				new HashMap<String, Object>();
			// 如果当前File是文件夹，使用folder图标；否则使用file图标
			if (files[i].isDirectory())
			{
				listItem.put("icon", R.drawable.folder);
			}
			else
			{
				listItem.put("icon", R.drawable.file);
			}			
			
			listItem.put("fileName", files[i].getName());
			// 添加List项
			listItems.add(listItem);
		}
		// 创建一个SimpleAdapter
		SimpleAdapter simpleAdapter = new SimpleAdapter(this
			, listItems, R.layout.line
			, new String[]{ "icon", "fileName" }
			, new int[]{R.id.icon, R.id.file_name });
		// 为ListView设置Adapter
		listView.setAdapter(simpleAdapter);
		try
		{
			textView.setText("当前路径为：" 
				+ currentParent.getCanonicalPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
    /* 上传文件至Server的方法 */
    static public String UploadFile(String uploadFileName)
    {
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        try
        {
            URL url = new URL(postUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
          /* Output to the connection. Default is false,
             set to true because post method must write something to the connection */
            con.setDoOutput(true);
          /* Read from the connection. Default is true.*/
            con.setDoInput(true);
          /* Post cannot use caches */
            con.setUseCaches(false);
          /* Set the post method. Default is GET*/
            con.setRequestMethod("POST");
          /* 设置请求属性 */
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Charset", "UTF-8");
            con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
          /*设置StrictMode 否则HTTPURLConnection连接失败，因为这是在主进程中进行网络连接*/
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
            DataOutputStream ds=new DataOutputStream(con.getOutputStream());

            //= new DataOutputStream(con.getOutputStream());  //output to the connection
            ds.writeBytes(twoHyphens + boundary + end);
            Log.i("www", uploadFileName.substring(uploadFileName.lastIndexOf("/")+1));
            ds.writeBytes("Content-Disposition: form-data; " +
                    "name=\"file\";filename=\"" +
                    uploadFileName.substring(uploadFileName.lastIndexOf("/")+1) + "\"" + end);
            ds.writeBytes(end);
          /* 取得文件的FileInputStream */
            FileInputStream fStream = new FileInputStream(uploadFileName);
          /* 设置每次写入8192bytes */
            int bufferSize = 8192;
            byte[] buffer = new byte[bufferSize];   //8k
            int length = -1;
          /* 从文件读取数据至缓冲区 */
            while ((length = fStream.read(buffer)) != -1)
            {
            /* 将资料写入DataOutputStream中 */
                ds.write(buffer, 0, length);
            }
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
          /* 关闭流，写入的东西自动生成Http正文*/
            fStream.close();
          /* 关闭DataOutputStream */
            ds.close();
          /* 从返回的输入流读取响应信息 */
            InputStream is = con.getInputStream();  //input from the connection 正式建立HTTP连接
            int ch;
            StringBuffer b = new StringBuffer();
           
         // while ((ch = is.read()) != -1)
         // {
         //    // b.append((char) ch);
         //     b.append(ch);
         // }
            byte[] bytes = new byte[1024];
            for(int n ; (n = is.read(bytes))!=-1 ; ){
                b.append(new String(bytes,0,n));
            }
          /* 显示网页响应内容 */
            Log.i("www", b.toString().trim()+"hucd");
            return b.toString();
            //Toast.makeText(UploadfileActivity.this, b.toString().trim(), Toast.LENGTH_SHORT).show();//Post成功
        } catch (Exception e)
        {
        	Log.i("www", "fail"+e.getMessage());
            /* 显示异常信息 */
           // Toast.makeText(UploadfileActivity.this, "Fail:" + e, Toast.LENGTH_SHORT).show();//Post失败
        }
        return "";
    }
}