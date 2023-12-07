package com.example.serverside;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.serverside.Model.UploadSong;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    TextView textview;
    ProgressBar progressbar;
    Uri audiouri;
    DatabaseReference referencesongs;
    StorageReference mStrorageref;
    StorageTask uploadTask;
    String SongsCategory;
    MediaMetadataRetriever metadataRetriever;
    byte [] art;
    String title1,artist1,album_art1="",duration1;
    TextView title,artist,album,duration,dataa;
    ImageView album_art;
    Spinner spinner;
    Button openaudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textview=findViewById(R.id.TextViewSongFilesSelected);
        progressbar=findViewById(R.id.progressbar);
        title=findViewById(R.id.title);
        artist=findViewById(R.id.artist);
        duration=findViewById(R.id.duration);
        album=findViewById(R.id.album);
        artist=findViewById(R.id.artist);
        dataa=findViewById(R.id.dataa);
        album_art=findViewById(R.id.imageview);
        spinner=findViewById(R.id.spinner);
        openaudio=findViewById(R.id.OpenAudioFiles);

        metadataRetriever = new MediaMetadataRetriever();
        referencesongs= FirebaseDatabase.getInstance().getReference().child("songs");
        mStrorageref= FirebaseStorage.getInstance().getReference().child("songs");

        spinner.setOnItemSelectedListener(this);

        List<String> categories = new ArrayList<>();
        categories.add("Love Songs");
        categories.add("Sad Songs");
        categories.add("Party Songs");
        categories.add("Birthday Songs");
        categories.add("Devotional Songs");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        openaudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAudioFiles();
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        SongsCategory = adapterView.getItemAtPosition(i).toString();
        Toast.makeText(this, "Selected: "+SongsCategory, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void openAudioFiles()
    {
        Intent i =new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("audio/*");
        startActivityForResult(i,101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==101 && resultCode==RESULT_OK && data.getData()!=null)
        {
            audiouri=data.getData();
            String filename = getFilename(audiouri);
            textview.setText(filename);
            metadataRetriever.setDataSource(this,audiouri);

            art=metadataRetriever.getEmbeddedPicture();
            Bitmap bitmap = BitmapFactory.decodeByteArray(art,0,art.length);
            album_art.setImageBitmap(bitmap);
            album.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
            artist.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            dataa.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
            duration.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            title.setText(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));


            artist1=metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            title1=metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            duration1=metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        }
    }

    @SuppressLint("Range")
    private String getFilename(Uri uri)
    {
        String result=null;
        if (uri.getScheme().equals("content"))
        {
            Cursor cursor = getContentResolver().query(uri,null,null,null,null);
            try {
                if (cursor!=null && cursor.moveToFirst())
                {
                    result=cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
            finally {
                cursor.close();
            }
        }

        if (result==null)
        {
            result=uri.getPath();
            int cut=result.lastIndexOf('/');
            if (cut!=-1){
                result=result.substring(cut+1);
            }
        }
        return result;
    }

    public void uploadfiletofirebase(View view)
    {
        if (textview.equals("No file selected"))
        {
            Toast.makeText(this, "please select an image", Toast.LENGTH_LONG).show();
        }
        else {
            if (uploadTask != null && uploadTask.isInProgress())
            {
                Toast.makeText(this, "song uploading in progress", Toast.LENGTH_LONG).show();
            }else {
                uploadFiles();
            }
        }
    }

    private void uploadFiles() {
        if (audiouri != null )
        {
            Toast.makeText(this, "uploading please wait!", Toast.LENGTH_SHORT).show();
            progressbar.setVisibility(View.VISIBLE);
            final StorageReference storageReference=mStrorageref.child(title.getText()+"."+getfileextension(audiouri));
            uploadTask=storageReference.putFile(audiouri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            UploadSong uploadSong = new UploadSong(SongsCategory,title1,artist1,album_art1,duration1,uri.toString());
                            String uploadId = referencesongs.push().getKey();
                            referencesongs.child(uploadId).setValue(uploadSong);
                            Toast.makeText(MainActivity.this, "Song Uploaded"+title.getText(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                    double progress= (100.0*snapshot.getBytesTransferred()/snapshot.getTotalByteCount());
                    progressbar.setProgress((int)progress);
                }
            });
        }
        else {
            Toast.makeText(this, "No File Selected To Upload", Toast.LENGTH_LONG).show();
        }
    }

    private String getfileextension(Uri uri)
    {
        ContentResolver contentResolver=getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    public void openAlbumactivity(View v){
        Intent intent = new Intent(MainActivity.this,UploadAlbumActivity.class);
        startActivity(intent);
    }
}