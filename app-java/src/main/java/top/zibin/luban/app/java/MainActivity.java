package top.zibin.luban.app.java;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import top.zibin.luban.api.Luban;
import top.zibin.luban.api.OnCompressListener;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SELECT_IMAGE = 100;
    private static final String TAG = "LubanJava";

    private ImageView ivOriginal;
    private TextView tvOriginalSize;
    private TextView tvOriginalDimens;
    private ImageView ivCompressed;
    private TextView tvCompressedSize;
    private TextView tvCompressedDimens;
    private TextView tvStatus;
    private Button btnCompress;

    private Uri selectedUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initStorageDirs();
    }
    
    private void initStorageDirs() {
        new Thread(() -> {
            File appStorageDir = getAppStorageDir();
            File outputStorageDir = getOutputStorageDir();
            
            if (appStorageDir != null) {
                copyAssetsToStorage(appStorageDir);
            }
        }).start();
    }
    
    private File getAppStorageDir() {
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir == null) return null;
        
        File parentFile = externalFilesDir.getParentFile();
        if (parentFile == null) return null;
        
        File appDir = new File(parentFile, "images");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        return appDir;
    }
    
    private File getOutputStorageDir() {
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir == null) return null;
        
        File parentFile = externalFilesDir.getParentFile();
        if (parentFile == null) return null;
        
        File outputDir = new File(parentFile, "compressed");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        return outputDir;
    }
    
    private void copyAssetsToStorage(File targetDir) {
        try {
            String[] assetFiles = getAssets().list("test_images");
            if (assetFiles == null || assetFiles.length == 0) {
                Log.d(TAG, "assets/test_images 目录为空，跳过复制");
                return;
            }
            
            String[] imageExtensions = {".jpg", ".jpeg", ".png", ".webp", ".bmp"};
            
            for (String fileName : assetFiles) {
                String lowerFileName = fileName.toLowerCase();
                
                boolean isImageFile = false;
                for (String ext : imageExtensions) {
                    if (lowerFileName.endsWith(ext)) {
                        isImageFile = true;
                        break;
                    }
                }
                if (!isImageFile) {
                    Log.d(TAG, "跳过非图片文件: " + fileName);
                    continue;
                }
                
                File targetFile = new File(targetDir, fileName);
                
                if (targetFile.exists()) {
                    Log.d(TAG, "文件已存在，跳过: " + fileName);
                    continue;
                }
                
                try (InputStream input = getAssets().open("test_images/" + fileName);
                     FileOutputStream output = new FileOutputStream(targetFile)) {
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    
                    Log.d(TAG, "复制成功: " + fileName);
                } catch (IOException e) {
                    Log.e(TAG, "复制失败: " + fileName, e);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "复制assets图片失败", e);
        }
    }

    private void initViews() {
        Button btnSelect = findViewById(R.id.btn_select_image);
        btnCompress = findViewById(R.id.btn_compress_image);
        Button btnBatchCompress = findViewById(R.id.btn_batch_compress);
        
        ivOriginal = findViewById(R.id.iv_original);
        tvOriginalSize = findViewById(R.id.tv_original_size);
        tvOriginalDimens = findViewById(R.id.tv_original_dimens);
        
        ivCompressed = findViewById(R.id.iv_compressed);
        tvCompressedSize = findViewById(R.id.tv_compressed_size);
        tvCompressedDimens = findViewById(R.id.tv_compressed_dimens);
        
        tvStatus = findViewById(R.id.tv_status);

        btnSelect.setOnClickListener(v -> openGallery());
        btnCompress.setOnClickListener(v -> compressImage());
        btnBatchCompress.setOnClickListener(v -> batchCompress());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedUri = data.getData();
            if (selectedUri != null) {
                displayOriginalImage(selectedUri);
                btnCompress.setEnabled(true);
                // Reset compressed view
                ivCompressed.setImageDrawable(null);
                tvCompressedSize.setText("");
                tvCompressedDimens.setText("");
                tvStatus.setText("");
            }
        }
    }

    private void displayOriginalImage(Uri uri) {
        ivOriginal.setImageURI(uri);
        
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is != null) {
                int size = is.available();
                tvOriginalSize.setText(String.format(getString(R.string.file_size), formatFileSize(size)));
                is.close();
            }
            
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream is2 = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(is2, null, options);
            if (is2 != null) is2.close();
            
            tvOriginalDimens.setText(String.format(Locale.getDefault(), getString(R.string.image_size), options.outWidth, options.outHeight));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void compressImage() {
        if (selectedUri == null) return;
        
        tvStatus.setText("Compressing...");
        
        File outputDir = new File(getExternalFilesDir(null), "compressed");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Use the Java compatible Builder API
        Luban.with(this)
                .load(selectedUri)
                .setTargetDir(outputDir)
                .bindLifecycle(this)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        Log.d(TAG, "Compression started");
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.d(TAG, "Compression success: " + file.getAbsolutePath());
                        tvStatus.setText(getString(R.string.compress_success));
                        displayCompressedImage(file);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Compression error", e);
                        tvStatus.setText(String.format(getString(R.string.compress_failed), e.getMessage()));
                    }
                })
                .launch();
    }
    
    private void displayCompressedImage(File file) {
        ivCompressed.setImageURI(Uri.fromFile(file));
        
        long size = file.length();
        tvCompressedSize.setText(String.format(getString(R.string.file_size), formatFileSize(size)));
        
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        
        tvCompressedDimens.setText(String.format(Locale.getDefault(), getString(R.string.image_size), options.outWidth, options.outHeight));
    }
    
    private void batchCompress() {
        File inputDir = new File(getExternalFilesDir(null).getParentFile(), "images");
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            Toast.makeText(this, "Please put images in " + inputDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return;
        }
        
        File[] files = inputDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
        });
        
        if (files == null || files.length == 0) {
            Toast.makeText(this, "No images found in " + inputDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return;
        }
        
        File outputDir = new File(getExternalFilesDir(null).getParentFile(), "compressed");
        
        List<File> fileList = new ArrayList<>();
        for (File f : files) {
            fileList.add(f);
        }
        
        tvStatus.setText("Batch compressing " + fileList.size() + " images...");
        
        final int[] successCount = {0};
        final int[] failedCount = {0};
        final int total = fileList.size();
        
        Luban.with(this)
                .load(fileList)
                .setTargetDir(outputDir)
                .bindLifecycle(this)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        // Batch start
                    }

                    @Override
                    public void onSuccess(File file) {
                        successCount[0]++;
                        checkBatchComplete(total, successCount[0], failedCount[0]);
                    }

                    @Override
                    public void onError(Throwable e) {
                        failedCount[0]++;
                        checkBatchComplete(total, successCount[0], failedCount[0]);
                    }
                })
                .launch();
    }
    
    private void checkBatchComplete(int total, int success, int failed) {
        if (success + failed >= total) {
            String msg = String.format(Locale.getDefault(), getString(R.string.batch_compress_result), total, success, failed);
            tvStatus.setText(msg);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } else {
            tvStatus.setText("Processing: " + (success + failed) + "/" + total);
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}