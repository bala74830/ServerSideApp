package com.example.serverside;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class UploadAlbumActivity extends AppCompatActivity implements View.OnClickListener{

    Button buttonchoose,buttonupload;
    EditText editTextName;
    ImageView imageView;
    String songsCategory;
    private static final int PICK_IMAGE_REQUEST = 234;
    Uri filepath;
    StorageReference storageReference;
    DatabaseReference mdatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_album);

        buttonchoose=findViewById(R.id.buttonChoose);
        buttonupload=findViewById(R.id.buttonuploadalb);
        editTextName=findViewById(R.id.edt_txt);
        imageView=findViewById(R.id.imageview);
        Spinner spinner=findViewById(R.id.spinner);

        buttonupload.setOnClickListener(this);
        buttonchoose.setOnClickListener(this);

        storageReference= FirebaseStorage.getInstance().getReference();
        mdatabaseReference= FirebaseDatabase.getInstance().getReference(Constants.DATABASE_PATH_UPLOAD);


        List<String> categories = new ArrayList<>();
        categories.add("Love Songs");
        categories.add("Sad Songs");
        categories.add("Party Songs");
        categories.add("Birthday Songs");
        categories.add("Devotional Songs");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                songsCategory=adapterView.getItemAtPosition(i).toString();
                Toast.makeText(UploadAlbumActivity.this, "Selected:"+songsCategory, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view==buttonchoose)
        {
            showfilechoose();
        }
        else if(view==buttonupload){
            uploadfile();
        }
    }

    private void uploadfile() {

        if (filepath != null){
            ProgressDialog progressDialog =new ProgressDialog(this);
            progressDialog.setTitle("uploading...");
            progressDialog.show();
            final StorageReference sref =storageReference.child(Constants.STORAGE_PATH_UPLOAD+System.currentTimeMillis()+"."+getfileExtension(filepath));

            sref.putFile(filepath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    sref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            String url =uri.toString();
                            Upload upload=new Upload(editTextName.getText().toString().trim(),url,songsCategory);
                            String uploadId = mdatabaseReference.push().getKey();
                            mdatabaseReference.child(uploadId).setValue(upload);
                            progressDialog.dismiss();
                            Toast.makeText(UploadAlbumActivity.this, "File Uploaded...", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    progressDialog.dismiss();
                    Toast.makeText(UploadAlbumActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                    double progress =(100.0 * snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
                    progressDialog.setMessage("upload "+(int)progress+"%...");

                }
            });

        }

    }

    private void showfilechoose() {
        Intent intent =new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==PICK_IMAGE_REQUEST && resultCode==RESULT_OK && data != null && data.getData() != null)
        {
            filepath=data.getData();
            Bitmap bitmap = null;
            try {
                bitmap= MediaStore.Images.Media.getBitmap(getContentResolver(),filepath);
                imageView.setImageBitmap(bitmap);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    private String getfileExtension(Uri uri){
        ContentResolver contentResolver=getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(contentResolver.getType(uri));
    }
}