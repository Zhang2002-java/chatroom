package com.chatroom.service.impl;

import com.chatroom.entity.FileRecord;
import com.chatroom.mapper.FileRecordMapper;
import com.chatroom.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class FileServiceImpl implements FileService {

    private final FileRecordMapper fileRecordMapper;

    @Value("${upload.path}")
    private String uploadPath;

    public FileServiceImpl(FileRecordMapper fileRecordMapper) {
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public Map<String, String> upload(MultipartFile file, Long uploaderId) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + ext;

            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            File dest = new File(uploadPath + File.separator + newFileName);
            file.transferTo(dest);

            FileRecord record = new FileRecord();
            record.setUploaderId(uploaderId);
            record.setFileName(originalName);
            record.setFilePath("/uploads/" + newFileName);
            record.setFileSize(file.getSize());
            record.setFileType(file.getContentType());
            fileRecordMapper.insert(record);

            Map<String, String> result = new HashMap<>();
            result.put("url", "/uploads/" + newFileName);
            result.put("fileName", originalName);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
}
