package com.zdk.mymusic.entity;

/**
 * 歌曲的实体类
 * 
 * @author 郑敦坤
 * 
 */
public class Music {
	private long id;
	private String data;//路径
	private String title;//歌名
	private long duration;//时长
	private String artist;//艺术家
	private String album;//专辑
	private long albumId;


	public long getAlbumId() {
		return albumId;
	}

	public void setAlbumId(long albumId) {
		this.albumId = albumId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	@Override
	public String toString() {
		return "Music [id=" + id + ", data=" + data + ", title=" + title
				+ ", duration=" + duration + ", artist=" + artist + ", album="
				+ album + "]";
	}

}
