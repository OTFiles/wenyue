package com.otfiles.wenyue.utils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;

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
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
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
        } catch (IOException e) {
            Log.e(TAG, "Error detecting encoding by BOM", e);
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
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, encoding);
             BufferedReader br = new BufferedReader(isr)) {
            
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
        } catch (IOException e) {
            return false;
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
        
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, encoding);
             BufferedReader br = new BufferedReader(isr)) {
            
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + file.getAbsolutePath(), e);
            return "";
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
        
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
             BufferedWriter bw = new BufferedWriter(osw)) {
            
            bw.write(content);
            bw.flush();
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving file: " + file.getAbsolutePath(), e);
            return false;
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
     * 判断文件是否为文本文件
     * @param file 文件
     * @return 是否为文本文件
     */
    public static boolean isTextFile(File file) {
        String extension = getFileExtension(file);
        return extension.equals("txt") || 
               extension.equals("xml") || 
               extension.equals("html") || 
               extension.equals("htm") || 
               extension.equals("json") || 
               extension.equals("js") || 
               extension.equals("css") || 
               extension.equals("java") || 
               extension.equals("c") || 
               extension.equals("cpp") || 
               extension.equals("h") || 
               extension.equals("py") || 
               extension.equals("php") || 
               extension.equals("log") || 
               extension.equals("md");
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
        
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {
            
            byte[] buffer = new byte[4096];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error copying file", e);
            return false;
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
}