package com.example.OnlineRec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.SurfaceView;

public class ClsOscilloscope {
	private ArrayList<short[]> inBuf = new ArrayList<short[]>();
	private boolean isRecording = false;// �߳̿��Ʊ��
	private boolean isPlaying = false;// �߳̿��Ʊ��
	/**
	 * X����С�ı���
	 */
	public int rateX = 4;
	/**
	 * Y����С�ı���
	 */
	public int rateY = 4;
	/**
	 * Y�����
	 */
	public int baseLine = 0;

	/**
	 * ��ʼ��
	 */
	public void initOscilloscope(int rateX, int rateY, int baseLine) {
		this.rateX = rateX;
		this.rateY = rateY;
		this.baseLine = baseLine;
	}

	/**
	 * ��ʼ
	 * 
	 * @param recBufSize
	 *            AudioRecord��MinBufferSize
	 */
	public void Start(AudioRecord audioRecord, int recBufSize, SurfaceView sfv,
			Paint mPaint, Handler handler) {
		isRecording = true;
		new RecordThread(audioRecord, recBufSize, handler).start();// ��ʼ¼���߳�
		new DrawThread(sfv, mPaint).start();// ��ʼ�����߳�
	}

	/**
	 * ֹͣ
	 */
	public void Stop() {
		isRecording = false;

		inBuf.clear();// ���
	}

	/**
	 * �����MIC�������ݵ�inBuf
	 * 
	 * @author GV
	 * 
	 */
	static Socket socket=new Socket() ;
	static OutputStream ou ;
	static BufferedReader bff ;
	
	class RecordThread extends Thread {
		private int recBufSize;
		private AudioRecord audioRecord;
		private Handler handler_;
		static final int MAX_BUFF_SIZE = 10485760;

	

		public RecordThread(AudioRecord audioRecord, int recBufSize,
				Handler handler) {
			this.audioRecord = audioRecord;
			this.recBufSize = recBufSize;
			this.handler_ = handler;
		}

		public void run() {
			try {
				byte[] buff = new byte[MAX_BUFF_SIZE];
				int bufflen = 0;

				short[] buffer = new short[recBufSize / 2];
				byte[] bytebuff = new byte[recBufSize];
				audioRecord.startRecording();// ��ʼ¼��

				while (isRecording) {
					// ��MIC�������ݵ�������
					int bufferReadResult = audioRecord.read(bytebuff, 0,
							recBufSize);
					/*************** copy to buff for file and for play **********************/
					if ((bufflen + bufferReadResult) < MAX_BUFF_SIZE) {
						System.arraycopy(bytebuff, 0, buff, bufflen,
								bufferReadResult);
						bufflen += bufferReadResult;
					} else {
						audioRecord.stop();
						Message toMain = handler_.obtainMessage();
						toMain.obj = "buff full, record end!";
						handler_.sendMessage(toMain);
						isRecording = false;
					}
					/********************* convert for paint *********************/
					int bc = 0;
					for (int i = 0; i < bufferReadResult; i = i + 2) {
						buffer[bc++] = (short) (((bytebuff[i + 1] & 0xFF) << 8) | (bytebuff[i] & 0xFF));
					}
					/********************* get the paint buff *********************/
					short[] tmpBuf = new short[bc / rateX];
					for (int i = 0, ii = 0; i < tmpBuf.length; i++, ii = i
							* rateX) {
						tmpBuf[i] = buffer[ii];
					}
					/********************* add to the paint buff *********************/
					synchronized (inBuf) {//
						inBuf.add(tmpBuf);// �������
					}
				}
				audioRecord.stop();
				synchronized (this) {//
					SimpleDateFormat sDateFormat = new SimpleDateFormat(
							"yyyy_MMddhh_mmss");
				
					String date_ = sDateFormat.format(new java.util.Date());
					File sdCardDir = Environment.getExternalStorageDirectory();
					String root=sdCardDir.getPath();
					String currfile_=root+"/hucd"+"/"+date_;
					Log.i("save", currfile_+" start");
					SaveBuffToFile(buff, bufflen,currfile_+".wav");
					
					
					Message toMain = handler_.obtainMessage();
	                String recRsult= UploadfileActivity.UploadFile(currfile_+".wav");
	                toMain.obj =recRsult;
					handler_.sendMessage(toMain);
					
					Log.i("save", currfile_+" end");
					
					try {
						if(!socket.isConnected()){
							//���ӷ����� ���������ӳ�ʱΪ5��
							socket.connect(new InetSocketAddress("192.168.43.215", 30000), 5000);
							//��ȡ���������
							ou = socket.getOutputStream();
							bff = new BufferedReader(new InputStreamReader(
								socket.getInputStream()));
						}
						//�������������Ϣ
						ou.write(("0001"+recRsult+"\0").getBytes("UTF8"));
						ou.flush();

						//�رո������������
						//bff.close();
						//ou.close();
						//socket.close();
					} catch (SocketTimeoutException aa) {
						//���ӳ�ʱ ��UI������ʾ��Ϣ
						bff.close();
						ou.close();
						socket.close();
					} catch (IOException e) {
						bff.close();
						ou.close();
						socket.close();
						e.printStackTrace();
					}
				}
			} catch (Throwable t) {
			}
		}
	};

	/**
	 * �������inBuf�е�����
	 * 
	 * @author GV
	 * 
	 */
	class DrawThread extends Thread {
		private int oldX = 0;// �ϴλ��Ƶ�X����
		private int oldY = 0;// �ϴλ��Ƶ�Y����
		private SurfaceView sfv;// ����
		private int X_index = 0;// ��ǰ��ͼ������ĻX�������
		private Paint mPaint;// ����

		public DrawThread(SurfaceView sfv, Paint mPaint) {
			this.sfv = sfv;
			this.mPaint = mPaint;
		}

