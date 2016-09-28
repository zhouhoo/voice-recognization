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
    //private String fileName = "360sicheck.txt";  //�����е��ļ�������
    private String path = Environment.getExternalStorageDirectory().getPath();  //Don't use "/sdcard/" here
 //   private String uploadFile = path + "/" + "360sicheck.txt";    //���ϴ����ļ�·��
   // private String postUrl = "http://mycloudnote.sinaapp.com/upload.php"; //����POST�����ҳ��
    static public String postUrl = "http://192.168.43.215/upload.php"; //����POST�����ҳ��
    static public String checkUrl= "http://192.168.43.215/check.php";  
   // private TextView mText1;
   // private TextView mText2;
   // private Button mButton;
    
	ListView listView; 
	TextView textView;
	// ��¼��ǰ�ĸ��ļ���
	File currentParent;
	// ��¼��ǰ·���µ������ļ����ļ�����
	File[] currentFiles;
	
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
                String what = msg.getData().getString("result");
                what="�����"+what;
                Toast.makeText(UploadfileActivity.this,what,Toast.LENGTH_LONG).show();
        }
   };    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploadfile);
        //"�ļ�·����\n"+
   //   mText1 = (TextView) findViewById(R.id.textView2);
   //   mText1.setText(uploadFile);
   //   //"�ϴ���ַ��\n"+
   //   mText2 = (TextView) findViewById(R.id.textView3);
   //   mText2.setText(postUrl);
   //   /* ����mButton��onClick�¼����� */
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
		// ��ȡϵͳ��SD����Ŀ¼
		File sdCardDir = Environment.getExternalStorageDirectory();
		String rootdir=sdCardDir.getPath();
		rootdir=rootdir+"/hucd"+"/";
		File root = new File(rootdir);	
		if (root.exists())
		{
			currentParent = root;
			currentFiles = root.listFiles();
			// ʹ�õ�ǰĿ¼�µ�ȫ���ļ����ļ��������ListView
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
		// ΪListView���б���ĵ����¼��󶨼�����
		listView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
				int position, long id)
			{
				// �û��������ļ���������Ƶ
				if (currentFiles[position].isFile()) {
					new CheckThread(currentFiles[position].getAbsolutePath()).start();
					// Global g=(Global)getApplicationContext();
					// g.PlayAudioFile(currentFiles[position].getAbsolutePath());
					return;
				}
					
				// ��ȡ�û�������ļ����µ������ļ�
				File[] tmp = currentFiles[position].listFiles();
				if (tmp == null || tmp.length == 0){
				
				}else{
					// ��ȡ�û��������б����Ӧ���ļ��У���Ϊ��ǰ�ĸ��ļ���
					currentParent = currentFiles[position]; //��
					// ���浱ǰ�ĸ��ļ����ڵ�ȫ���ļ����ļ���
					currentFiles = tmp;
					// �ٴθ���ListView
					inflateListView(currentFiles);
				}
			}
		});
    }
    private void inflateListView(File[] files) //��
	{
		// ����һ��List���ϣ�List���ϵ�Ԫ����Map
		List<Map<String, Object>> listItems = 
			new ArrayList<Map<String, Object>>();
		for (int i = 0; i < files.length; i++)
		{
			Map<String, Object> listItem = 
				new HashMap<String, Object>();
			// �����ǰFile���ļ��У�ʹ��folderͼ�ꣻ����ʹ��fileͼ��
			if (files[i].isDirectory())
			{
				listItem.put("icon", R.drawable.folder);
			}
			else
			{
				listItem.put("icon", R.drawable.file);
			}			
			
			listItem.put("fileName", files[i].getName());
			// ���List��
			listItems.add(listItem);
		}
		// ����һ��SimpleAdapter
		SimpleAdapter simpleAdapter = new SimpleAdapter(this
			, listItems, R.layout.line
			, new String[]{ "icon", "fileName" }
			, new int[]{R.id.icon, R.id.file_name });
		// ΪListView����Adapter
		listView.setAdapter(simpleAdapter);
		try
		{
			textView.setText("��ǰ·��Ϊ��" 
				+ currentParent.getCanonicalPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
    /* �ϴ��ļ���Server�ķ��� */
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
          /* ������������ */
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Charset", "UTF-8");
            con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
          /*����StrictMode ����HTTPURLConnection����ʧ�ܣ���Ϊ�������������н�����������*/
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
            DataOutputStream ds=new DataOutputStream(con.getOutputStream());

            //= new DataOutputStream(con.getOutputStream());  //output to the connection
            ds.writeBytes(twoHyphens + boundary + end);
            Log.i("www", uploadFileName.substring(uploadFileName.lastIndexOf("/")+1));
            ds.writeBytes("Content-Disposition: form-data; " +
                    "name=\"file\";filename=\"" +
                    uploadFileName.substring(uploadFileName.lastIndexOf("/")+1) + "\"" + end);
            ds.writeBytes(end);
          /* ȡ���ļ���FileInputStream */
            FileInputStream fStream = new FileInputStream(uploadFileName);
          /* ����ÿ��д��8192bytes */
            int bufferSize = 8192;
            byte[] buffer = new byte[bufferSize];   //8k
            int length = -1;
          /* ���ļ���ȡ������������ */
            while ((length = fStream.read(buffer)) != -1)
            {
            /* ������д��DataOutputStream�� */
                ds.write(buffer, 0, length);
            }
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
          /* �ر�����д��Ķ����Զ�����Http����*/
            fStream.close();
          /* �ر�DataOutputStream */
            ds.close();
          /* �ӷ��ص���������ȡ��Ӧ��Ϣ */
            InputStream is = con.getInputStream();  //input from the connection ��ʽ����HTTP����
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
          /* ��ʾ��ҳ��Ӧ���� */
            Log.i("www", b.toString().trim()+"hucd");
            return b.toString();
            //Toast.makeText(UploadfileActivity.this, b.toString().trim(), Toast.LENGTH_SHORT).show();//Post�ɹ�
        } catch (Exception e)
        {
        	Log.i("www", "fail"+e.getMessage());
            /* ��ʾ�쳣��Ϣ */
           // Toast.makeText(UploadfileActivity.this, "Fail:" + e, Toast.LENGTH_SHORT).show();//Postʧ��
        }
        return "";
    }
}