package com.zdk.mymusic.application;

import java.util.ArrayList;

import com.zdk.mymusic.dao.MusicDao;
import com.zdk.mymusic.entity.Music;

import android.app.Application;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class PlayMusicApplication extends Application {
	/**
	 * 歌曲的List集合
	 */
	private ArrayList<Music> musics;
	/**
	 * 当前播放的歌曲
	 */
	private int currentMusicIndex = 0;
	private int AudioSessionId;

	public int getAudioSessionId() {
		return AudioSessionId;
	}

	public void setAudioSessionId(int audioSessionId) {
		AudioSessionId = audioSessionId;
	}

	@Override
	public void onCreate() {
		Log.i("zdk", "PlayMusicApplication.onCreate()");
		// 获取歌曲的List集合
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {// 判断sd卡状态
			Toast.makeText(getApplicationContext(), "扫描完成", Toast.LENGTH_LONG).show();
		musics = new MusicDao(this).getMusicList();
		}else{
			
		Toast.makeText(getApplicationContext(), "您手机上未有SD卡，请检查一下插入是否正确", Toast.LENGTH_LONG).show();
		}
		super.onCreate();
	}

	/**
	 * 获取歌曲的List集合
	 * 
	 * @return 歌曲的List集合
	 */
	public ArrayList<Music> getMusicList() {
		return this.musics;
	}
	
	/**
	 * 获取当前正在播放的歌曲的索引
	 * 
	 * @return 当前正在播放的歌曲的索引
	 */
	public int getCurrentMusicIndex() {
		return currentMusicIndex;
	}

	/**
	 * 设置当前播放的歌曲的索引
	 * 
	 * @param currentMusicIndex
	 *            当前播放的歌曲的索引
	 */
	public void setCurrentMusicIndex(int currentMusicIndex) {
		this.currentMusicIndex = currentMusicIndex;
	}
}
