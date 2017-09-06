package com.zdk.mymusic.utils;

public interface ApplicationConsts {
	// 以下是Activity发出的广播

	/**
	 * Activity发出的，播放按钮被点击
	 */
	String ACTIVITY_PLAY_BUTTON_CLICK = "zdk.intent.action.PLAY_BUTTON_CLICK";
	String ACTIVITY_PREVIOUS_BUTTON_CLICK = "zdk.intent.action.PREVIOUS_BUTTON_CLICK";
	String ACTIVITY_NEXT_BUTTON_CLICK = "zdk.intent.action.NEXT_BUTTON_CLICK";
	String ACTIVITY_MUSIC_INDEX_CHANGED = "zdk.intent.action.MUSIC_INDEX_CHANGED";
	String ACTIVITY_SEEKBAR_CHANGED = "zdk.intent.action.SEEKBAR_CHANGED";
	String ACTIVITY_START_MAIN = "android.intent.action.MAIN";
	String ACTIVITY_NEW_OUTGOING_CALL="android.intent.action.NEW_OUTGOING_CALL";
	String ACTION_TIME_CHANGED="android.intent.action.TIME_SET";
	String ACTION_PHONE_STATE="android.intent.action.PHONE_STATE";
	// 以下是Service发出的广播

	/**
	 * Service发出的，播放歌曲
	 */
	String SERVICE_PLAYER_PLAY = "zdk.intent.action.PLAYER_PLAY";
	/**
	 * Service发出的，暂停播放歌曲
	 */
	String SERVICE_PLAYER_PAUSE = "zdk.intent.action.PLYAER_PAUSE";
	/**
	 * Service发出的，更新进度播放歌曲
	 */
	String SERVICE_UPDATE_PROGRESS = "8888888888";
}
