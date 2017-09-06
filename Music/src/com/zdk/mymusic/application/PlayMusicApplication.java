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
	 * ������List����
	 */
	private ArrayList<Music> musics;
	/**
	 * ��ǰ���ŵĸ���
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
		// ��ȡ������List����
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {// �ж�sd��״̬
			Toast.makeText(getApplicationContext(), "ɨ�����", Toast.LENGTH_LONG).show();
		musics = new MusicDao(this).getMusicList();
		}else{
			
		Toast.makeText(getApplicationContext(), "���ֻ���δ��SD��������һ�²����Ƿ���ȷ", Toast.LENGTH_LONG).show();
		}
		super.onCreate();
	}

	/**
	 * ��ȡ������List����
	 * 
	 * @return ������List����
	 */
	public ArrayList<Music> getMusicList() {
		return this.musics;
	}
	
	/**
	 * ��ȡ��ǰ���ڲ��ŵĸ���������
	 * 
	 * @return ��ǰ���ڲ��ŵĸ���������
	 */
	public int getCurrentMusicIndex() {
		return currentMusicIndex;
	}

	/**
	 * ���õ�ǰ���ŵĸ���������
	 * 
	 * @param currentMusicIndex
	 *            ��ǰ���ŵĸ���������
	 */
	public void setCurrentMusicIndex(int currentMusicIndex) {
		this.currentMusicIndex = currentMusicIndex;
	}
}
