package com.zdk.mymusic.utils;

public interface ApplicationConsts {
	// ������Activity�����Ĺ㲥

	/**
	 * Activity�����ģ����Ű�ť�����
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
	// ������Service�����Ĺ㲥

	/**
	 * Service�����ģ����Ÿ���
	 */
	String SERVICE_PLAYER_PLAY = "zdk.intent.action.PLAYER_PLAY";
	/**
	 * Service�����ģ���ͣ���Ÿ���
	 */
	String SERVICE_PLAYER_PAUSE = "zdk.intent.action.PLYAER_PAUSE";
	/**
	 * Service�����ģ����½��Ȳ��Ÿ���
	 */
	String SERVICE_UPDATE_PROGRESS = "8888888888";
}
