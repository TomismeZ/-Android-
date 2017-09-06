package com.zdk.mymusic.service;

import java.io.IOException;
import java.util.ArrayList;

import com.zdk.mymusic.MainActivity;
import com.zdk.mymusic.R;
import com.zdk.mymusic.application.PlayMusicApplication;
import com.zdk.mymusic.dao.MusicDao;
import com.zdk.mymusic.entity.Music;
import com.zdk.mymusic.myinterface.IMusicPlay;
import com.zdk.mymusic.utils.ApplicationConsts;
import com.zdk.mymusic.utils.BaseTools;
import com.zdk.mymusic.utils.TextFormatter;
import com.zdk.mymusic.widget.PlayMusicWidget;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

@SuppressLint("HandlerLeak") public class PlayMusicService extends Service implements ApplicationConsts {

	private IBinder binder;
	/**
	 * ������
	 */
	private MediaPlayer player;
	/**
	 * Application����
	 */
	private PlayMusicApplication app;
	/**
	 * ������List����
	 */
	private ArrayList<Music> musics;
	/**
	 * �Ƿ����ڲ���
	 */
	private boolean isPlaying;
	/**
	 * ��ͣʱ�Ľ���
	 */
	private int currentPosition;
	/**
	 * �Ƿ����û�������ʼ���Ÿ���
	 */
	private boolean isStarted = false;
	/**
	 * �߳��Ƿ���
	 */
	private boolean isThreadWorking;
	/**
	 * ����Widget�����߳�
	 */
	private UpdateWidgetThread widgetThread;
	/**
	 * �㲥������
	 */
	private BroadcastReceiver receiver;
	/**
	 * �㲥�����ߵ���ͼ������
	 */
	private IntentFilter filter;
	AppWidgetManager manager;
	ComponentName provider;
	RemoteViews views;

