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
	 * 播放器
	 */
	private MediaPlayer player;
	/**
	 * Application对象
	 */
	private PlayMusicApplication app;
	/**
	 * 歌曲的List集合
	 */
	private ArrayList<Music> musics;
	/**
	 * 是否正在播放
	 */
	private boolean isPlaying;
	/**
	 * 暂停时的进度
	 */
	private int currentPosition;
	/**
	 * 是否由用户操作开始播放歌曲
	 */
	private boolean isStarted = false;
	/**
	 * 线程是否工作
	 */
	private boolean isThreadWorking;
	/**
	 * 更新Widget的子线程
	 */
	private UpdateWidgetThread widgetThread;
	/**
	 * 广播接收者
	 */
	private BroadcastReceiver receiver;
	/**
	 * 广播接收者的意图过滤器
	 */
	private IntentFilter filter;
	AppWidgetManager manager;
	ComponentName provider;
	RemoteViews views;

	@Override
	public void onCreate() {
		// 获取Application对象
		Log.i("zdk", "PlayMusicService.onCreate()");
		app = (PlayMusicApplication) getApplication();
		// 获取歌曲的List集合
		musics = app.getMusicList();

		manager = AppWidgetManager.getInstance(PlayMusicService.this);
		provider = new ComponentName(PlayMusicService.this,
				PlayMusicWidget.class);

		views = new RemoteViews(getPackageName(), R.layout.play_music_widget);
		// 初始化播放器
		player = new MediaPlayer();
		player.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (isStarted) {
					// 播放下一首

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
		// 注册广播接收者
		initReceiver();
		initButtonReceiver();
		// 设置线程启动标示
		isThreadWorking = true;
		// 启动更新桌面小部件线程
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
	 * 播放
	 */
	private void playMusic() {
		try {
			if (player.isPlaying()) {
				player.stop(); // 停止当前音频的播放
			}
			player.reset();

			player.setDataSource(musics.get(app.getCurrentMusicIndex())
					.getData());
			Log.i("zdk", "getCurrentMusicIndex==" + app.getCurrentMusicIndex());
			Log.i("music---","——————————————————————————信息————————————————————"+ musics.get(app.getCurrentMusicIndex()).getData()
					.toString());
			player.prepare();
			player.seekTo(currentPosition);
			player.start();
			app.setAudioSessionId(player.getAudioSessionId());
			// 清空变量中记录的当前播放进度
			currentPosition = 0;
			// 更新播放状态
			isPlaying = true;
			// 更新状态为：用户操作后开始播放歌曲
			isStarted = true;
			// 发送广播：正在播放歌曲
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
	 * 暂停
	 */
	private void pauseMusic() {
		Log.i("", "pauseMusic==" + app.getCurrentMusicIndex());
		if (isPlaying) {
			// 暂停
			player.pause();
			// 记录暂停的位置
			currentPosition = player.getCurrentPosition();
			// 更新播放状态
			isPlaying = false;

		}
	}

	/**
	 * 上一首
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
	 * 下一首
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
	 * 初始化并注册广播接收者
	 */
	private void initReceiver() {
		// 初始化广播接收者
		receiver = new InnerReceiver();
		// 初始化意图过滤器
		filter = new IntentFilter();
		// 添加接收的广播类型
		filter.addAction(ACTIVITY_PLAY_BUTTON_CLICK);
		filter.addAction(ACTIVITY_PREVIOUS_BUTTON_CLICK);
		filter.addAction(ACTIVITY_NEXT_BUTTON_CLICK);
		filter.addAction(ACTIVITY_START_MAIN);
		filter.addAction(ACTIVITY_NEW_OUTGOING_CALL);
		filter.addAction(ACTION_TIME_CHANGED);
		filter.addAction(ACTION_PHONE_STATE);

		// 注册广播接收者
		registerReceiver(receiver, filter);
	}

	/**
	 * 广播接收者
	 * 
	 * @author 郑敦坤
	 * 
	 */
	private class InnerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// 当播放按钮被点击时
			PhoneStateListener listener = new PhoneStateListener() {

				@Override
				public void onCallStateChanged(int state, String incomingNumber) {
					// TODO Auto-generated method stub
					// state 当前状态 incomingNumber,貌似没有去电的API
					super.onCallStateChanged(state, incomingNumber);
					switch (state) {
					case TelephonyManager.CALL_STATE_IDLE:
						System.out.println("挂断");

						playMusic();

						break;
					case TelephonyManager.CALL_STATE_OFFHOOK:
						System.out.println("接听");
						break;
					case TelephonyManager.CALL_STATE_RINGING:
						System.out.println("响铃:来电号码" + incomingNumber);

						Log.i("", "binder.stop()" + isPlaying);
						pauseMusic();

						// 输出来电号码
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
				Log.i("zdk", "时间被设置了");
				Toast.makeText(app, "时间被设置了", Toast.LENGTH_LONG).show();

			} else if (ACTIVITY_NEW_OUTGOING_CALL.equals(action)) {

				// 如果是去电（拨出）

				Toast.makeText(app, "正在拨出，请等待", Toast.LENGTH_LONG).show();
				TelephonyManager tm = (TelephonyManager) context
						.getSystemService(Service.TELEPHONY_SERVICE);
				tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
				System.out.println("拨出");
			} else {
				// 查了下android文档，貌似没有专门用于接收来电的action,所以，非去电即来电
				System.out.println("来电");
				Toast.makeText(app, "有电话哦，亲", Toast.LENGTH_LONG).show();
				TelephonyManager tm = (TelephonyManager) context
						.getSystemService(Service.TELEPHONY_SERVICE);
				tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
				// 设置一个监听器
			}
			// 更新
			manager.updateAppWidget(provider, views);
		}
	}

	/**
	 * 更新桌面小部件
	 * 
	 * @author 郑敦坤
	 * 
	 */
	private class UpdateWidgetThread extends Thread {
		
		@Override
		public void run() {
			Music music;
			int progress;
			while (isThreadWorking) {
				// 获取歌曲对象
				music = app.getMusicList().get(app.getCurrentMusicIndex());
				handler.sendEmptyMessage(0x123);//发送消息  
				
				views.setTextViewText(R.id.tv_widget_title, music.getTitle());
				views.setTextViewText(R.id.tv_widget_album, music.getAlbum());
				views.setTextViewText(R.id.tv_widget_artist, music.getArtist());
				views.setTextViewText(R.id.tv_widget_position,
						TextFormatter.getMusicTime(player.getCurrentPosition()));
				views.setTextViewText(R.id.tv_widget_duration,
						TextFormatter.getMusicTime(music.getDuration()));
				// 设置ImageView

				// 计算并设置进度条
				progress = (int) (player.getCurrentPosition() * 100 / music
						.getDuration());
				progress = progress > 100 ? 0 : progress;
				views.setProgressBar(R.id.sb_widget_progress, 100, progress,
						false);
				// 更新
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

	/** 通知栏按钮点击事件对应的ACTION */
	public final static String ACTION_BUTTON = "com.notifications.intent.action.ButtonClick";
	/** 通知栏按钮广播 */
	public ButtonBroadcastReceiver bReceiver;
	/** Notification管理 */
	public NotificationManager mNotificationManager;
	// Notification ID
	private int NID_1 = 0x1;

	public void sendNotification() {
		Music mp3InfoSend = this.musics.get(app.getCurrentMusicIndex());
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this);
		// 创建一个远程的视图
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
		// 如果版本号低于（3。0），那么不显示按钮
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

		// 点击的事件处理
		Intent buttonIntent = new Intent(ACTION_BUTTON);
		/* 播放/暂停 按钮 */
		buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_PLAY_ID);
		PendingIntent intent_play = PendingIntent.getBroadcast(this, 1,
				buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.btn_custom_play, intent_play);
		/* 下一首 按钮 */
		buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_NEXT_ID);
		PendingIntent intent_next = PendingIntent.getBroadcast(this, 2,
				buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.btn_custom_next, intent_next);
		/* 退出按钮 */
		buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_EXIT_ID);
		PendingIntent intent_exit = PendingIntent.getBroadcast(this, 3,
				buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.btn_custom_exit, intent_exit);

		builder.setContent(views).setContentIntent(pi)
				.setWhen(System.currentTimeMillis())// 通知产生的时间，会在通知信息里显示
				.setTicker("坤哥音乐").setPriority(Notification.PRIORITY_MAX)// 设置该通知优先级
				.setOngoing(true).setSmallIcon(R.drawable.app_logo2);

		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(NID_1, builder.build());
	}

	/**
	 * 初始化要用到的系统服务
	 */
	private void initService() {
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	/** 带按钮的通知栏点击广播接收 */
	public void initButtonReceiver() {
		bReceiver = new ButtonBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_BUTTON);
		registerReceiver(bReceiver, intentFilter);
	}

	public final static String INTENT_BUTTONID_TAG = "ButtonId";
	/** 播放/暂停 按钮点击 ID */
	public final static int BUTTON_PLAY_ID = 1;
	/** 下一首 按钮点击 ID */
	public final static int BUTTON_NEXT_ID = 2;
	/** 退出 按钮点击 ID */
	public final static int BUTTON_EXIT_ID = 3;

	/**
	 * 广播监听按钮点击时间
	 */
	public class ButtonBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (action.equals(ACTION_BUTTON)) {
				Log.i("zdk", "----------------ButtonBroadcastReceiver------------------------");
				// 通过传递过来的ID判断按钮点击属性或者通过getResultCode()获得相应点击事件
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
					mNotificationManager.cancelAll();// 删除你发的所有通知
					// 为Intent设置Action属性
					intent.setAction("com.muyu_Service");
					stopService(intent);

					int pid = android.os.Process.myPid();// 获取当前应用程序的PID
					android.os.Process.killProcess(pid);// 杀死当前进程
					break;

				default:
					break;
				}
			}
		}
	}
	  private Handler handler = new Handler() {  
		  Music	music;
	        // 该方法运行在主线程中  
	        // 接收到handler发送的消息，对UI进行操作  
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
		// 停止线程
		isThreadWorking = false;
		// 停止接收广播
		unregisterReceiver(receiver);
		if (player != null) {
			// 释放播放器资源

			if (player.isPlaying()) {
				player.stop(); // 停止音乐的播放
			}
			player.release();
			player = null;
		}
		super.onDestroy();
	}
}
