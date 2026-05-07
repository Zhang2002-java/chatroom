package com.chatroom.service.impl;

import com.chatroom.entity.FileRecord;
import com.chatroom.mapper.FileRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock private FileRecordMapper fileRecordMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "uploadPath", tempDir.toString());
    }

    @Test
    void upload_success_returnsUrlAndFileName() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.png", "image/png", "test-content".getBytes()
        );

        var result = fileService.upload(file, 1L);

        assertTrue(result.get("url").startsWith("/uploads/"));
        assertTrue(result.get("url").endsWith(".png"));
        assertEquals("test.png", result.get("fileName"));
        verify(fileRecordMapper).insert(any(FileRecord.class));
    }

    @Test
    void upload_fileWithoutExtension_returnsUuidFileName() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "noext", "application/octet-stream", "data".getBytes()
        );

        var result = fileService.upload(file, 1L);

        assertTrue(result.get("url").startsWith("/uploads/"));
        assertFalse(result.get("url").endsWith("."));
    }

    @Test
    void upload_createsDirectoryIfNotExists() {
        // Use a subpath that doesn't exist yet
        String newDir = tempDir.resolve("subdir").toString();
        ReflectionTestUtils.setField(fileService, "uploadPath", newDir);

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.txt", "text/plain", "content".getBytes()
        );

        var result = fileService.upload(file, 1L);

        assertNotNull(result.get("url"));
        verify(fileRecordMapper).insert(any(FileRecord.class));
    }

    @Test
    void upload_persistsFileRecord() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", new byte[100]
        );

        fileService.upload(file, 1L);

        ArgumentCaptor<FileRecord> captor = ArgumentCaptor.forClass(FileRecord.class);
        verify(fileRecordMapper).insert(captor.capture());
        FileRecord record = captor.getValue();
        assertEquals(1L, record.getUploaderId());
        assertEquals("photo.jpg", record.getFileName());
        assertEquals("image/jpeg", record.getFileType());
        assertEquals(100L, record.getFileSize());
    }
}
