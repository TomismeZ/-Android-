package com.zdk.mymusic.dao;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.zdk.mymusic.R;
import com.zdk.mymusic.entity.Music;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.util.Log;
import android.widget.Toast;

/**
 * 歌曲的数据源操作类
 * @author 郑敦坤
 *
 */
public class MusicDao {
	private Context context;
    //获取专辑封面的Uri
    private static final Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
	public MusicDao(Context context) {
		this.context = context;
	}
	public void update(long l){
		ContentResolver cr = context.getContentResolver();
		System.out
		.println(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString());
		Uri uri = Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()+"/"+String.valueOf(l));
		ContentValues values=new ContentValues();
		values.put(Media.TITLE, "郑敦坤");
		cr.update(uri, values, null, null);
		Toast.makeText(context, "这是媒体文件，不允许修改歌曲信息哦", Toast.LENGTH_LONG).show();
	}
	public void delete(long l){
		ContentResolver cr = context.getContentResolver();
		System.out
		.println(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString());
		Uri uri = Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()+"/"+String.valueOf(l));
		cr.delete(uri, null, null);
	}
	public ArrayList<Music> getMusicList() {
		ArrayList<Music> musics = null;
		Music music;
		ContentResolver cr;
		String[] projection;
		Cursor cursor;
		Uri uri;

		uri = Media.EXTERNAL_CONTENT_URI;
		projection = new String[] { Media._ID, Media.DATA, Media.TITLE,
				Media.DURATION, Media.ARTIST, Media.ALBUM,Media.ALBUM_ID};
		cr = context.getContentResolver();
		cursor = cr.query(uri, projection, null, null, null);

		if (cursor != null && cursor.getCount() > 0) {
			musics = new ArrayList<Music>();
			while (cursor.moveToNext()) {
				long long1 = cursor.getLong(cursor
						.getColumnIndex(Media.DURATION));
				if (long1 > 30000) {
					music = new Music();

					music.setId(cursor.getLong(cursor.getColumnIndex(Media._ID)));
					music.setDuration(cursor.getLong(cursor
							.getColumnIndex(Media.DURATION)));
					Log.i("DURATION", long1 + "long1");
					music.setData(cursor.getString(cursor
							.getColumnIndex(Media.DATA)));
					music.setTitle(cursor.getString(cursor
							.getColumnIndex(Media.TITLE)));
					music.setArtist(cursor.getString(cursor
							.getColumnIndex(Media.ARTIST)));
					music.setAlbum(cursor.getString(cursor
							.getColumnIndex(Media.ALBUM)));
					music.setAlbumId(cursor.getLong(cursor.getColumnIndex(Media.ALBUM_ID)));
					musics.add(music);
					music = null;
				}

			}

			cursor.close();
		}

		// 以下是Log输出获取数据的结果
		Log.d("", "MusicDao.getMusicList(), musics = " + musics);
		if (musics != null) {
			for (Music m : musics) {
				Log.d("", "MusicDao.getMusicList(), music = " + m.toString());
			}
		}
		return musics;
	}
	
    /**
     * 获取专辑封面位图对象
     *
     * @param context
     * @param song_id
     * @param album_id
     * @param allowdefalut
     * @param small
     * @return
     */
    public static Bitmap getArtwork(Context context, long song_id, long album_id, boolean allowdefalut, boolean small) {
        if (album_id < 0) {
            if (song_id < 0) {
                Bitmap bm = getArtworkFromFile(context, song_id, -1);
                if (bm != null) {
                    return bm;
                }
            }
            if (allowdefalut) {
                return getDefaultArtwork(context, small);
            }
            return null;
        }
        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(albumArtUri, album_id);
        if (uri != null) {
            InputStream in = null;
            try {
                in = res.openInputStream(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                //先制定原始大小
                options.inSampleSize = 1;
                //只进行大小判断
                options.inJustDecodeBounds = true;
                //调用此方法得到options得到图片的大小
                BitmapFactory.decodeStream(in, null, options);
                /**我们的目的是在你N pixel的画面上显示。所以需要调用computeSampleSize得到图片缩放的比例**/
                /**这里的target为800是根据默认专辑图片大小决定的，800只是测试数字但是实验后发现完美的结合**/
                if (small) {
                    options.inSampleSize = computeSampleSize(options, 40);
                } else {
                    options.inSampleSize = computeSampleSize(options, 600);
                }
                //我们得到了缩放比例，现在开始正式读入Bitmap数据
                options.inJustDecodeBounds = false;
                options.inDither = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                in = res.openInputStream(uri);
                return BitmapFactory.decodeStream(in, null, options);
            } catch (FileNotFoundException e) {
                Bitmap bm = getArtworkFromFile(context, song_id, album_id);
                if (bm != null) {
                    if (bm.getConfig() == null) {
                        bm = bm.copy(Bitmap.Config.RGB_565, false);
                        if (bm == null && allowdefalut) {
                            return getDefaultArtwork(context, small);
                        }
                    }
                } else if (allowdefalut) {
                    bm = getDefaultArtwork(context, small);
                }
                return bm;
            } finally {
                try {
                    if (in != null) {

                        in.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    
    /**
     * 获取默认专辑图片
     *
     * @param context
     * @param small
     * @return
     */
    public static Bitmap getDefaultArtwork(Context context, boolean small) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        if (small) {//返回小图片
            return BitmapFactory.decodeStream(context.getResources()
                    .openRawResource(R.drawable.music), null, opts);
        }
        return BitmapFactory.decodeStream(context.getResources()
                .openRawResource(R.drawable.defaultalbum), null, opts);
    }
    /**
     * 从文件当中获取专辑封面位图
     *
     * @param context
     * @param songid
     * @param albumid
     * @return
     */
    private static Bitmap getArtworkFromFile(Context context, long songid, long albumid) {
        Bitmap bm = null;
        if (albumid < 0 && songid < 0) {
            throw new IllegalArgumentException("Must specify an album or a song id");
        }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            FileDescriptor fd = null;
            if (albumid < 0) {
                Uri uri = Uri.parse("content://media/external/audio/media" + songid + "/albumart");
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    fd = pfd.getFileDescriptor();
                }
            } else {
                Uri uri = ContentUris.withAppendedId(albumArtUri, albumid);
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    fd = pfd.getFileDescriptor();
                }
            }
            options.inSampleSize = 1;
            //只进行大小判断
            options.inJustDecodeBounds = true;
            //调用此方法得到options得到图片的大小
            BitmapFactory.decodeFileDescriptor(fd, null, options);
            //我们的目标是在800pixel的画面上显示
            //所以需要调用computeSampleSize得到图片缩放的比例
            options.inSampleSize = 100;
            //我们得到缩放比例，现在开始正式读入Bitmap数据
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            //根据options参数，减少所需要的内存
            bm = BitmapFactory.decodeFileDescriptor(fd, null, options);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bm;
    }
    public static int computeSampleSize(BitmapFactory.Options options, int target) {
        int w = options.outWidth;
        int h = options.outHeight;
        int candidateW = w / target;
        int candidateH = h / target;
        int candidate = Math.max(candidateW, candidateH);
        if (candidate == 0) {
            return 1;
        }
        if (candidate > 1) {
            if ((w > target) && (w / candidate < target)) {
                candidate -= 1;
            }
        }
        if (candidate > 1) {
            if ((h > target) && (h / candidate < target)) {
                candidate -= 1;
            }
        }
        return candidate;
    }
}
