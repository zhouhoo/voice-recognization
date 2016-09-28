package com.example.OnlineRec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.example.OnlineRec.R;



import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RecordActivity extends Activity {
	private static  ClsOscilloscope clsOscilloscope=new ClsOscilloscope();
	SurfaceView sfv;Paint mPaint;
	static final int xMax = 16;//X轴缩小比例最大值,X轴数据量巨大，容易产生刷新延时
	static final int xMin = 8;//X轴缩小比例最小值
	static final int yMax = 60;//Y轴缩小比例最大值
	static final int yMin = 20;//Y轴缩小比例最小值
	
	//int recBufSize,playBufSize;//录音最小buffer大小
	//AudioRecord audioRecord;
	//AudioTrack audioTrack;
	
	private static boolean recordflag=false;
	private static EditText filename;
	private static TextView wavfilename;
	private static Button rec_or_stop;
	private static Button history;
	
	int recBufSize_,playBufSize_;//录音最小buffer大小
	AudioRecord audioRecord_;
	static AudioTrack audioTrack_;
	//static final int frequency_ = 44100;//分辨率
	static final int frequency_ = 16000;//分辨率
	static final int channelConfiguration_ = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	static final int audioEncoding_ = AudioFormat.ENCODING_PCM_16BIT;


	private static final String HOST = "192.168.43.215"; 
	private static final int PORT = 3333;
	private Socket socket = null;  
	private BufferedReader in = null; 
	private PrintWriter out = null;
	private String content = ""; 
	String buffer = "";
	
	private  Handler rec_handler_ = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i("rec_finished", "Got an incoming message from the child thread - "
                    + (String) msg.obj);
            // Toast.makeText(mActivity, "超出最大时长，录音自动停止了 ", 1).show();
            filename.setText((String) msg.obj);
            
    		//Toast.makeText(RecordActivity.this, (String) msg.obj+":"+_recSet.size(), 0).show();
            if(recordflag){
            	recordflag=false;
            	clsOscilloscope.Stop();
				recordflag=false;
			    rec_or_stop.setText("录音");
			    history.setEnabled(true);
			}
        }
    };
    class MyThread extends Thread {
		public String txt1;
		public MyThread(String str) {
			txt1 = str;
		}
		@Override
		public void run() {
			//定义消息
			Message msg = new Message();
			msg.what = 0x11;
			Bundle bundle = new Bundle();
			bundle.clear();
			try {
				//连接服务器 并设置连接超时为5秒
				socket = new Socket();
				socket.connect(new InetSocketAddress("192.168.43.215", 30000), 5000);
				//获取输入输出流
				OutputStream ou = socket.getOutputStream();
				BufferedReader bff = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				////读取发来服务器信息
				//String line = null;
				//buffer="";
				//while ((line = bff.readLine()) != null) {
				//	buffer = line + buffer;
				//}
				
				//向服务器发送信息
				ou.write("android 客户端".getBytes("UTF8"));
				ou.flush();
				bundle.putString("msg", buffer.toString());
				msg.setData(bundle);
				//发送消息 修改UI线程中的组件
				rec_handler_.sendMessage(msg);
				//关闭各种输入输出流
				bff.close();
				ou.close();
				socket.close();
			} catch (SocketTimeoutException aa) {
				//连接超时 在UI界面显示消息
				bundle.putString("msg", "服务器连接失败！请检查网络是否打开");
				msg.setData(bundle);
				//发送消息 修改UI线程中的组件
				rec_handler_.sendMessage(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);
		sfv = (SurfaceView) this.findViewById(R.id.SurfaceView01); 
		sfv.setOnTouchListener(new TouchEvent());
		//sfv.setZOrderOnTop(true);//设置画布  背景透明
		//sfv.getHolder().setFormat(PixelFormat.TRANSLUCENT);
	    mPaint = new Paint();  
	    mPaint.setColor(Color.GREEN);// 画笔为绿色  
	    mPaint.setStrokeWidth(1);// 设置画笔粗细 
	    
	    recBufSize_ = AudioRecord.getMinBufferSize(frequency_,
				channelConfiguration_, audioEncoding_);
		playBufSize_=AudioTrack.getMinBufferSize(frequency_,
				channelConfiguration_, audioEncoding_);
		audioRecord_ = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency_,
				channelConfiguration_, audioEncoding_, recBufSize_);
		audioTrack_ = new AudioTrack(AudioManager.STREAM_MUSIC, frequency_,
				channelConfiguration_, audioEncoding_,
				playBufSize_, AudioTrack.MODE_STREAM);
		
	    clsOscilloscope.initOscilloscope(xMax/2, yMax/2, sfv.getHeight()/2);
	    
	    filename = (EditText) findViewById(R.id.editText_Fname);
	    filename.setText("hhhhh");
	    filename.addTextChangedListener(mTextWatcher);
	    wavfilename = (TextView) findViewById(R.id.wavFileName);
	    
	    rec_or_stop=(Button)findViewById(R.id.rec_or_stop);
	    rec_or_stop.setText("录音");
	    history=(Button)findViewById(R.id.check_history);
	    
	    File sdCardDir = Environment.getExternalStorageDirectory();
	    File file = new File(sdCardDir+"/hucd");
	  //判断文件夹是否存在,如果不存在则创建文件夹
		if (!file.exists()) {
			file.mkdir();
		}
		//new MyThread("hucd").start(); 
		filename.setText("请输入ip地址以回车结束");
	    Log.i("file_root",sdCardDir.getPath());
	    rec_or_stop.setOnTouchListener(new OnTouchListener() {

			@Override

			public boolean onTouch(View v, MotionEvent event) {

				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					clsOscilloscope.baseLine=sfv.getHeight()/2;
					clsOscilloscope.Start(audioRecord_,recBufSize_,sfv,mPaint,rec_handler_);
					recordflag=true;
					rec_or_stop.setText("停止");
					history.setEnabled(false);
					// 按下

				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					clsOscilloscope.Stop();
					recordflag=false;
					rec_or_stop.setText("录音");
					history.setEnabled(true);
								
					// 弹起
				}

				return false;

			}

		});
	    

	//rec_or_stop.setOnClickListener(new OnClickListener()
	//{
	//	@Override
	//	public void onClick(View v)
	//	{
    //		if(recordflag==false){
    //		 //   g.currfile_=g.root_+g.userName+"/"+g.date_+"_"+g.userName+"_"+g.androidID_;
    //		 //   g.Name=filename.getText().toString();
    //		    
	//			clsOscilloscope.baseLine=sfv.getHeight()/2;
	//		    clsOscilloscope.Start(audioRecord_,recBufSize_,sfv,mPaint,rec_handler_);
	//			recordflag=true;
	//			rec_or_stop.setText("停止");
	//			history.setEnabled(false);
	//		}else{
	//		    
	//			clsOscilloscope.Stop();
	//			recordflag=false;
	//			rec_or_stop.setText("录音");
	//			history.setEnabled(true);
	//			
	//			SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyyMMddhhmmss");
	//		//    g.date_   =   sDateFormat.format(new   java.util.Date()); 
	//		//	filename.setText(g.date_);
	//			
	//		}
	//	}
	//});
	    history.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(RecordActivity.this, UploadfileActivity.class);
				startActivity(intent);
			}
		});
	 //   SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyyMMddhhmmss");
	   // g.date_   =   sDateFormat.format(new   java.util.Date()); 
	  //  g.currfile_=g.root_+g.userName+"/"+g.date_+"_"+g.userName+"_"+g.androidID_;
	  //  filename.setText(g.date_);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record, menu);
		return true;
	}
	class TouchEvent implements OnTouchListener{
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			clsOscilloscope.baseLine=(int)event.getY();
			return true;
		}
		
	}
	private static int ReadAudioToBuff(String filename,byte [] buff){
		int length=0;
		try
		{
			// 如果手机插入了SD卡，而且应用程序具有访问SD的权限
			if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED))
			{
				FileInputStream in = new FileInputStream(filename);
   
				byte[] buffer = new byte[44];
				int len=in.read(buffer, 0, 44);
				while (len != -1) {
				    len = in.read(buff);
				    length+=len;
				}
				in.close();	
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return length;
	}
	static final int MAX_BUFF_SIZE = 10485760;
	public static void PlayAudioFile(String filename){
		final byte[] buff=new byte[MAX_BUFF_SIZE];
		final int len=ReadAudioToBuff(filename,buff);
		
		new Thread() {
			public void run() {
				try { 
					audioTrack_.stop();
					synchronized (audioTrack_) {
						audioTrack_.play();
						audioTrack_.write(buff, 0, len);
						audioTrack_.stop();
					}
				} catch (Throwable t) {
					//Toast.makeText(Global.this, t.getMessage(), 1000).show();
				}
			}
		}.start();
	}
	  TextWatcher mTextWatcher = new TextWatcher() {
	        private CharSequence temp;
	        private int editStart ;
	        private int editEnd ;
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				 temp = s;
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
//				mTextView.setText(s);//将输入的内容实时显示
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
	            editStart = filename.getSelectionStart();
	            editEnd = filename.getSelectionEnd();
	            //mTextView.setText("您输入了" + temp.length() + "个字符");
	            if (temp.length()>0&&temp.charAt(temp.length()-1)=='\n') {
	               // Toast.makeText(TestActivity.this,
	               //         "你输入的字数已经超过了限制！", Toast.LENGTH_SHORT)
	               //         .show();
	               // int tempSelection = editStart;
	            	UploadfileActivity.postUrl="http://"+temp.subSequence(0, temp.length()-1)+"/upload.php";
	            	UploadfileActivity.checkUrl="http://"+temp.subSequence(0, temp.length()-1)+"/check.php";
	                filename.setText(UploadfileActivity.postUrl);
	               // mEditText.setSelection(tempSelection);
	            }
			}
		};
	
}