	@Override
	public void onCreate() {
		// ��ȡApplication����
		Log.i("zdk", "PlayMusicService.onCreate()");
		app = (PlayMusicApplication) getApplication();
		// ��ȡ������List����
		musics = app.getMusicList();

		manager = AppWidgetManager.getInstance(PlayMusicService.this);
		provider = new ComponentName(PlayMusicService.this,
				PlayMusicWidget.class);

		views = new RemoteViews(getPackageName(), R.layout.play_music_widget);
		// ��ʼ��������
		player = new MediaPlayer();
		player.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (isStarted) {
					// ������һ��

					Log.i("onCompletion", mp.getDuration() + "getDuration");
					Log.i("onCompletion", mp.getCurrentPosition()
							+ "getCurrentPosition");
					Log.i("onCompletion", app.getCurrentMusicIndex() + "");
					nextMusic();
					views.setImageViewResource(R.id.ib_widget_play,
							R.drawable.button_pause);
				}

			}
		});
		player.setOnErrorListener(new OnErrorListener() {
			
			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				player.reset();
				return false;
			}
		});
		sendNotification();
		initService();
		// ע��㲥������
		initReceiver();
		initButtonReceiver();
		// �����߳�������ʾ
		isThreadWorking = true;
		// ������������С�����߳�
		widgetThread = new UpdateWidgetThread();
		widgetThread.start();
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		binder = new PlayMusicBinder();
		return binder;
	}

	/**
	 * ����
	 */
	private void playMusic() {
		try {
			if (player.isPlaying()) {
				player.stop(); // ֹͣ��ǰ��Ƶ�Ĳ���
			}
			player.reset();

			player.setDataSource(musics.get(app.getCurrentMusicIndex())
					.getData());
			Log.i("zdk", "getCurrentMusicIndex==" + app.getCurrentMusicIndex());
			Log.i("music---","������������������������������������������������������Ϣ����������������������������������������"+ musics.get(app.getCurrentMusicIndex()).getData()
					.toString());
			player.prepare();
			player.seekTo(currentPosition);
			player.start();
			app.setAudioSessionId(player.getAudioSessionId());
			// ��ձ����м�¼�ĵ�ǰ���Ž���
			currentPosition = 0;
			// ���²���״̬
			isPlaying = true;
			// ����״̬Ϊ���û�������ʼ���Ÿ���
			isStarted = true;
			// ���͹㲥�����ڲ��Ÿ���
			Intent intent = new Intent();
			intent.setAction(SERVICE_PLAYER_PLAY);
			sendBroadcast(intent);
			sendNotification();

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ��ͣ
	 */
	private void pauseMusic() {
		Log.i("", "pauseMusic==" + app.getCurrentMusicIndex());
		if (isPlaying) {
			// ��ͣ
			player.pause();
			// ��¼��ͣ��λ��
			currentPosition = player.getCurrentPosition();
			// ���²���״̬
			isPlaying = false;

		}
	}

	/**
	 * ��һ��
	 */
	private void previousMusic() {
		Log.i("", "previousMusic==" + app.getCurrentMusicIndex());
		int index = app.getCurrentMusicIndex();
		if (index - 1 < 0) {
			index = musics.size() - 1;
		} else {
			index--;
		}
		app.setCurrentMusicIndex(index);
		playMusic();
	}

	/**
	 * ��һ��
	 */
	private void nextMusic() {
		Log.i("", "nextMusic==" + app.getCurrentMusicIndex());
		int index = app.getCurrentMusicIndex();
		if (index + 1 >= musics.size()) {
			index = 0;
		} else {
			index++;
		}
		app.setCurrentMusicIndex(index);
		playMusic();

	}

	public class PlayMusicBinder extends Binder implements IMusicPlay {
		@Override
		public void play() {

			Log.i("", "PlayMusicBinder---play()");
			playMusic();
		}

		@Override
		public void stop() {
			Log.i("", "PlayMusicBinder---stop()");
			pauseMusic();
		}

		@Override
		public int getProgress() {

			return player.getCurrentPosition();
		}

		@Override
		public void playSeekTo(int progress) {
			Log.i("", "playSeekTo---()");
			currentPosition = (int) (musics.get(app.getCurrentMusicIndex())
					.getDuration() * progress / 100);
			playMusic();
		}

		@Override
		public void playNext() {
			nextMusic();

		}

		@Override
		public void playPrevious() {
			previousMusic();

		}

		@Override
		public int getAudioSessionId() {

			return player.getAudioSessionId();
		}

	}

	/**
	 * ��ʼ����ע��㲥������
	 */
	private void initReceiver() {
		// ��ʼ���㲥������
		receiver = new InnerReceiver();
		// ��ʼ����ͼ������
		filter = new IntentFilter();
		// ��ӽ��յĹ㲥����
		filter.addAction(ACTIVITY_PLAY_BUTTON_CLICK);
		filter.addAction(ACTIVITY_PREVIOUS_BUTTON_CLICK);
		filter.addAction(ACTIVITY_NEXT_BUTTON_CLICK);
		filter.addAction(ACTIVITY_START_MAIN);
		filter.addAction(ACTIVITY_NEW_OUTGOING_CALL);
		filter.addAction(ACTION_TIME_CHANGED);
		filter.addAction(ACTION_PHONE_STATE);

		// ע��㲥������
		registerReceiver(receiver, filter);
	}

	/**
	 * �㲥������
	 * 
	 * @author ֣����
	 * 
	 */
	private class InnerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// �����Ű�ť�����ʱ
			PhoneStateListener listener = new PhoneStateListener() {

				@Override
				public void onCallStateChanged(int state, String incomingNumber) {
					// TODO Auto-generated method stub
					// state ��ǰ״̬ incomingNumber,ò��û��ȥ���API
					super.onCallStateChanged(state, incomingNumber);
					switch (state) {
					case TelephonyManager.CALL_STATE_IDLE:
						System.out.println("�Ҷ�");

						playMusic();

						break;
					case TelephonyManager.CALL_STATE_OFFHOOK:
						System.out.println("����");
						break;
					case TelephonyManager.CALL_STATE_RINGING:
						System.out.println("����:�������" + incomingNumber);

						Log.i("", "binder.stop()" + isPlaying);
						pauseMusic();

						// ����������
						break;
					}
				}
			};
			if (ACTIVITY_PLAY_BUTTON_CLICK.equals(action)) {

				if (isPlaying) {
					pauseMusic();
					views.setImageViewResource(R.id.ib_widget_play,
							R.drawable.button_play);
				} else {
					playMusic();

					views.setImageViewResource(R.id.ib_widget_play,
							R.drawable.button_pause);
				}
				sendNotification();
			} else if (ACTIVITY_PREVIOUS_BUTTON_CLICK.equals(action)) {
				previousMusic();
			} else if (ACTIVITY_NEXT_BUTTON_CLICK.equals(action)) {
				nextMusic();
			} else if (ACTION_TIME_CHANGED.equals(action)) {
				Log.i("zdk", "ʱ�䱻������");
				Toast.makeText(app, "ʱ�䱻������", Toast.LENGTH_LONG).show();

			} else if (ACTIVITY_NEW_OUTGOING_CALL.equals(action)) {

				// �����ȥ�磨������

				Toast.makeText(app, "���ڲ�������ȴ�", Toast.LENGTH_LONG).show();
				TelephonyManager tm = (TelephonyManager) context
						.getSystemService(Service.TELEPHONY_SERVICE);
				tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
				System.out.println("����");
			} else {
				// ������android�ĵ���ò��û��ר�����ڽ��������action,���ԣ���ȥ�缴����
				System.out.println("����");
				Toast.makeText(app, "�е绰Ŷ����", Toast.LENGTH_LONG).show();
				TelephonyManager tm = (TelephonyManager) context
						.getSystemService(Service.TELEPHONY_SERVICE);
				tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
				// ����һ��������
			}
			// ����
			manager.updateAppWidget(provider, views);
		}
	}

	/**
	 * ��������С����
	 * 
	 * @author ֣����
	 * 
	 */
	private class UpdateWidgetThread extends Thread {
		
		@Override
		public void run() {
			Music music;
			int progress;
			while (isThreadWorking) {
				// ��ȡ��������
				music = app.getMusicList().get(app.getCurrentMusicIndex());
				handler.sendEmptyMessage(0x123);//������Ϣ  
				
				views.setTextViewText(R.id.tv_widget_title, music.getTitle());
				views.setTextViewText(R.id.tv_widget_album, music.getAlbum());
				views.setTextViewText(R.id.tv_widget_artist, music.getArtist());
				views.setTextViewText(R.id.tv_widget_position,
						TextFormatter.getMusicTime(player.getCurrentPosition()));
				views.setTextViewText(R.id.tv_widget_duration,
						TextFormatter.getMusicTime(music.getDuration()));
				// ����ImageView

				// ���㲢���ý�����
				progress = (int) (player.getCurrentPosition() * 100 / music
						.getDuration());
				progress = progress > 100 ? 0 : progress;
				views.setProgressBar(R.id.sb_widget_progress, 100, progress,
						false);
				// ����
				manager.updateAppWidget(provider, views);

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			super.run();
		}

	}

	/** ֪ͨ����ť����¼���Ӧ��ACTION */
	public final static String ACTION_BUTTON = "com.notifications.intent.action.ButtonClick";
	/** ֪ͨ����ť�㲥 */
	public ButtonBroadcastReceiver bReceiver;
	/** Notification���� */
	public NotificationManager mNotificationManager;
	// Notification ID
	private int NID_1 = 0x1;

	public void sendNotification() {
		Music mp3InfoSend = this.musics.get(app.getCurrentMusicIndex());
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this);
		// ����һ��Զ�̵���ͼ
		RemoteViews views = new RemoteViews(getPackageName(),
				R.layout.custom_layout);
		Bitmap albumBitmap = MusicDao.getArtwork(this, mp3InfoSend.getId(),
				mp3InfoSend.getAlbumId(), false, false);
		if (albumBitmap != null) {
			views.setImageViewBitmap(R.id.custom_song_icon, albumBitmap);
		} else {
			views.setImageViewResource(R.id.custom_song_icon,
					R.drawable.app_logo2);
		}
		views.setTextViewText(R.id.tv_custom_song_name, mp3InfoSend.getTitle());
		views.setTextViewText(R.id.tv_custom_song_singer,
				mp3InfoSend.getArtist());
		// ����汾�ŵ��ڣ�3��0������ô����ʾ��ť
		if (BaseTools.getSystemVersion() <= 9) {
			views.setViewVisibility(R.id.ll_custom_button, View.GONE);
		} else {
			views.setViewVisibility(R.id.ll_custom_button, View.VISIBLE);

			if (isPlaying) {
				views.setImageViewResource(R.id.btn_custom_play,
						R.drawable.pause2);
			} else {
				views.setImageViewResource(R.id.btn_custom_play,
						R.drawable.play2);
			}
		}
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		// ������¼�����
		Intent buttonIntent = new Intent(ACTION_BUTTON);
		/* ����/��ͣ ��ť */
		buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_PLAY_ID);
		PendingIntent intent_play = PendingIntent.getBroadcast(this, 1,
				buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.btn_custom_play, intent_play);
		/* ��һ�� ��ť */
		buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_NEXT_ID);
		PendingIntent intent_next = PendingIntent.getBroadcast(this, 2,
				buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.btn_custom_next, intent_next);
		/* �˳���ť */
		buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_EXIT_ID);
		PendingIntent intent_exit = PendingIntent.getBroadcast(this, 3,
				buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.btn_custom_exit, intent_exit);

		builder.setContent(views).setContentIntent(pi)
				.setWhen(System.currentTimeMillis())// ֪ͨ������ʱ�䣬����֪ͨ��Ϣ����ʾ
				.setTicker("��������").setPriority(Notification.PRIORITY_MAX)// ���ø�֪ͨ���ȼ�
				.setOngoing(true).setSmallIcon(R.drawable.app_logo2);

		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(NID_1, builder.build());
	}

	/**
	 * ��ʼ��Ҫ�õ���ϵͳ����
	 */
	private void initService() {
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	/** ����ť��֪ͨ������㲥���� */
	public void initButtonReceiver() {
		bReceiver = new ButtonBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_BUTTON);
		registerReceiver(bReceiver, intentFilter);
	}

	public final static String INTENT_BUTTONID_TAG = "ButtonId";
	/** ����/��ͣ ��ť��� ID */
	public final static int BUTTON_PLAY_ID = 1;
	/** ��һ�� ��ť��� ID */
	public final static int BUTTON_NEXT_ID = 2;
	/** �˳� ��ť��� ID */
	public final static int BUTTON_EXIT_ID = 3;

	/**
	 * �㲥������ť���ʱ��
	 */
	public class ButtonBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (action.equals(ACTION_BUTTON)) {
				Log.i("zdk", "----------------ButtonBroadcastReceiver------------------------");
				// ͨ�����ݹ�����ID�жϰ�ť������Ի���ͨ��getResultCode()�����Ӧ����¼�
				int buttonId = intent.getIntExtra(INTENT_BUTTONID_TAG, 0);
				switch (buttonId) {

				case BUTTON_PLAY_ID:
					if (isPlaying) {
						pauseMusic();
					}else{
						playMusic();
					}
					sendNotification();
					break;
				case BUTTON_NEXT_ID:
					nextMusic();
//					sendNotification();
					break;
				case BUTTON_EXIT_ID:
					mNotificationManager.cancelAll();// ɾ���㷢������֪ͨ
					// ΪIntent����Action����
					intent.setAction("com.muyu_Service");
					stopService(intent);

					int pid = android.os.Process.myPid();// ��ȡ��ǰӦ�ó����PID
					android.os.Process.killProcess(pid);// ɱ����ǰ����
					break;

				default:
					break;
				}
			}
		}
	}
	  private Handler handler = new Handler() {  
		  Music	music;
	        // �÷������������߳���  
	        // ���յ�handler���͵���Ϣ����UI���в���  
	        @SuppressLint("HandlerLeak") @Override  
	        public void handleMessage(Message msg) {  
	            // TODO Auto-generated method stub  
	            if (msg.what == 0x123) {  
	            music = app.getMusicList().get(app.getCurrentMusicIndex());
	            	Bitmap albumBitmap = MusicDao.getArtwork(app, music.getId(),
							music.getAlbumId(), false, false);
					if (albumBitmap != null) {
						views.setImageViewBitmap(R.id.iv_widget_image, albumBitmap);
					} else {
						views.setImageViewResource(R.id.iv_widget_image,
								R.drawable.icon);
					} 
	            }  
	        }  
	    };  
	@Override
	public void onDestroy() {
		// ֹͣ�߳�
		isThreadWorking = false;
		// ֹͣ���չ㲥
		unregisterReceiver(receiver);
		if (player != null) {
			// �ͷŲ�������Դ

			if (player.isPlaying()) {
				player.stop(); // ֹͣ���ֵĲ���
			}
			player.release();
			player = null;
		}
		super.onDestroy();
	}
}