		public void run() {
			while (isRecording || isPlaying) {
				ArrayList<short[]> buf = new ArrayList<short[]>();
				synchronized (inBuf) {
					if (inBuf.size() == 0)
						continue;
					buf = (ArrayList<short[]>) inBuf.clone();// ����
					inBuf.clear();// ���
				}
				for (int i = 0; i < buf.size(); i++) {
					short[] tmpBuf = buf.get(i);
					SimpleDraw(X_index, tmpBuf, rateY, baseLine);// �ѻ��������ݻ�����
					X_index = X_index + tmpBuf.length;
					if (X_index > sfv.getWidth()) {
						X_index = 0;
					}
				}
			}
		}

		/**
		 * ����ָ������
		 * 
		 * @param start
		 *            X�Ὺʼ��λ��(ȫ��)
		 * @param buffer
		 *            ������
		 * @param rate
		 *            Y��������С�ı���
		 * @param baseLine
		 *            Y�����
		 */
		void SimpleDraw(int start, short[] buffer, int rate, int baseLine) {
			if (start == 0)
				oldX = 0;
			Canvas canvas = sfv.getHolder().lockCanvas(
					new Rect(start, 0, start + buffer.length, sfv.getHeight()));// �ؼ�:��ȡ����
			canvas.drawColor(Color.BLACK);// �������
			int y;
			for (int i = 0; i < buffer.length; i++) {// �ж��ٻ�����
				int x = i + start;
				y = buffer[i] / rate + baseLine;// ������С���������ڻ�׼��
				canvas.drawLine(oldX, oldY, x, y, mPaint);
				oldX = x;
				oldY = y;
			}
			sfv.getHolder().unlockCanvasAndPost(canvas);// �����������ύ���õ�ͼ��
		}
	}

//	public void StartPlay(SurfaceView sfv, Paint mPaint, Handler handler) {
//		isPlaying = true;
//		inBuf.clear();
		//new Play_Draw_Thread(handler).start();// ��ʼ¼���߳�
		//new DrawThread(sfv, mPaint).start();// ��ʼ�����߳�
//	}

	public void StopPlay() {
		isPlaying = false;
		inBuf.clear();
		// g.audioTrack_.stop();
	}

//class Play_Draw_Thread extends Thread {
//	private int playedbuff_ = 0;
//	private Handler handler_;
//
//	public Play_Draw_Thread(Handler handler) {
//		handler_ = handler;
//	}
//
//	public void run() {
//		try {
//			short[] buffer = new short[g.recBufSize_ / 2];
//			byte[] bytebuff = new byte[g.recBufSize_];
//			// g.audioTrack_.play();// ��ʼ����
//			while (isPlaying) {
//				/********************* copy for play *********************/
//				if ((playedbuff_ < g.audio_used_Buflen_ + g.recBufSize_)
//						&& (g.audio_used_Buflen_ >= g.recBufSize_)) {
//					System.arraycopy(g.audiodatBuf_, playedbuff_, bytebuff,
//							0, g.recBufSize_);
//					playedbuff_ += g.recBufSize_;
//					g.audioTrack_.write(bytebuff, 0, g.recBufSize_);
//				} else {
//					isPlaying = false;
//					break;
//				}
//				/********************* convert for paint *********************/
//				int bc = 0;
//				for (int i = 0; i < g.recBufSize_; i = i + 2) {
//					buffer[bc++] = (short) (((bytebuff[i + 1] & 0xFF) << 8) | (bytebuff[i] & 0xFF));
//				}
//				short[] tmpBuf = new short[bc / rateX];
//				for (int i = 0, ii = 0; i < tmpBuf.length; i++, ii = i
//						* rateX) {
//					tmpBuf[i] = buffer[ii];
//				}
//				synchronized (inBuf) {//
//					inBuf.add(tmpBuf);// �������
//				}
//			}
//			g.audioTrack_.stop();
//
//			Message toMain = handler_.obtainMessage();
//			toMain.obj = "finished play!";
//			handler_.sendMessage(toMain);
//		} catch (Throwable t) {
//		}
//	}
//};
	public void WriteWaveFileHeader(RandomAccessFile raf,
			long totalAudioLen, long totalDataLen, long longSampleRate,
			int channels, long byteRate) throws IOException {
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		//header[32] = (byte) (2 * 16 / 8); // block align
		header[32] = (byte) (16 / 8); // block align
		header[33] = 0;
		header[34] = 16; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		raf.write(header);
	}
	public void SaveBuffToFile(byte[] buffer,int lenght,String file){
		try
		{
			Log.i("save", "start save");
			// ����ֻ�������SD��������Ӧ�ó�����з���SD��Ȩ��
			if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED))
			{
				Log.i("save", "geted SD card");
				// ��ȡSD����Ŀ¼
		//		File sdCardDir = Environment.getExternalStorageDirectory();
				File targetFile = new File(file);
				Log.i("save", "geted file"+file);
				// ��ָ���ļ����� RandomAccessFile����
				RandomAccessFile raf = new RandomAccessFile(
					targetFile, "rw");
				// ���ļ���¼ָ���ƶ������
				Log.i("save", "geted RandomAccessFile");
				raf.seek(0);
				// ����ļ�����
				WriteWaveFileHeader(raf, lenght, lenght+36, 16000, 1, 16 * 16000 * 1 / 8);
				//raf.write(audiodatBuf_);
				Log.i("save", "write head");
				raf.write(buffer, 0, lenght);
				Log.i("save", "write buffer");
				// �ر�RandomAccessFile				
				raf.close();
			}
		}
		catch (Exception e)
		{
			Log.i("save", "fail"+e.getMessage());
			e.printStackTrace();
		}
	}
}
