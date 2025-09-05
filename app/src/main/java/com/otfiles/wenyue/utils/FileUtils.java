package com.otfiles.wenyue.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileUtils {

    private static final String TAG = "FileUtils";
    
    // 常见编码的BOM标记
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] UTF16LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF32BE_BOM = {(byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF};
    private static final byte[] UTF32LE_BOM = {(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00};
    
    // 支持的编码列表
    public static final String[] SUPPORTED_ENCODINGS = {
        "UTF-8", "GBK", "GB2312", "ISO-8859-1", "Big5", 
        "UTF-16", "UTF-16BE", "UTF-16LE", "US-ASCII"
    };

    /**
     * 对文件列表进行排序：文件夹在前，文件在后，按名称排序
     * @param files 要排序的文件列表
     * @return 排序后的文件列表
     */
    public static List<File> sortFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 分离文件夹和文件
        List<File> directories = new ArrayList<>();
        List<File> fileList = new ArrayList<>();
        
        for (File file : files) {
            if (file.isDirectory()) {
                directories.add(file);
            } else {
                fileList.add(file);
            }
        }
        
        // 对文件夹和文件分别按名称排序
        Collections.sort(directories, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        
        // 合并结果：文件夹在前，文件在后
        List<File> result = new ArrayList<>();
        result.addAll(directories);
        result.addAll(fileList);
        
        return result;
    }

    /**
     * 判断文件是否为文本文件（通过扩展名）
     * @param extension 文件扩展名
     * @return 是否为文本文件
     */
    public static boolean isTextFile(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        String ext = extension.toLowerCase();
        return ext.equals("txt") || 
               ext.equals("xml") || 
               ext.equals("html") || 
               ext.equals("htm") || 
               ext.equals("json") || 
               ext.equals("js") || 
               ext.equals("css") || 
               ext.equals("java") || 
               ext.equals("c") || 
               ext.equals("cpp") || 
               ext.equals("h") || 
               ext.equals("py") || 
               ext.equals("php") || 
               ext.equals("log") || 
               ext.equals("md");
    }

    /**
     * 判断文件是否为图片文件（通过扩展名）
     * @param extension 文件扩展名
     * @return 是否为图片文件
     */
    public static boolean isImageFile(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        String ext = extension.toLowerCase();
        return ext.equals("jpg") || 
               ext.equals("jpeg") || 
               ext.equals("png") || 
               ext.equals("gif") || 
               ext.equals("bmp") || 
               ext.equals("webp");
    }

    /**
     * 判断文件是否为音频文件（通过扩展名）
     * @param extension 文件扩展名
     * @return 是否为音频文件
     */
    public static boolean isAudioFile(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        String ext = extension.toLowerCase();
        return ext.equals("mp3") || 
               ext.equals("wav") || 
               ext.equals("ogg") || 
               ext.equals("flac") || 
               ext.equals("aac");
    }

    /**
     * 判断文件是否为视频文件（通过扩展名）
     * @param extension 文件扩展名
     * @return 是否为视频文件
     */
    public static boolean isVideoFile(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        String ext = extension.toLowerCase();
        return ext.equals("mp4") || 
               ext.equals("avi") || 
               ext.equals("mkv") || 
               ext.equals("mov") || 
               ext.equals("wmv") || 
               ext.equals("flv");
    }

    /**
     * 检测文件编码
     * @param file 要检测的文件
     * @return 检测到的编码名称
     */
    public static String detectEncoding(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return "UTF-8"; // 默认返回UTF-8
        }
        
        // 首先检查BOM标记
        String encodingByBOM = detectEncodingByBOM(file);
        if (encodingByBOM != null) {
            return encodingByBOM;
        }
        
        // 如果没有BOM，尝试通过内容分析
        return detectEncodingByContent(file);
    }
    
    /**
     * 通过BOM标记检测编码
     */
    private static String detectEncodingByBOM(File file) {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            
            bis.mark(4);
            byte[] bom = new byte[4];
            int read = bis.read(bom, 0, 4);
            
            if (read >= 3 && 
                bom[0] == UTF8_BOM[0] && 
                bom[1] == UTF8_BOM[1] && 
                bom[2] == UTF8_BOM[2]) {
                return "UTF-8";
            }
            
            if (read >= 2) {
                if (bom[0] == UTF16BE_BOM[0] && bom[1] == UTF16BE_BOM[1]) {
                    return "UTF-16BE";
                }
                if (bom[0] == UTF16LE_BOM[0] && bom[1] == UTF16LE_BOM[1]) {
                    return "UTF-16LE";
                }
            }
            
            if (read >= 4) {
                if (bom[0] == UTF32BE_BOM[0] && bom[1] == UTF32BE_BOM[1] && 
                    bom[2] == UTF32BE_BOM[2] && bom[3] == UTF32BE_BOM[3]) {
                    return "UTF-32BE";
                }
                if (bom[0] == UTF32LE_BOM[0] && bom[1] == UTF32LE_BOM[1] && 
                    bom[2] == UTF32LE_BOM[2] && bom[3] == UTF32LE_BOM[3]) {
                    return "UTF-32LE";
                }
            }
            
            bis.reset();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            Log.e(TAG, "Error detecting encoding by BOM", e);
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
        
        return null;
    }
    
    /**
     * 通过内容分析检测编码
     */
    private static String detectEncodingByContent(File file) {
        // 尝试常见的中文编码
        String[] candidateEncodings = {"GBK", "GB2312", "UTF-8", "ISO-8859-1", "Big5"};
        
        for (String encoding : candidateEncodings) {
            if (isValidEncoding(file, encoding)) {
                return encoding;
            }
        }
        
        // 如果都不行，默认返回UTF-8
        return "UTF-8";
    }
    
    /**
     * 检查文件是否可以用指定编码正确读取
     */
    private static boolean isValidEncoding(File file, String encoding) {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        
        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, encoding);
            br = new BufferedReader(isr);
            
            // 尝试读取前几行，如果没有异常，则认为编码有效
            for (int i = 0; i < 10; i++) {
                String line = br.readLine();
                if (line == null) break;
                
                // 检查是否有明显的乱码字符（替换字符）
                if (line.contains("�")) {
                    return false;
                }
            }
            
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + file.getAbsolutePath(), e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error validating encoding: " + encoding, e);
            return false;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }
    
    /**
     * 读取文件内容
     * @param file 要读取的文件
     * @param encoding 文件编码
     * @return 文件内容字符串
     */
    public static String readFile(File file, String encoding) {
        if (file == null || !file.exists() || !file.isFile()) {
            return "";
        }
        
        StringBuilder content = new StringBuilder();
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        
        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, encoding);
            br = new BufferedReader(isr);
            
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + file.getAbsolutePath(), e);
            return "";
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + file.getAbsolutePath(), e);
            return "";
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
        
        return content.toString();
    }
    
    /**
     * 保存内容到文件
     * @param file 要保存的文件
     * @param content 要保存的内容
     * @param encoding 使用的编码
     * @return 是否保存成功
     */
    public static boolean saveFile(File file, String content, String encoding) {
        if (file == null || content == null) {
            return false;
        }
        
        // 确保目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + parentDir.getAbsolutePath());
                return false;
            }
        }
        
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        
        try {
            fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos, encoding);
            bw = new BufferedWriter(osw);
            
            bw.write(content);
            bw.flush();
            return true;
            
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + file.getAbsolutePath(), e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error saving file: " + file.getAbsolutePath(), e);
            return false;
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
                if (osw != null) {
                    osw.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }
    
    /**
     * 获取文件扩展名
     * @param file 文件
     * @return 文件扩展名（小写）
     */
    public static String getFileExtension(File file) {
        if (file == null) {
            return "";
        }
        
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
            return name.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * 获取文件扩展名（从路径字符串）
     * @param path 文件路径
     * @return 文件扩展名（小写）
     */
    public static String getFileExtension(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < path.length() - 1) {
            return path.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * 判断文件是否为文本文件
     * @param file 文件
     * @return 是否为文本文件
     */
    public static boolean isTextFile(File file) {
        String extension = getFileExtension(file);
        return isTextFile(extension);
    }
    
    /**
     * 获取文件大小（带单位）
     * @param file 文件
     * @return 格式化后的文件大小字符串
     */
    public static String getFileSize(File file) {
        if (file == null || !file.exists()) {
            return "0 B";
        }
        
        long size = file.length();
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 获取文件最后修改时间
     * @param file 文件
     * @return 最后修改时间字符串
     */
    public static String getFileLastModified(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        
        long time = file.lastModified();
        return android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", time).toString();
    }
    
    /**
     * 复制文件
     * @param src 源文件
     * @param dest 目标文件
     * @return 是否复制成功
     */
    public static boolean copyFile(File src, File dest) {
        if (src == null || !src.exists() || dest == null) {
            return false;
        }
        
        // 确保目标目录存在
        File parentDir = dest.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + parentDir.getAbsolutePath());
                return false;
            }
        }
        
        FileInputStream fis = null;
        FileOutputStream fos = null;
        
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);
            
            byte[] buffer = new byte[4096];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + src.getAbsolutePath(), e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error copying file", e);
            return false;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }
    
    /**
     * 删除文件或目录
     * @param file 要删除的文件或目录
     * @return 是否删除成功
     */
    public static boolean deleteFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        if (file.isDirectory()) {
            // 递归删除目录内容
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteFile(child);
                }
            }
        }
        
        return file.delete();
    }
    
    /**
     * 从文件路径获取文件名
     * @param path 文件路径
     * @return 文件名
     */
    public static String getFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int lastSeparator = path.lastIndexOf(File.separator);
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
    }
    
    /**
     * 判断路径是否为目录
     * @param path 路径
     * @return 是否为目录
     */
    public static boolean isDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isDirectory();
    }
    
    /**
     * 判断文件是否存在
     * @param path 文件路径
     * @return 是否存在
     */
    public static boolean fileExists(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists();
    }
    
    /**
     * 从Uri获取文件路径
     * @param context 上下文
     * @param uri 文件的Uri
     * @return 文件路径
     */
    /*public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        
        // 如果Uri是文件协议，直接返回路径
        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }
        
        // 如果是content协议，尝试通过ContentResolver查询
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                String[] projection = {MediaStore.Images.Media.DATA};
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting path from URI: " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        
        // 如果以上都不行，返回null
        return null;
    }*/
    
    public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        
        // 如果Uri是文件协议，直接返回路径
        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }
        
        // 如果是content协议，尝试通过ContentResolver查询
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                String[] projection = {MediaStore.Images.Media.DATA};
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting path from URI: " + e.getMessage());
                
                // 如果上述方法失败，尝试另一种方法
                try {
                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        // 对于某些特殊URI，我们无法获取路径，只能返回null
                        inputStream.close();
                    }
                } catch (IOException ioException) {
                    Log.e(TAG, "Error opening stream from URI: " + ioException.getMessage());
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        
        // 如果以上都不行，返回null
        return null;
    }
}