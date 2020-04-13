package com.example.zou.recording;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    private static final int WRITE_EXTERNAL_STORAGE = 200;
    MediaRecorder meRec;                                            //定義錄音的方式MediaRecorder
    Button RecordStart, RecordStop, PlayStart, DeletePlay, PermissionCheck, StopPlay, Pick;           // 定義 按鈕的變數
    EditText Filename;                                               //定義 輸入文字的變數
    TextView fname, sname;                                          //定義 文字顯示的變數
    File dir;                                                         //定義 文件夾的變數
    Toast tos;                                                        //定義 Toast的變數
    MediaPlayer m;
    String str;

    Uri uri;

    boolean isStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //定義螢幕開啟 旋轉 不休眠
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        RecordStart = (Button) findViewById(R.id.RecordStart);       // 定義 按鈕的變數  值
        RecordStop = (Button) findViewById(R.id.RecordStop);
        PlayStart = (Button) findViewById(R.id.PlayStart);
        DeletePlay = (Button) findViewById(R.id.DeletePlay);
        PermissionCheck = (Button) findViewById(R.id.PermissionCheck);
        StopPlay = (Button) findViewById(R.id.StopPlay);
        Pick = (Button) findViewById(R.id.PickRecord);

        Filename = (EditText) findViewById(R.id.filename);          // 定義 文字輸入的變數 值
        fname = (TextView) findViewById(R.id.fname);
        sname = (TextView) findViewById(R.id.sname);

        tos = Toast.makeText(this, "", Toast.LENGTH_SHORT);     // 定義 Toast的變數 值

        RecordStop.setEnabled(false);                               //  鎖住 按鈕 不能用
        PlayStart.setEnabled(false);
        DeletePlay.setEnabled(false);
        StopPlay.setEnabled(false);

        m = new MediaPlayer();                                      //定義播放的方式 MediaPlayer
        m.setOnCompletionListener(this);

    }


    public void onPermissionCheck(View view) {

        String[] permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        List<String> mPermissionList = new ArrayList<>();

        mPermissionList.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        if (mPermissionList.isEmpty()) {//未授權值為空
            Toast.makeText(MainActivity.this,"已經拿到所有權限囉!",Toast.LENGTH_LONG).show();
        } else {//請求權限
            String[] permission = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(MainActivity.this, permission, WRITE_EXTERNAL_STORAGE);
        }
    }

    public void onRecordStart(View view) {
        PlayStart.setEnabled(false);
        Pick.setEnabled(false);
        DeletePlay.setEnabled(false);

        dir = new File(Environment.getExternalStorageDirectory(), "Recoeds");  //設定儲存的文件夾
        if (!dir.exists()) dir.mkdir();

        File outputFile = new File(dir, Filename.getText().toString() + ".3gp"); //設定儲存附檔名
        meRec = new MediaRecorder();                                            //設定MediaRecorder 給變數 meRec

        try {

            meRec.setAudioSource(MediaRecorder.AudioSource.MIC);                   //定義MediaRecorder 使用手機麥克風
            meRec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);         //定義MediaRecorder匯出的格式
            meRec.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);            //定義MediaRecorder編碼的格式
            meRec.setOutputFile(String.valueOf(outputFile));                         //定義MediaRecorder 輸出的位置
            sname.setText(outputFile.toString());

            try {
                meRec.prepare();                                                    //定義MediaRecorder 準備錄音
                meRec.start();                                                      //定義MediaRecorder 開始錄音
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            tos.setText("開始錄音");
            tos.show();
            fname.setText(Filename.getText().toString() + ".3gp");
            RecordStart.setEnabled(false);
            RecordStop.setEnabled(true);                                            //  啟動 按鈕
        }catch (Exception e) {
            tos.setText("發生錯誤");
            tos.show();
        }

    }

    public void onRecordStop(View view) {
        try {
            meRec.stop();                                                           //定義MediaRecorder 停止錄音
            meRec.reset();                                                          //定義MediaRecorder 重啟程式
            meRec.release();                                                        //定義MediaRecorder 釋放資源
            meRec = null;

            tos.setText("結束錄音");
            tos.show();
        }catch (Exception e) {
            tos.setText("發生錯誤");
            tos.show();
            PlayStart.setEnabled(false);
        }
        if(m != null) m.reset();
        str = null;

        RecordStart.setEnabled(true);                                            //  啟動 按鈕
        RecordStop.setEnabled(false);
        PlayStart.setEnabled(true);                                              //  啟動 按鈕
        DeletePlay.setEnabled(true);                                             //  啟動 按鈕
        Pick.setEnabled(true);
    }

    public void onPlayStart(View view) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
        try{
            RecordStart.setEnabled(false);
            DeletePlay.setEnabled(false);
            Pick.setEnabled(false);
            if(!m.isPlaying()){
                str= sname.getText().toString();
                m.reset();
                m.setDataSource(str);
                m.prepare();

                m.start();
                StopPlay.setEnabled(true);
                PlayStart.setText("暫停播放");
                tos.setText("播放");
                tos.show();
            }else {
                m.pause();
                PlayStart.setText("繼續播放");
                tos.setText("暫停播放");
                tos.show();
            }
        } catch (Exception e) {
            tos.setText("發生錯誤");
            tos.show();
            PlayStart.setEnabled(false);
        }
    }

    public void onStopPlay(View view) {
        m.pause();
        m.seekTo(0);
        PlayStart.setText("播放錄音");

        StopPlay.setEnabled(false);
        Pick.setEnabled(true);
        DeletePlay.setEnabled(true);
        RecordStart.setEnabled(true);
    }


    //監聽播放狀態-已結束
    @Override
    public void onCompletion(MediaPlayer mp) {
        m.seekTo(0);
        PlayStart.setText("播放錄音");
        StopPlay.setEnabled(false);
        Pick.setEnabled(true);
        DeletePlay.setEnabled(true);
        RecordStart.setEnabled(true);
    }

    public void onDelete(View view) {
        String x = sname.getText().toString();                                  //定義 變數的值
        File f = new File(x);                                                     //位置判斷

        if(f.exists()){
            f.delete();                                                          //刪除檔案
        }

        fname.setText("檔案儲存的名稱");
        sname.setText(" ");
        tos.setText("刪除檔案");
        tos.show();

        PlayStart.setEnabled(false);
        StopPlay.setEnabled(false);
        DeletePlay.setEnabled(false);
        Pick.setEnabled(true);
        RecordStart.setEnabled(true);
    }


    public void onPickRecord(View view) {

        Intent it = new Intent(Intent.ACTION_GET_CONTENT);
        it.setType( "Audio/*");
        startActivityForResult(it, 100);

    }

    protected void  onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            uri = convertUri(data.getData());
        }
        if(uri != null){
            PlayStart.setEnabled(true);                                              //  啟動 按鈕
            DeletePlay.setEnabled(true);
        }
    }

    private Uri convertUri(Uri uri) {
        if(uri.toString().substring(0, 7).equals("content")) {
            String[] colName = {MediaStore.MediaColumns.DATA};
            Cursor cursor = getContentResolver().query(uri, colName, null, null, null);
            cursor.moveToFirst();
            uri = Uri.parse("file://" + cursor.getString(0));
            cursor.close();
            sname.setText(uri.getPath());
            fname.setText(uri.getLastPathSegment());
        }
        return uri;
    }
}
